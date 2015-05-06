package net.kornstar.exchange

import scala.annotation.tailrec
import scala.collection.immutable.{SortedSet}
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.util.Try

/**
 * Created by Ben Kornmeier on 5/4/2015.
 */

object OrderBook {
  val logger = LoggerFactory.getLogger(classOf[OrderBook])
  val askOrdering = new Ordering[Order]{
    def compare(x:Order,y:Order) = {
      x.price.compareTo(y.price)
    }
  }
  val bidOrdering = askOrdering.reverse

  def askSet = SortedSet.empty[Order](askOrdering) //Lowest first (sell)
  def bidSet = SortedSet.empty[Order](bidOrdering) //Highest first (buy)


  case class Order(id:Int,timeCreated:Long,isBid:Boolean,amount:Int,remainingAmount:Int,price:Double,settledPrice:Double = 0.0,timeSettled:Option[Long] = None){
    def hashcode:Int = id
  }

  object Order {
    def apply(id:Int,timeCreated:Long,isBid:Boolean,amount:Int,price:Double):Order = {
      Order(id,timeCreated,isBid,amount,amount,price)
    }
  }
  //TODO use SortedMap id -> Order
  case class OrderBook(assetId:Int,bids:SortedSet[Order],asks:SortedSet[Order],fullFilledOrders:List[Order] = List.empty[Order]) {

    def submit(o:Order) = {
      lazy val settlingTime = System.currentTimeMillis()
      val (newBidSet,newAskSet) = if(o.isBid) {
        bids + o -> asks
      } else {
        bids -> (asks + o)
      }

      @tailrec //TODO refactor to just take OrderBook
      def _match(bids:SortedSet[Order],asks:SortedSet[Order],fullFilledOrders:List[Order]):OrderBook = {
        val topBidOpt = bids.headOption
        val topAskOpt = asks.headOption

        if(topBidOpt.isEmpty || topAskOpt.isEmpty) {
          logger.debug(s"One or more sets are empty")
          this.copy(bids = bids,asks = asks,fullFilledOrders = fullFilledOrders)
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
                _match(bids.tail + mostQuantityOrder,asks.tail,leastQuantityOrder :: fullFilledOrders)
              } else {
                _match(bids.tail ,asks.tail + mostQuantityOrder,leastQuantityOrder :: fullFilledOrders)
              }
            } else {
              _match(bids.tail,asks.tail,mostQuantityOrder.copy(timeSettled = Some(settlingTime)) :: leastQuantityOrder :: fullFilledOrders)
            }
          } else {
            this.copy(bids = bids,asks = asks,fullFilledOrders = fullFilledOrders)
          }
        }
      }
      _match(newBidSet,newAskSet,fullFilledOrders)
    }

    def cancel(id:Int):Try[(OrderBook,Order)] = { //TODO check and make sure the Order is not already partialy filled
      val potentialBid = bids.find(_.id == id)
      val potentialAsk = asks.find(_.id == id)

      if(potentialAsk.isDefined) {
        Try{potentialAsk.get}.flatMap(cancel(_))
      } else {
        Try{potentialBid.get}.flatMap(cancel(_))
      }
    }

    def cancel(o:Order):Try[(OrderBook,Order)] = Try {
      assert(o.remainingAmount == o.amount,"Order is already half filled!")
      this.copy(bids = bids - o,asks = asks - o) -> o
    }

    def get(id:Int):Option[Order] = {
      val potentialBidOrder = bids.find(_.id == id)
      if(potentialBidOrder.isDefined)
        potentialBidOrder
      else asks.find(_.id == id)

    }

  }

  def apply(assetId:Int) ={
    new OrderBook(assetId,bidSet,askSet)
  }
}

