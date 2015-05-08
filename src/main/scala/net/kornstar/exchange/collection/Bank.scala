package net.kornstar.exchange.collection

import org.slf4j.LoggerFactory
import com.typesafe.config._
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

case class Transaction(id:Int,userId:Int,isBid:Boolean,baseCurrencyAmount:Double, otherCurrencyAmount:Double,transactionType:Transaction.TransactionType,note:Option[String] = None,orderOpt:Option[Order] = None)

trait Account {
  def baseCurrencyAmount:Double
  def otherCurrencyAmount:Double

  def submitTransaction(t:Transaction):Account

  def userId:Int

}

object Account {
  def apply(userId:Int):Account = {
    Try(config.getString("account-type")).getOrElse("memory") match {
      case "memory" => new MemoryAccount(userId)
      case _ => throw new IllegalArgumentException("Unknown account type.")
    }
  }
}

case class MemoryAccount(val userId:Int,ledger:Seq[Transaction] = Seq.empty[Transaction]) extends Account {
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
    this.copy(ledger = ledger :+ t)
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


}
