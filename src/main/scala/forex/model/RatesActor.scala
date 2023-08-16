package forex.model

import akka.actor.{Actor, ActorLogging, ActorSystem}
import forex.http.RatesUpdate
import forex.http.data_sources.OneFrameHttpClient

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{Duration, FiniteDuration}

class RatesActor(oneFrameHttpClient: OneFrameHttpClient) extends Actor with ActorLogging {

  private var lastUpdated: Long = Long.MinValue
  private var rates: Option[Rates] = None
  implicit val system:ActorSystem = context.system
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher


  override def preStart(): Unit = {
    log.info("Rates actor started")
    context.system.scheduler
      .scheduleAtFixedRate(Duration.Zero, FiniteDuration(3, TimeUnit.MINUTES), self, RequestUpdates())
  }

  override def receive: Receive = {
    case RequestUpdates() =>
      log.info("Received request updates")
      oneFrameHttpClient.requestOneFrameUpdates
        .foreach(rates => self ! UpdateResult(rates))

    case UpdateResult(rates) =>
      this.rates = Some(rates)
      lastUpdated = System.currentTimeMillis()

    case message@GetRates() =>
      log.info("Received get rates")
      if (lastUpdated < 0 || System.currentTimeMillis() - lastUpdated > 5 * 60 * 1000) {
        log.info("Data is too old, resending message to self")
        context.system.scheduler
          .scheduleOnce(FiniteDuration(5, TimeUnit.SECONDS), self, message)(executionContext, sender())
      } else {

        sender() ! RatesUpdate(rates.get, lastUpdated)
      }
    case x =>
      log.error(s"Unknown message received $x")
  }

  override def postStop(): Unit = {
    super.aroundPostStop()
    log.info("Rates actor stopped")
  }
}

case class RequestUpdates()
case class UpdateResult(rates: Rates)
case class GetRates()
