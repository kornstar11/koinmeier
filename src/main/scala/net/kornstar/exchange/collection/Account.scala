package net.kornstar.exchange.collection

import java.util.concurrent.atomic.AtomicInteger

import net.kornstar.exchange.collection.Transaction.TransactionType
import net.kornstar.exchange.global._
import org.slf4j.LoggerFactory

import scala.util.Try

/**
 * Created by Ben Kornmeier on 5/9/2015.
 */
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
