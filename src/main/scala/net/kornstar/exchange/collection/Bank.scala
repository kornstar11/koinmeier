package net.kornstar.exchange.collection

import org.slf4j.LoggerFactory

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
}

case class MemoryAccount(userId:Int,ledger:Seq[Transaction] = Seq.empty[Transaction]) extends Account {
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
}
object Bank {
  val logger = LoggerFactory.getLogger(classOf[Bank])
}
class MemoryBank(userIdToAccount:Map[Int,Account] = Map.empty[Int,Account]) extends Bank {
  import Bank._

  def getAccountFor(userId:Int):Option[Account] = userIdToAccount.get(userId)


  def submitTransaction(t:Transaction):Bank = {
    getAccountFor(t.userId).map{acct =>
      new MemoryBank(userIdToAccount + (t.userId -> acct.submitTransaction(t)))
    } getOrElse{
      logger.warn(s"Transaction user ID not found ${t}")
      this
    }
  }


}
