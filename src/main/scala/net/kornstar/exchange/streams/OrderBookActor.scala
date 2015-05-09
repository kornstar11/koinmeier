package net.kornstar.exchange.streams

import akka.actor.{ActorLogging, Actor}
import akka.stream.actor.{ActorSubscriberMessage, OneByOneRequestStrategy, ActorPublisher, ActorSubscriber}
import net.kornstar.exchange.streams.OrderBookActor.Message.{GetMarket, GetOrder, CancelOrder, PlaceOrder}
import net.kornstar.exchange.collection.{Exchange, OrderBook, Order}
import net.kornstar.exchange.streams.messages.Tick
import scala.concurrent.duration._

import scala.util.{Failure, Success, Try}

/**
 * Created by ben on 5/5/15.
 */
object OrderBookActor {
  trait Message
  object Message {
    case class PlaceOrder(o:Order) extends Message
    case class CancelOrder(id:Int) extends Message
    case class GetOrder(id:Int) extends Message
    case object GetOrderBook extends Message
    case object GetMarket extends Message

  }
}
class OrderBookActor extends Actor with ActorLogging {
  import context._

  //val requestStrategy = OneByOneRequestStrategy

  override def preStart() = {
    log.info(s"Orderbook actor starting.")
    become(running(Exchange()))
    system.scheduler.schedule(5 second,5 second,self,Tick)
    super.preStart()
  }

  def running(e:Exchange ):Receive = {
    case ActorSubscriberMessage.OnNext(PlaceOrder(o)) =>
      val (ex,newOrder) = e.placeOrder(o.userId,o.price,o.amount,o.isBid)
      become(running(ex))
      sender() ! newOrder
    case ActorSubscriberMessage.OnNext(CancelOrder(id)) =>
      val (ex,orderOpt) = e.cancelOrder(id)
      become(running(ex))
      sender() ! orderOpt
    /*
    case ActorSubscriberMessage.OnNext(GetOrder(id)) =>
      val potentialOrder = ob.get(id)
      sender() ! potentialOrder
      */

    case ActorSubscriberMessage.OnNext(GetMarket) =>
      sender() ! e.market

    case Tick =>
      /*
      log.info(s"New fullfilled orders: ${e..fullFilledOrders}")
      log.info(s"Asks: ${ob.asks}")
      log.info(s"Bids: ${ob.bids}")
      become(running(ob.copy(fullFilledOrders = List.empty[Order])))
      */
  }

  def receive:Receive = {
    case _ =>

  }

}
