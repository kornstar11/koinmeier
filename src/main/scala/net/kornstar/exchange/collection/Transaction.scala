package net.kornstar.exchange.collection

/**
 * Created by Ben Kornmeier on 5/9/2015.
 */
object Transaction {
  sealed trait TransactionType
  object TransactionType {
    case object Deposit extends TransactionType
    case object Withdraw extends TransactionType
    case object Buy extends TransactionType
    case object Sell extends TransactionType
    case class Refund(o:Order) extends TransactionType
  }
}

case class Transaction(userId:Int,baseCurrencyAmount:Double, otherCurrencyAmount:Int,transactionType:Transaction.TransactionType,time:Option[Long] = None,id:Option[Int] = None,note:Option[String] = None,orderOpt:Option[Order] = None)

