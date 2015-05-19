package net.kornstar.exchange.streams

import akka.actor._
import akka.stream.actor._
import net.kornstar.exchange.streams.OrderBookActor.Message._
import net.kornstar.exchange.collection.{Exchange, OrderBook, Order}
import net.kornstar.exchange.streams.messages.{RegisterWs, Deposit, Tick}
import scala.concurrent.duration._
import scala.collection.mutable.Map

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
    case class GetOrders(userId:Int) extends Message
    case object GetOrderBook extends Message
    case object GetMarket extends Message

    case class PlaceDeposit(d:Deposit) extends Message
    case class GetAccount(userId:Int) extends Message

  }
}

class OrderBookActor extends Actor with ActorLogging{
  import scala.collection.mutable.{Map => MMap}
  import context._

  var streams = Map.empty[Int,ActorRef]

  override def preStart() = {
    log.info(s"Orderbook actor starting.")
    become(running(Exchange()))
    system.scheduler.schedule(5 second,5 second,self,Tick)
    super.preStart()
  }

  def running(e:Exchange ):Receive = {
    case Terminated(ref) =>
      log.debug(s"Removing ${ref}")
      streams = streams.filterNot{
        case (userId,_ref) => _ref.equals(ref)
      }

    case ActorSubscriberMessage.OnNext(PlaceDeposit(d)) if d.isBase =>
      log.debug(s"Deposit message ${d}")
      become(running(e.depositBaseCurrency(d.userId,d.amount)))
      sender() ! ()

    case ActorSubscriberMessage.OnNext(PlaceDeposit(d)) =>
      log.debug(s"Deposit message ${d}")
      become(running(e.depositOtherCurrency(d.userId,d.amount.toInt)))
      sender() ! ()

    case ActorSubscriberMessage.OnNext(GetAccount(userId)) =>
      log.debug(s"Fetching acount for userID ${userId}")
      sender() ! e.bank.getAccountFor(userId)


    case ActorSubscriberMessage.OnNext(PlaceOrder(o)) =>
      Try(e.placeOrder(o.userId,o.price,o.amount,o.isBid)) match {
        case Success((ex,newOrder)) =>
          sender() ! newOrder
          become(running(ex))
        case Failure(e) =>
          log.error(e,"While trying to place a order!")
      }

    case ActorSubscriberMessage.OnNext(CancelOrder(id)) =>
      val (ex,orderOpt) = e.cancelOrder(id)
      become(running(ex))
      sender() ! orderOpt
    case ActorSubscriberMessage.OnNext(GetOrders(userId)) =>
      val potentialOrders = e.openOrders(userId)
      sender() ! potentialOrders

    case ActorSubscriberMessage.OnNext(GetMarket) =>
      sender() ! e.market

    case Tick =>
      streams.foreach{
        case (userId,ref) =>
          log.debug(s"sending to ref: ${ref}")
          ref ! e.market.toString
      }
      log.info(s"Streams: ${streams}")
      log.info(s"Exchange stats: ${e.market} \n ${e.openOrders}")
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
