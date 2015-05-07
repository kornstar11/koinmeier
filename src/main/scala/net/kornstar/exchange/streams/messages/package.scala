package net.kornstar.exchange.streams

import java.util.concurrent.atomic.AtomicInteger

import net.kornstar.exchange.collection.{OrderBook, Order}
import play.api.libs.json._ // JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._

/**
 * Created by Ben Kornmeier on 5/5/2015.
 */
package object messages { //TODO Order goes here
  case object Tick
  case class OrderBookStats(ask:Option[Order] = None,bid:Option[Order] = None, last:Option[Order] = None) {
  }
  case class JsonError(error:String)

  val id = new AtomicInteger(0)

  implicit val orderReads:Reads[Order] = (
    (JsPath \ "amount").read[Int] and
      (JsPath \ "price").read[Double] and
      (JsPath \ "isBid").read[Boolean]
    )( (amount:Int,price:Double,isBid:Boolean) => {
    Order(id.getAndIncrement,System.currentTimeMillis(),isBid,amount,price)
  } )
  implicit val orderWrites:Writes[Order] = (
    (JsPath \ "id").write[Int] and
    (JsPath \ "amount").write[Int] and
      (JsPath \ "price").write[Double] and
      (JsPath \ "isBid").write[Boolean]
  )(o => (o.id,o.amount,o.price,o.isBid))

  implicit val simpleOrderBookStatsWrites:Writes[OrderBookStats] = (
    (JsPath \ "ask").write[Double] and
      (JsPath \ "bid").write[Double] and
      (JsPath \ "last").write[Double]
    )(obs => (obs.ask.map(_.price).getOrElse(0.0),obs.bid.map(_.price).getOrElse(0.0),obs.last.map(_.price).getOrElse(0.0)))

  implicit val jsonErrorWrites:Writes[JsonError] = Writes[JsonError](jsErr => JsObject(Seq("error" -> JsString(jsErr.error))))




}
