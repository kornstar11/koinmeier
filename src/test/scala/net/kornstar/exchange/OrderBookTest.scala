package net.kornstar.exchange

import org.specs2.mutable.Specification

/**
 * Created by Ben Kornmeier on 5/4/2015.
 */
class OrderBookTest extends Specification {
  sequential
  "OrderBookTest" should {
    "bids and asks should be ordered right" in {
      val bids = OrderBook.bidSet + (OrderBook.Order(1,1L,true,1,1.1), OrderBook.Order(1,1L,true,1,2.0))
      val asks = OrderBook.askSet + (OrderBook.Order(1,1L,true,1,1.1), OrderBook.Order(1,1L,true,1,2.0))


      bids.head.price must be equalTo(2.0)
      asks.head.price must be equalTo(1.1)
      success
    }

    "match a order" in {

      success
    }
  }

}
