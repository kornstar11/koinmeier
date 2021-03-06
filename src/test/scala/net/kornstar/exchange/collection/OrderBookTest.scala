package net.kornstar.exchange.collection

import org.specs2.mutable.Specification

/**
 * Created by Ben Kornmeier on 5/4/2015.
 */
class OrderBookTest extends Specification {
  sequential
  "OrderBookTest" should {
    "bids and asks should be ordered right" in {
      val bids = OrderBook.bidSet + (Order(0,0,1L,true,1,1.1), Order(1,1,1L,true,1,2.0), Order(4,4,1L,true,1,2.0),Order(5,5,1L,true,1,1.1) )
      val asks = OrderBook.askSet + (Order(2,2,1L,true,1,1.1), Order(3,3,1L,true,1,2.0))
      println("")
      println(s"BIDS====${bids}")

      bids.head.price must be equalTo(2.0)
      asks.head.price must be equalTo(1.1)
      success
    }

    "not match a order when there is a spread" in {
      val bids = IndexedSeq(Order(0,0,1L,true,1,1.1), Order(1,1,1L,true,1,2.0))
      val asks = IndexedSeq(Order(2,2,1L,false,1,3.1), Order(3,3,1L,false,1,4.0))

      val orderBook = OrderBook(1)
      val ordersIn1 = orderBook.submit(bids(0)).submit(asks(0)).submit(bids(1)).submit(asks(1))

      ordersIn1.fullFilledOrders.isEmpty must be equalTo(true)

      ordersIn1.bids.contains(bids(0)) must be equalTo(true)
      ordersIn1.bids.contains(bids(1)) must be equalTo(true)

      ordersIn1.asks.contains(asks(0)) must be equalTo(true)
      ordersIn1.asks.contains(asks(1)) must be equalTo(true)
    }
    "match a exact order and remove from the orderBook" in {
      val bids = IndexedSeq(Order(0,0,1L,true,1,1.1), Order(1,1,1L,true,1,2.0), Order(2,2,1L,true,1,2.1),Order(4,4,1L,true,1,3.1)  )
      val asks = IndexedSeq(Order(5,5,1L,false,1,3.1), Order(6,6,1L,false,1,4.0))

      val orderBook = OrderBook(1)
      val ordersIn1 = orderBook.submit(bids(0)).submit(asks(0)).submit(bids(1)).submit(asks(1)).submit(bids(2))
      ordersIn1.fullFilledOrders.isEmpty must be equalTo(true)

      val ordersIn2 = ordersIn1.submit(bids(3))
      ordersIn2.fullFilledOrders.isEmpty must be equalTo(false)

      ordersIn2.fullFilledOrders.count(_.id == 4) mustEqual(1)
      ordersIn2.fullFilledOrders.count(_.id == 5) mustEqual(1)
      success
    }

    "match a 2 order that make it exact" in {
      val bids = IndexedSeq(Order(1,1,1L,true,1,1.1), Order(2,2,1L,true,1,2.0), Order(3,3,1L,true,1,3.1),Order(4,4,1L,true,1,3.1)  )
      val asks = IndexedSeq(Order(5,5,1L,false,2,3.1), Order(6,6,1L,false,1,4.0))

      val orderBook = OrderBook(1)
      val ordersIn1 = orderBook.submit(bids(0)).submit(asks(0)).submit(bids(1)).submit(asks(1)).submit(bids(2))
      println(ordersIn1)
      ordersIn1.fullFilledOrders.isEmpty must be equalTo(false)
      ordersIn1.fullFilledOrders.count(_.id == 3) mustEqual(1)
      ordersIn1.bids.count(_.id == 3) mustEqual(0)
      val clearedOrderBook = ordersIn1.copy(fullFilledOrders = List.empty[Order])

      val ordersIn2 = clearedOrderBook.submit(bids(3))
      println(ordersIn2)
      ordersIn2.fullFilledOrders.isEmpty must be equalTo(false)
      ordersIn2.fullFilledOrders.count(_.id == 4) mustEqual(1)
      ordersIn2.fullFilledOrders.count(_.id == 5) mustEqual(1)

      ordersIn2.bids.count(_.id == 5) mustEqual(0)
      ordersIn2.asks.count(_.id == 5) mustEqual(0)

      ordersIn2.bids.count(_.id == 4) mustEqual(0)
      ordersIn2.asks.count(_.id == 4) mustEqual(0)

    }
  }

}
