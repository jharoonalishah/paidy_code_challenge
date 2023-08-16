package forex.http

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import forex.model.Currency.Currency
import forex.model.{GetRates, Rates}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration

class HttpProcessActor(ratesActor: ActorRef) extends Actor with ActorLogging {

  private var requestUpdateTimestamp: Long = Long.MinValue
  private var rates: Option[Rates] = None

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  implicit val sendr: ActorRef = self

  override def preStart(): Unit = {
    log.info("Http Actor started")
    context.system.scheduler.scheduleAtFixedRate(FiniteDuration(0, TimeUnit.SECONDS),
      FiniteDuration(3, TimeUnit.MINUTES), ratesActor, GetRates())
  }

  override def receive: Receive = {
    case RatesUpdate(map, lastUpdated) =>
      log.info("Received rate updates")
      requestUpdateTimestamp = lastUpdated
      rates = Some(map)


    case RequestRates(from, to) =>
      log.info("Received rates request")
      val r = rates
      log.info(s"Rates $r")

      val ff = rates.flatMap(_.get(from, to))
      log.info(s"FF $ff")

      val rats = rates.flatMap(_.get(from, to))
        .map(rate => {
          HttpResponse(StatusCodes.OK, entity = HttpEntity(MediaTypes.`application/json`, Json.toJson(rate).toString))
        }).getOrElse(HttpResponse(StatusCodes.NotFound, entity = "Valid rates data not found"))

      sender() ! rats
  }

  override def postStop(): Unit = {
    super.aroundPostStop()
    log.info("Http actor stopped")
  }

}

case class RequestRates(from: Currency, to: Currency)
case class RatesUpdate(map: Rates, lastUpdated: Long)
