package net.kornstar.exchange.collection

import java.util.concurrent.atomic.AtomicInteger

import net.kornstar.exchange.collection.Transaction.TransactionType
import org.slf4j.LoggerFactory
import net.kornstar.exchange.global._

import scala.util.Try

/**
 * Created by Ben Kornmeier on 5/7/2015.
 */
object Transaction {
  sealed trait TransactionType
  object TransactionType {
    case object Deposit extends TransactionType
    case object Withdraw extends TransactionType
    case object Buy extends TransactionType
    case object Sell extends TransactionType
  }
}

case class Transaction(userId:Int,baseCurrencyAmount:Double, otherCurrencyAmount:Int,transactionType:Transaction.TransactionType,time:Option[Long] = None,id:Option[Int] = None,note:Option[String] = None,orderOpt:Option[Order] = None)

trait Account {
  def baseCurrencyAmount:Double
  def otherCurrencyAmount:Double

  def submitTransaction(t:Transaction):Account

  def userId:Int

}

object Account {
  val logger = LoggerFactory.getLogger(classOf[Account])
  val idMaker:AtomicInteger = new AtomicInteger(0)
  def apply(userId:Int):Account = {
    logger.info(s"Creating a new account for user ID ${userId}")
    Try(config.getString("account-type")).getOrElse("memory") match {
      case "memory" => new MemoryAccount(userId)
      case _ => throw new IllegalArgumentException("Unknown account type.")
    }
  }
}

case class MemoryAccount(val userId:Int,ledger:Seq[Transaction] = Seq.empty[Transaction]) extends Account {
  import Account._
  private def sumLedger(isBase:Boolean):Double = {
    ledger.foldLeft(0.0) {
      case (acc,ele) if isBase =>
        acc + ele.baseCurrencyAmount
      case (acc,ele) =>
        acc + ele.otherCurrencyAmount
    }
  }

  lazy val baseCurrencyAmount = sumLedger(true)

  lazy val otherCurrencyAmount = sumLedger(false)

  def submitTransaction(t:Transaction):Account = {
    val tWithId = if(t.id.isEmpty) t.copy(id = Some(idMaker.getAndIncrement))
                  else t
    val tWithTime = if(tWithId.time.isEmpty) tWithId.copy(time = Some(System.currentTimeMillis()))
                    else tWithId

    this.copy(ledger = ledger :+ tWithTime)
  }

}
trait Bank {
  def getAccountFor(userId:Int):Option[Account]

  def submitTransaction(t:Transaction):Bank
}
object Bank {
  def apply():Bank = {
    Try(config.getString("bank-type")).getOrElse("memory") match {
      case "memory" => new MemoryBank()
      case _ => throw new IllegalArgumentException("Unknown bank type.")
    }
  }

  val logger = LoggerFactory.getLogger(classOf[Bank])
}
class MemoryBank(userIdToAccount:Map[Int,Account] = Map.empty[Int,Account]) extends Bank {
  import Bank._

  def getAccountFor(userId:Int):Option[Account] = userIdToAccount.get(userId)


  def submitTransaction(t:Transaction):Bank = {
    getAccountFor(t.userId).map{acct =>
      new MemoryBank(userIdToAccount + (t.userId -> acct.submitTransaction(t)))
    } getOrElse{
      new MemoryBank(userIdToAccount + (t.userId -> Account(t.userId).submitTransaction(t)))
    }
  }

  def depositBaseCurrency(userId:Int,depositAmount:Double) = {
    submitTransaction(Transaction(userId = userId,baseCurrencyAmount = depositAmount,otherCurrencyAmount = 0,transactionType = TransactionType.Deposit))
  }

  def depositOtherCurrency(userId:Int,depositAmount:Int) = {
    submitTransaction(Transaction(userId = userId,baseCurrencyAmount = 0.0,otherCurrencyAmount = depositAmount,transactionType = TransactionType.Deposit))
  }

  def settleOrders(order1:Order,order2:Order):Bank = {
    assert((order1.isBid && !order2.isBid) || (!order1.isBid && order2.isBid), "can not have two orders be bid or two orders be ask")
    assert(order1.settledPrice == order2.settledPrice, "settlePrices must be equal.")

    val (bidOrder,askOrder) = if(order1.isBid) order1 -> order2
    else order2 -> order1

    val bidAcct = userIdToAccount.getOrElse(bidOrder.userId,Account(bidOrder.userId))
    val askAcct = userIdToAccount.getOrElse(askOrder.userId,Account(askOrder.userId))

    val bidTotalAmount = bidOrder.settledAmount.toDouble * bidOrder.settledPrice
    val askTotalAmount = askOrder.settledAmount.toDouble * askOrder.settledPrice

    val updatedBidAcct = bidAcct.submitTransaction(Transaction(bidAcct.userId,baseCurrencyAmount = bidTotalAmount * -1.0,otherCurrencyAmount = bidOrder.amount,transactionType = TransactionType.Buy))
    val updatedAskAcct = askAcct.submitTransaction(Transaction(askAcct.userId,baseCurrencyAmount = askTotalAmount,otherCurrencyAmount = askOrder.amount * -1,transactionType = TransactionType.Sell))

    new MemoryBank(userIdToAccount + (bidOrder.userId -> updatedBidAcct, askOrder.userId -> updatedAskAcct))


  }


}
