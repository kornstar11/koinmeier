package net.kornstar.exchange.streams

import akka.actor.{ActorLogging, Actor}
import akka.stream.actor.{ActorSubscriberMessage, OneByOneRequestStrategy, ActorPublisher, ActorSubscriber}
import net.kornstar.exchange.streams.OrderBookActor.Message.{GetMarket, GetOrder, CancelOrder, PlaceOrder}
import net.kornstar.exchange.collection.OrderBook
import net.kornstar.exchange.collection.Order
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
    become(running(OrderBook(1)))
    system.scheduler.schedule(5 second,5 second,self,Tick)
    super.preStart()
  }

  def running(ob:OrderBook ):Receive = {
    case ActorSubscriberMessage.OnNext(PlaceOrder(o)) =>
      become(running(ob.submit(o)))
      sender() ! o
    case ActorSubscriberMessage.OnNext(CancelOrder(id)) =>
      val (nOrderBook, orderOpt) = ob.cancel(id)
      become(running(nOrderBook))
      sender() ! orderOpt

    case ActorSubscriberMessage.OnNext(GetOrder(id)) =>
      val potentialOrder = ob.get(id)
      sender() ! potentialOrder

    case ActorSubscriberMessage.OnNext(GetMarket) =>
      sender() ! ob.market

    case Tick =>
      log.info(s"New fullfilled orders: ${ob.fullFilledOrders}")
      log.info(s"Asks: ${ob.asks}")
      log.info(s"Bids: ${ob.bids}")
      become(running(ob.copy(fullFilledOrders = List.empty[Order])))
  }

  def receive:Receive = {
    case _ =>

  }

}
