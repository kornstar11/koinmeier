package net.kornstar.exchange.collection

import net.kornstar.exchange.streams.messages.OrderBookStats
import org.slf4j.LoggerFactory

/**
 * Created by Ben Kornmeier on 5/9/2015.
 */
object Exchange {
  val logger = LoggerFactory.getLogger(classOf[Exchange])

  def apply() = new MemoryExchange()
}
trait Exchange {
  def placeOrder(userId:Int,price:Double,amount:Int,isBid:Boolean):(Exchange,Order)
  def cancelOrder(orderId:Int):(Exchange,Option[Order])
  def market:OrderBookStats
  def bank:Bank
  def openOrders(userId:Int):Iterable[Order]
  def openOrders:Iterable[Order]

  def depositBaseCurrency(userId:Int,depositAmount:Double):Exchange

  def depositOtherCurrency(userId:Int,depositAmount:Int):Exchange


}
class MemoryExchange(orderBook:OrderBook = OrderBook(0),val bank:Bank = Bank()) extends Exchange {
  import Exchange._
  def placeOrder(userId:Int,price:Double,amount:Int,isBid:Boolean):(Exchange,Order) = {
    val (newBank,order) = bank.createOrder(userId,price,amount,isBid)
    val newOrderBook = orderBook.submit(order)

    val settledBank = newOrderBook.fullFilledOrders.foldLeft(bank){
      case (acc,ele) =>
        acc.settleOrder(ele)
    }

    new MemoryExchange(newOrderBook,settledBank) -> order
  }

  def cancelOrder(orderId:Int):(Exchange,Option[Order]) = {
    val (newOrderBook,orderOpt) = orderBook.cancel(orderId)

    val newBank = orderOpt.map{o =>
      bank.refundOrder(o)
    } getOrElse(bank)

    new MemoryExchange(newOrderBook,bank) -> orderOpt
  }

  def openOrders(userId:Int):Iterable[Order] = {
    openOrders.filter(_.userId == userId)
  }

  def depositBaseCurrency(userId:Int,depositAmount:Double):Exchange = {
    val newBank = bank.depositBaseCurrency(userId,depositAmount)
    new MemoryExchange(orderBook,newBank)
  }

  def depositOtherCurrency(userId:Int,depositAmount:Int):Exchange = {
    val newBank = bank.depositOtherCurrency(userId,depositAmount)
    new MemoryExchange(orderBook,newBank)
  }

  def openOrders:Iterable[Order] = orderBook.asks.toSeq ++ orderBook.bids.toSeq

  val market = orderBook.market



}
