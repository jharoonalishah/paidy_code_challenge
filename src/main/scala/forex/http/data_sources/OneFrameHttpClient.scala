package forex.http.data_sources

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest, Uri}
import akka.util.ByteString
import com.typesafe.config.Config
import forex.model.{Currency, Rates, RatesUpdatePair}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

trait OneFrameHttpClient {

  def requestOneFrameUpdates(implicit actorSystem: ActorSystem, executionContext: ExecutionContext): Future[Rates]
}

class OneFrameHttpClientImpl(config: Config) extends OneFrameHttpClient {

  override def requestOneFrameUpdates(implicit actorSystem: ActorSystem, executionContext: ExecutionContext): Future[Rates] = {
    Http().singleRequest(httpRequest)
      .flatMap(httpResponse => {
        httpResponse.entity.dataBytes
          .runFold(ByteString.newBuilder)((builder, bs) => builder.append(bs))
          .map(_.result())
          .map(decodeResponse)
          .map(Rates(_))
      })
  }

  private def httpRequest:HttpRequest =
    HttpRequest(
      HttpMethods.GET,
      oneFrameUri,
      headers = Seq(getTokenHeander)
    )

  private lazy val oneFrameUri:Uri =
    Uri(scheme = "http",
      authority = authority,
      path = Uri.Path(config.getString("rates-endpoint"))
    ).withQuery(queryString)

  private lazy val authority =
    Uri.Authority(
      host = Uri.Host(config.getString("host")),
      port = config.getInt("port")
    )

  private lazy val queryString =
    Currency.getAllCurrencyPairs
      .map(pair => ("pair", pair._1 + pair._2))
      .foldLeft(Query.newBuilder)((builder, pair) => builder.addOne(pair))
      .result()

  private lazy val getTokenHeander: HttpHeader =
    RawHeader("token", config.getString("rates-header-token"))

  private def decodeResponse: ByteString => List[RatesUpdatePair] =
    bs => Json.parse(bs.decodeString(StandardCharsets.UTF_8))
      .as[List[RatesUpdatePair]]
}
