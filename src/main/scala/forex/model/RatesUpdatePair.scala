package forex.model

import forex.model.Currency.Currency
import play.api.libs.json.{Json, OFormat}

case class RatesUpdatePair(from: Currency,
                           to: Currency,
                           bid: BigDecimal,
                           ask: BigDecimal,
                           price: BigDecimal,
                           time_stamp: String)

object RatesUpdatePair {

  implicit val format: OFormat[RatesUpdatePair] = Json.format[RatesUpdatePair]
}