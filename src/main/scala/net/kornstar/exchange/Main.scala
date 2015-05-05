package net.kornstar.exchange

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import net.kornstar.exchange.streams.ExchangeStream

/**
 * Created by Ben Kornmeier on 5/4/2015.
 */
object Main extends App{
  implicit val system = ActorSystem("exchange")
  implicit val mat = ActorFlowMaterializer()

  val stream = ExchangeStream.apply()


  system.awaitTermination()

}
