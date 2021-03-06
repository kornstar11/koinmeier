package net.kornstar.exchange.streams

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorRef
import net.kornstar.exchange.collection.{Account, OrderBook, Order}
import play.api.libs.json._ // JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._

/**
 * Created by Ben Kornmeier on 5/5/2015.
 */
package object messages { //TODO Order goes here
  case object Tick
  case class OrderBookStats(ask:Option[Order] = None,bid:Option[Order] = None, last:Option[Order] = None)
  case class Deposit(userId:Int,amount:Double,isBase:Boolean)
  case class RegisterWs(userId:Int,stream:ActorRef)
  //case class ChatMessage
  case class JsonError(error:String)

  implicit val depositReads:Reads[Deposit] = (
    (JsPath \ "userId").read[Int] and
      (JsPath \ "amount").read[Double] and
      (JsPath \ "isBase").read[Boolean]
    )( (userId:Int,amount:Double,isBase:Boolean) => Deposit(userId,amount,isBase) )

  implicit val accountWrites:Writes[Account] = (
    (JsPath \ "baseAmount").write[Double] and
      (JsPath \ "otherAmount").write[Double]
    )(a => (a.baseCurrencyAmount,a.otherCurrencyAmount))

  implicit val orderReads:Reads[Order] = (
    (JsPath \ "userId").read[Int] and
    (JsPath \ "amount").read[Int] and
      (JsPath \ "price").read[Double] and
      (JsPath \ "isBid").read[Boolean]
    )( (userId:Int,amount:Int,price:Double,isBid:Boolean) => {
    Order(userId,isBid,amount,price)
  } )
  implicit val orderWrites:Writes[Order] = (
    (JsPath \ "userId").write[Int] and
    (JsPath \ "id").write[Int] and
    (JsPath \ "amount").write[Int] and
      (JsPath \ "price").write[Double] and
      (JsPath \ "isBid").write[Boolean]
  )(o => (o.userId,o.id,o.remainingAmount,o.price,o.isBid))

  implicit val simpleOrderBookStatsWrites:Writes[OrderBookStats] = (
    (JsPath \ "ask").write[Double] and
      (JsPath \ "bid").write[Double] and
      (JsPath \ "last").write[Double]
    )(obs => (obs.ask.map(_.price).getOrElse(0.0),obs.bid.map(_.price).getOrElse(0.0),obs.last.map(_.price).getOrElse(0.0)))

  implicit val jsonErrorWrites:Writes[JsonError] = Writes[JsonError](jsErr => JsObject(Seq("error" -> JsString(jsErr.error))))




}
