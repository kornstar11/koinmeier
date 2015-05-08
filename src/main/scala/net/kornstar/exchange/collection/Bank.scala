package net.kornstar.exchange.collection

/**
 * Created by Ben Kornmeier on 5/7/2015.
 */

case class Transaction(id:Int,userId:Int,isBid:Boolean,baseCurrencyAmount:Double, otherCurrencyAmount:Double,orderOpt:Option[Order] = None)

trait Account {
  def baseCurrencyAmount:Double
  def otherCurrencyAmount:Double

  def submitTransaction(t:Transaction):Account
}

class MemoryAccount(userId:Int,ledger:Seq[Transaction] = Seq.empty[Transaction]) extends Account {
  protected def sumLedger(isBase:Boolean):Double = {
    ledger.foldLeft(0.0) {
      case (acc,ele) if isBase =>
        acc + ele.baseCurrencyAmount
      case (acc,ele) =>
        acc + ele.otherCurrencyAmount
    }
  }

  lazy val baseCurrencyAmount = sumLedger(true)

  lazy val otherCurrencyAmount = sumLedger(false)

}
trait Bank {
  def getAccountFor(userId:Int):Option[Account]
}
object Bank {
}
class MemoryBank(userIdToAccount:Map[Int,Account] = Map.empty[Int,Account]) extends Bank {
  import Bank._

  def getAccountFor(userId:Int):Option[Account] = userIdToAccount.get(userId)


  def submitTransaction(t:Transaction):Bank = {
    getAccountFor(t.userId).map{acct =>
      new MemoryBank(userIdToAccount + (t.userId -> acct.submitTransaction(t)))
    } getOrElse(this)
  }


}
