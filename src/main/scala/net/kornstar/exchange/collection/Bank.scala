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

  def getLedger:Iterable[Transaction]

  def submitTransaction(t:Transaction):Account

  def createOrder(price:Double,amount:Int,isBid:Boolean):(Account,Order)

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

  def getLedger = ledger

  lazy val baseCurrencyAmount = sumLedger(true)

  lazy val otherCurrencyAmount = sumLedger(false)

  def submitTransaction(t:Transaction):Account = {
    val tWithId = if(t.id.isEmpty) t.copy(id = Some(idMaker.getAndIncrement))
                  else t
    val tWithTime = if(tWithId.time.isEmpty) tWithId.copy(time = Some(System.currentTimeMillis()))
                    else tWithId

    this.copy(ledger = ledger :+ tWithTime)
  }

  def createOrder(price:Double,amount:Int,isBid:Boolean):(Account,Order) = {
    val amountInDrawedFromLedger = if(isBid) baseCurrencyAmount
                               else otherCurrencyAmount

    val proposedTotalPrice = price * amount.toDouble

    assert(proposedTotalPrice <= amountInDrawedFromLedger, s"unable to make a order of ${proposedTotalPrice} because only ${amountInDrawedFromLedger} is avialiable.")

    val newAcct = if(isBid) submitTransaction(Transaction(userId,proposedTotalPrice * -1.0,0,TransactionType.Buy,Some(System.currentTimeMillis()),Some(idMaker.getAndIncrement)))
                  else submitTransaction(Transaction(userId, 0.0, (proposedTotalPrice * -1.0).toInt, TransactionType.Sell, Some(System.currentTimeMillis()), Some(idMaker.getAndIncrement)))

    val order = Order(userId,isBid,amount,price)

    newAcct -> order

  }

}
trait Bank {
  def getAccountFor(userId:Int):Option[Account]

  protected def submitTransaction(t:Transaction):Bank

  def depositBaseCurrency(userId:Int,depositAmount:Double):Bank

  def depositOtherCurrency(userId:Int,depositAmount:Int):Bank

  def settleOrders(order1:Order,order2:Order):Bank

  def createOrder(userId:Int,price:Double,amount:Int,isBid:Boolean):(Bank,Order)

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

  def depositBaseCurrency(userId:Int,depositAmount:Double):Bank = {
    submitTransaction(Transaction(userId = userId,baseCurrencyAmount = depositAmount,otherCurrencyAmount = 0,transactionType = TransactionType.Deposit))
  }

  def depositOtherCurrency(userId:Int,depositAmount:Int):Bank = {
    submitTransaction(Transaction(userId = userId,baseCurrencyAmount = 0.0,otherCurrencyAmount = depositAmount,transactionType = TransactionType.Deposit))
  }

  def createOrder(userId:Int,price:Double,amount:Int,isBid:Boolean):(Bank,Order) = {
    val acct = userIdToAccount.getOrElse(userId,Account(userId))

    val (newAcct,order) = acct.createOrder(price,amount,isBid)

    logger.debug(s"UserID ${userId} creating a new account and order ${newAcct} -> ${order} ")

    val newBank = new MemoryBank(userIdToAccount + (userId -> newAcct))

    newBank -> order
  }

  def settleOrders(order1:Order,order2:Order):Bank = {
    assert((order1.isBid && !order2.isBid) || (!order1.isBid && order2.isBid), "can not have two orders be bid or two orders be ask")
    assert(order1.settledPrice == order2.settledPrice, "settlePrices must be equal.")


    val (bidOrder,askOrder) = if(order1.isBid) order1 -> order2
    else order2 -> order1

    logger.debug(s"Attempting to settle the following orders: \n Bid: ${bidOrder} -> ${bidOrder.settledAmount} \n Ask: ${askOrder} -> ${askOrder.settledAmount}")

    val bidAcct = userIdToAccount.getOrElse(bidOrder.userId,Account(bidOrder.userId))
    val askAcct = userIdToAccount.getOrElse(askOrder.userId,Account(askOrder.userId))

    val bidTotalAmount = bidOrder.settledAmount.toDouble * bidOrder.settledPrice
    val askTotalAmount = askOrder.settledAmount.toDouble * askOrder.settledPrice

    logger.debug(s"\n Bid amt: ${bidTotalAmount} \n Ask amt: ${askTotalAmount}")

    val updatedBidAcct = bidAcct.submitTransaction(Transaction(bidAcct.userId,baseCurrencyAmount = bidTotalAmount * -1.0,otherCurrencyAmount = bidOrder.amount,transactionType = TransactionType.Buy, orderOpt = Some(bidOrder)))
    val updatedAskAcct = askAcct.submitTransaction(Transaction(askAcct.userId,baseCurrencyAmount = askTotalAmount,otherCurrencyAmount = askOrder.amount * -1,transactionType = TransactionType.Sell, orderOpt = Some(askOrder)))

    new MemoryBank(userIdToAccount + (bidOrder.userId -> updatedBidAcct, askOrder.userId -> updatedAskAcct))
  }


}
