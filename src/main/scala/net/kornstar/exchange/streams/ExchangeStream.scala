package net.kornstar.exchange.streams

import java.util.concurrent.TimeUnit

import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{TextMessage, UpgradeToWebsocket, Message}
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorSubscriberMessage
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.scaladsl.{ Flow, Sink }
import akka.util.ByteString
import net.kornstar.exchange.collection.{Account, OrderBook, Order}
import net.kornstar.exchange.streams.OrderBookActor.Message._
import play.api.libs.json.{JsString, JsObject, Json}
//import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.LoggerFactory
import scala.concurrent.Future
import net.kornstar.exchange.streams.messages._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 * Created by Ben Kornmeier on 5/4/2015.
 */
object ExchangeStream {
  implicit val timeout = Timeout(5L,TimeUnit.SECONDS)

  val notFound = HttpResponse(400, entity = HttpEntity(MediaTypes.`application/javascript`,Json.toJson(JsonError("not found")).toString()))

  val logger = LoggerFactory.getLogger("ExchangeStream")

  def apply()(implicit system:ActorSystem,materializer: ActorFlowMaterializer) = {
    implicit val exeCtx = system.dispatcher
    val orderBookActor = system.actorOf(Props[OrderBookActor])
    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http(system).bind(interface = "127.0.0.1", port = 8081)

    val bindingFuture: Future[Http.ServerBinding] =serverSource.to(Sink.foreach { connection =>
      logger.info("Accepted new connection from " + connection.remoteAddress)
      connection handleWith {
        Flow[HttpRequest].mapAsync(3) {

          case e@HttpRequest(POST, Uri.Path("/bank"), headers, rEntity:RequestEntity, _) =>
            rEntity.dataBytes.runFold(ByteString.empty){
              case (acc,b) =>
                acc ++ b
            }.map{b =>
              val j = Json.parse(b.toArray).as[Deposit]//b.toArray
              (headers.seq,j)
            }.flatMap{
              case (headers,data) =>
                orderBookActor.ask(ActorSubscriberMessage.OnNext(PlaceDeposit(data))).mapTo[Unit]
            }.map{o =>
              HttpResponse(200,entity = HttpEntity(MediaTypes.`application/javascript`,""))
            }
          case e@HttpRequest(GET, Uri.Path("/bank") , headers, rEntity:RequestEntity, _) =>
            e.uri.query.get("userId").map{userId =>
              orderBookActor.ask(ActorSubscriberMessage.OnNext(GetAccount(userId.toInt))).mapTo[Option[Account]].map{
                case Some(acct) =>
                  val acctJson = Json.toJson(acct)
                  HttpResponse(200,entity = HttpEntity(MediaTypes.`application/javascript`,acctJson.toString()))
                case None =>
                  val acctJson = Json.toJson(JsonError("Account not found"))
                  HttpResponse(404,entity = HttpEntity(MediaTypes.`application/javascript`,acctJson.toString()))
              }
            } getOrElse(Future(notFound))

          case e@HttpRequest(POST, Uri.Path("/order"), headers, rEntity:RequestEntity, _) =>
            logger.info(s"Req: ${e}")
            rEntity.dataBytes.runFold(ByteString.empty){
              case (acc,b) =>
                acc ++ b
            }.map{b =>
              val j = Json.parse(b.toArray).as[Order]//b.toArray
              (headers.seq,j)
            }.flatMap{
              case (headers,data) =>
                orderBookActor.ask(ActorSubscriberMessage.OnNext(PlaceOrder(data))).mapTo[Order]
            }.map{o =>
              val jsonString = Json.toJson(o).toString()
              HttpResponse(200,entity = HttpEntity(MediaTypes.`application/javascript`,jsonString))
            }
          case e@HttpRequest(DELETE, u@Uri.Path("/order"), headers, rEntity:RequestEntity, _) =>
            val id = u.query.get("id").get
            logger.debug(s"delete ID: ${id}")
            orderBookActor.ask(ActorSubscriberMessage.OnNext(CancelOrder(id.toInt))).mapTo[Option[Order]].map{
              case Some(o) =>
                val jsonString = Json.toJson(o).toString()
                HttpResponse(200,entity = HttpEntity(MediaTypes.`application/javascript`,jsonString))
              case None =>
                notFound
            }
          case e@HttpRequest(GET, u@Uri.Path("/order"), headers, rEntity:RequestEntity, _) =>
            u.query.get("userId") map {userId =>
              logger.debug(s"Getting order for userID: ${userId}")
              orderBookActor.ask(ActorSubscriberMessage.OnNext(GetOrders(userId.toInt))).mapTo[Iterable[Order]].map {order =>
                val j = Json.toJson(order).toString()
                HttpResponse(200,entity = HttpEntity(MediaTypes.`application/javascript`,j))
              }
            } getOrElse(Future(notFound))
          case e@HttpRequest(GET, u@Uri.Path("/market"), headers, rEntity:RequestEntity, _) =>
            orderBookActor.ask(ActorSubscriberMessage.OnNext(GetMarket)).mapTo[OrderBookStats].map {obs =>
              val jsonString = Json.toJson(obs).toString()
              HttpResponse(200, entity = HttpEntity(MediaTypes.`application/javascript`,jsonString))
            }
          case e: HttpRequest =>
            Future(notFound)
        }
      }
    }).run()

  }


}
