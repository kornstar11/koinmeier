package net.kornstar.exchange.collection

import net.kornstar.exchange.streams.messages.OrderBookStats
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.immutable.SortedSet

/**
 * Created by Ben Kornmeier on 5/4/2015.
 */

object OrderBook {
  val logger = LoggerFactory.getLogger(classOf[OrderBook])
  val askOrdering = new Ordering[Order]{
    def compare(x:Order,y:Order) = {
      val priceCompare = x.price.compareTo(y.price)
      if(priceCompare == 0)
        x.id.compareTo(y.id)
      else
        priceCompare
    }
  }
  val bidOrdering = askOrdering.reverse

  def askSet = SortedSet.empty[Order](askOrdering) //Lowest first (sell)
  def bidSet = SortedSet.empty[Order](bidOrdering) //Highest first (buy)




  def apply(assetId:Int) ={
    new OrderBook(assetId,bidSet,askSet)
  }
}

case class OrderBook(assetId:Int,bids:SortedSet[Order],asks:SortedSet[Order],fullFilledOrders:List[Order] = List.empty[Order],lastOrder:Option[Order] = None) {
  import OrderBook._
  val ask:Option[Order] = asks.headOption
  val bid:Option[Order] = bids.headOption
  val last = lastOrder
  val market = OrderBookStats(ask,bid,last)

  def submit(o:Order) = {
    lazy val settlingTime = System.currentTimeMillis()
    val (newBidSet,newAskSet) = if(o.isBid) {
      bids + o -> asks
    } else {
      bids -> (asks + o)
    }

    @tailrec
    def _match(ob:OrderBook):OrderBook = {
      val topBidOpt = ob.bids.headOption
      val topAskOpt = ob.asks.headOption

      if(topBidOpt.isEmpty || topAskOpt.isEmpty) {
        logger.debug(s"One or more sets are empty")
        ob
      } else {
        val topBid = topBidOpt.get
        val topAsk = topAskOpt.get
        if(topBid.price >= topAsk.price) { //Do the prices intersect?
          logger.debug(s"TopBid crosses the topAsk. Will fulfil ")
          val averagePrice = (topBid.price + topAsk.price) / 2 //Give the settledPrice as the avereage
          val (mostQuantityOrder,leastQuantityOrder) = if(topBid.remainingAmount > topAsk.remainingAmount) topBid.copy(remainingAmount = topBid.remainingAmount - topAsk.remainingAmount,settledPrice = averagePrice) -> topAsk.copy(remainingAmount = 0,settledPrice = averagePrice,timeSettled = Some(settlingTime))
            else topAsk.copy(remainingAmount = topAsk.remainingAmount - topBid.remainingAmount,settledPrice = averagePrice) -> topBid.copy(remainingAmount = 0,settledPrice = averagePrice,timeSettled = Some(settlingTime))
          if(mostQuantityOrder.remainingAmount > 0){
            logger.debug(s"${mostQuantityOrder} still has remaining.")
            if(mostQuantityOrder.isBid) {
              _match(ob.copy(bids = ob.bids.tail + mostQuantityOrder,asks = ob.asks.tail,fullFilledOrders = leastQuantityOrder :: ob.fullFilledOrders,lastOrder = Some(leastQuantityOrder)))
            } else {
              _match(ob.copy(bids = ob.bids.tail ,asks = ob.asks.tail + mostQuantityOrder,fullFilledOrders = leastQuantityOrder :: ob.fullFilledOrders, lastOrder = Some(leastQuantityOrder)))
            }
          } else {
            _match(ob.copy(bids = ob.bids.tail,asks = ob.asks.tail,fullFilledOrders = mostQuantityOrder.copy(timeSettled = Some(settlingTime)) :: leastQuantityOrder :: fullFilledOrders,lastOrder = Some(mostQuantityOrder)))
          }
        } else {
          logger.debug(s"No intersection")
          ob
        }
      }
    }
    _match(this.copy(bids = newBidSet,asks = newAskSet))
  }

  def cancel(id:Int):(OrderBook,Option[Order]) = { //TODO check and make sure the Order is not already partialy filled
    val bidOpt = bids.find(_.id == id)
    val askOpt = asks.find(_.id == id)

    if(bidOpt.isDefined) {
      val bid = bidOpt.get
      (cancel(bid),Some(bid))
    } else if(askOpt.isDefined) {
      val ask = askOpt.get
      (cancel(ask),Some(ask))
    } else {
      (this,Option.empty[Order])
    }
  }

  protected def cancel(o:Order):OrderBook = {
    if(o.remainingAmount == o.amount) {
      this.copy(bids = bids - o,asks = asks - o)
    } else {
      logger.warn(s"Order is already half filled!")
      this
    }
  }

  def get(id:Int):Option[Order] = {
    val potentialBidOrder = bids.find(_.id == id)
    if(potentialBidOrder.isDefined)
      potentialBidOrder
    else asks.find(_.id == id)

  }
}

