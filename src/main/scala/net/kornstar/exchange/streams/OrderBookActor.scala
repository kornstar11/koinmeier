package net.kornstar.exchange.streams

import akka.actor.{ActorLogging, Actor}
import akka.stream.actor.{ActorSubscriberMessage, OneByOneRequestStrategy, ActorPublisher, ActorSubscriber}
import net.kornstar.exchange.streams.OrderBookActor.Message.{GetOrder, CancelOrder, PlaceOrder}
import net.kornstar.exchange.{OrderBook =>OB}
import net.kornstar.exchange.OrderBook.{OrderBook, Order}
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

  }
}
class OrderBookActor extends Actor with ActorLogging {
  import context._

  //val requestStrategy = OneByOneRequestStrategy

  override def preStart() = {
    log.info(s"Orderbook actor starting.")
    become(running(OB(1)))
    system.scheduler.schedule(5 second,5 second,self,Tick)
    super.preStart()
  }

  def running(ob:OrderBook ):Receive = {
    case ActorSubscriberMessage.OnNext(PlaceOrder(o)) =>
      become(running(ob.submit(o)))
      sender() ! o.id
    case ActorSubscriberMessage.OnNext(CancelOrder(id)) =>
      ob.cancel(id) match {
        case Success((ob,o)) =>
          become(running(ob))
          sender() ! Success(o)

        case f@Failure(e) =>
          log.error(e,"Error canceling")
          sender() ! f
      }
    case ActorSubscriberMessage.OnNext(GetOrder(id)) =>
      val potentialOrder = ob.get(id)
      sender() ! potentialOrder

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
