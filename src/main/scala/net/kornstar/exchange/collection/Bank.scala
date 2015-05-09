package net.kornstar.exchange.collection

import java.util.concurrent.atomic.AtomicInteger

import net.kornstar.exchange.collection.Transaction.TransactionType
import org.slf4j.LoggerFactory
import net.kornstar.exchange.global._

import scala.util.Try

/**
 * Created by Ben Kornmeier on 5/7/2015.
 */

trait Bank {
  def getAccountFor(userId:Int):Option[Account]

  protected def submitTransaction(t:Transaction):Bank

  def depositBaseCurrency(userId:Int,depositAmount:Double):Bank

  def depositOtherCurrency(userId:Int,depositAmount:Int):Bank

  def settleOrders(order1:Order,order2:Order):Bank

  def createOrder(userId:Int,price:Double,amount:Int,isBid:Boolean):(Bank,Order)

  def refundOrder(order:Order):Bank

}
object Bank {
  val logger = LoggerFactory.getLogger(classOf[Bank])

  def apply():Bank = {
    Try(config.getString("bank-type")).getOrElse("memory") match {
      case "memory" => new MemoryBank()
      case _ => throw new IllegalArgumentException("Unknown bank type.")
    }
  }

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

  def refundOrder(order:Order):Bank = {
    val acct = userIdToAccount(order.userId).refundOrder(order)
    new MemoryBank(userIdToAccount + (order.userId -> acct))
  }


}
