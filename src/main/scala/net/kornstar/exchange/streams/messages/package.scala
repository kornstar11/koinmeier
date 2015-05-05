package net.kornstar.exchange.streams

import java.util.concurrent.atomic.AtomicInteger

import net.kornstar.exchange.OrderBook.Order
import play.api.libs.json._ // JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._

/**
 * Created by Ben Kornmeier on 5/5/2015.
 */
package object messages {
  case object Tick

  val id = new AtomicInteger(0)

  implicit val dataReads:Reads[Order] = (
    (JsPath \ "amount").read[Int] and
      (JsPath \ "price").read[Double] and
      (JsPath \ "isBid").read[Boolean]
    )( (amount:Int,price:Double,isBid:Boolean) => {
    Order(id.getAndIncrement,System.currentTimeMillis(),isBid,amount,price)
  } )

}
