package net.kornstar.exchange.streams

import akka.actor.{ActorLogging, Actor}
import akka.stream.actor.{ActorSubscriberMessage, OneByOneRequestStrategy, ActorPublisher, ActorSubscriber}
import net.kornstar.exchange.{OrderBook =>OB}
import net.kornstar.exchange.OrderBook.{OrderBook, Order}
import net.kornstar.exchange.streams.messages.Tick
import scala.concurrent.duration._

import scala.util.Try

/**
 * Created by ben on 5/5/15.
 */
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
    case ActorSubscriberMessage.OnNext(o:Order) =>
      become(running(ob.submit(o)))
      sender() ! 1

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
