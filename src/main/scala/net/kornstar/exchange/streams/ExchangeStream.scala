package net.kornstar.exchange.streams

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.scaladsl.{ Flow, Sink }
import akka.util.ByteString
import net.kornstar.exchange.OrderBook.Order
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.concurrent.Future
import net.kornstar.exchange.streams.messages._
/**
 * Created by Ben Kornmeier on 5/4/2015.
 */
object ExchangeStream {
  val logger = LoggerFactory.getLogger("ExchangeStream")
  def apply()(implicit system:ActorSystem,materializer: ActorFlowMaterializer) = {
    logger.debug("here")
    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http(system).bind(interface = "localhost", port = 8081)

    val bindingFuture: Future[Http.ServerBinding] =serverSource.to(Sink.foreach { connection =>
      logger.info("Accepted new connection from " + connection.remoteAddress)

      connection handleWith {
        Flow[HttpRequest].mapAsync(3) {
          case e@HttpRequest(POST, Uri.Path("/order"), headers, rEntity:RequestEntity, _) =>
            logger.info(s"Req: ${e}")
            rEntity.dataBytes.runFold(ByteString.empty){
              case (acc,b) =>
                acc ++ b
            }.map{b =>
              val j = Json.parse(b.toArray).as[Order]//b.toArray
              (GET,headers.seq,j)
            }
          case e: HttpRequest =>
            Future.failed(new Exception("404")) //(GET,Seq.empty[HttpHeader],ByteString.empty))
        } map {case(method,headers,data) =>
          HttpResponse(200, entity = data.toString())
        }
      }
    }).run()

  }


}
