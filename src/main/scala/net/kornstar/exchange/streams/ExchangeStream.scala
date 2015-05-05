package net.kornstar.exchange.streams

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.scaladsl.{ Flow, Sink }
import akka.util.ByteString

import scala.concurrent.Future

/**
 * Created by Ben Kornmeier on 5/4/2015.
 */
object ExchangeStream {
  def apply()(implicit system:ActorSystem,materializer: ActorFlowMaterializer) = {
    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http(system).bind(interface = "localhost", port = 8080)

    val bindingFuture: Future[Http.ServerBinding] =serverSource.to(Sink.foreach { connection =>
      println("Accepted new connection from " + connection.remoteAddress)

      connection handleWith {
        Flow[HttpRequest].mapAsync(3) {
          case HttpRequest(GET, Uri.Path("/"), headers, rEntity:RequestEntity, _) =>
            rEntity.dataBytes.runFold(ByteString.empty){
              case (acc,b) =>
                acc ++ b
            }.map{b =>
              (GET,headers.seq,b)
            }
          case e: HttpRequest =>
            Future((GET,Seq.empty[HttpHeader],ByteString.empty))
        } map {case(method,headers,data) =>
          HttpResponse(200, entity = data.toString())
        }
      }


/*
      connection handleWith {
        Flow[HttpRequest] map{
          case _: HttpRequest =>
            HttpResponse(404, entity = "Unknown resource!")
        }
      }
      */
    }).run()

  }


}
