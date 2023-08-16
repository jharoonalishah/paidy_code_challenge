package forex

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import forex.http.RequestRates
import forex.model.Currency

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

object RouteLogic {

  def route(httpProcessors: ActorRef, log: LoggingAdapter)(implicit system: ActorSystem): Route =
    isAlive ~ rates(httpProcessors, log)(system, system.dispatcher)

  def isAlive: Route =
    path("isAlive"){
      get {
        complete("It's ALIVE!!!")
      }
    }

  def rates(httpProcessors: ActorRef, log: LoggingAdapter)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext): Route =
    path("rates") {
      get {
        extractRequest { request =>
          implicit val timeout:Timeout = Timeout(5, TimeUnit.SECONDS)
          val map = request.uri.query().toMap

          val from = map.get("from").toRight("From not specified").flatMap(Currency.fromString)
          val to = map.get("to").toRight("To not specified").flatMap(Currency.fromString)

          val response = from.flatMap(fr => to.map(t => (fr, t)))
            .map(currecyTuple => {
              val from = currecyTuple._1
              val to = currecyTuple._2

              httpProcessors ? RequestRates(from, to) map {
                case response: HttpResponse =>
                  log.info(s"Received http response $response")
                  response
                case x => HttpResponse(StatusCodes.InternalServerError, entity = s"Received unknown response $x")
              } recover(throwable => HttpResponse(StatusCodes.InternalServerError, entity = throwable.getMessage))
            }).fold(errorMessage => Future.successful(HttpResponse(StatusCodes.BadRequest, entity = errorMessage)),
            result => result)

          complete(response)
        }
      }
    }
}
