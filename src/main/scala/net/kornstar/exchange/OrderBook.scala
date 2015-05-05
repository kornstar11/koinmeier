package net.kornstar.exchange

import scala.annotation.tailrec
import scala.collection.immutable.{SortedSet}

/**
 * Created by Ben Kornmeier on 5/4/2015.
 */

object OrderBook {
  val askOrdering = new Ordering[Order]{
    def compare(x:Order,y:Order) = {
      x.price.compareTo(y.price)
    }
  }
  val bidOrdering = askOrdering.reverse

  def askSet = SortedSet.empty[Order](askOrdering) //Lowest first (sell)
  def bidSet = SortedSet.empty[Order](bidOrdering) //Highest first (buy)


  case class Order(id:Int,timeCreated:Long,isBid:Boolean,amount:Int,price:Double,settledPrice:Double = 0.0,timeSettled:Option[Long] = None){
    def hashcode:Int = id
  }

  class OrderBook(assetId:Int,bids:SortedSet[Order],asks:SortedSet[Order],fullFilledOrders:List[Order] = List.empty[Order]) {

    def submit(o:Order) = {
      val (naturalSet,opposingSet) = if(o.isBid) {
        bids + o -> asks
      } else {
        asks + o -> bids
      }

      @tailrec
      def _match(bids:SortedSet[Order],asks:SortedSet[Order],fullFilledOrders:List[Order]):OrderBook = {
        val topBidOpt = bids.headOption
        val topAskOpt = asks.headOption

        if(topBidOpt.isEmpty || topAskOpt.isEmpty) {
          this
        } else {
          val topBid = topBidOpt.get
          val topAsk = topAskOpt.get
          if(topBid.price >= topAsk.price) { //Do the prices intersect?
            val averagePrice = (topBid.price + topAsk.price) / 2 //Give the settledPrice as the avereage
            val (mostQuantityOrder,leastQuantityOrder) = if(topBid.amount > topAsk.amount) topBid.copy(amount = topBid.amount - topAsk.amount,settledPrice = averagePrice) -> topAsk.copy(amount = 0,settledPrice = averagePrice)
                                                         else topAsk.copy(amount = topAsk.amount - topBid.amount,settledPrice = averagePrice) -> topBid.copy(amount = 0,settledPrice = averagePrice)
            if(mostQuantityOrder.amount > 0){
              if(mostQuantityOrder.isBid) {
                _match(bids.tail + mostQuantityOrder,asks.tail,leastQuantityOrder :: fullFilledOrders)
              } else {
                _match(bids.tail ,asks.tail + mostQuantityOrder,leastQuantityOrder :: fullFilledOrders)
              }
            } else {
              _match(bids.tail,asks.tail,mostQuantityOrder :: leastQuantityOrder :: fullFilledOrders)
            }
          } else {
            this
          }
        }
      }
      _match(bids,asks,fullFilledOrders)
    }
  }

  def apply(assetId:Int) ={
    new OrderBook(assetId,bidSet,askSet)
  }
}

