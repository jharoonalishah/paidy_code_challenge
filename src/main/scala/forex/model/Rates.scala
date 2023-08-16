package forex.model

import forex.model.Currency.Currency
import play.api.libs.json.{Json, OFormat}

import scala.collection.mutable

class Rates private(values: Map[Currency, Map[Currency, Rate]]) {

  def get(from:Currency, to: Currency): Option[Rate] =
    if (from == to) Some(Rates.selfRate)
    else values.get(from).flatMap(_.get(to))

}

object Rates {

  def apply(values: List[RatesUpdatePair]): Rates = {
    val mapValues = values.foldLeft(new mutable.HashMap[Currency, mutable.HashMap[Currency, Rate]]())((acc, entry) => {
      val rate = Rate(entry.bid, entry.ask, entry.price)
      val reverseRate = oppositeRate(rate)

      insertValue(acc, rate, entry.from, entry.to)
      insertValue(acc, reverseRate, entry.to, entry.from)

      acc
    }).map(entry => (entry._1, entry._2.toMap)).toMap

    new Rates(mapValues)
  }

  private def insertValue(map: mutable.HashMap[Currency, mutable.HashMap[Currency, Rate]],
                          rate: Rate, from: Currency, to: Currency): mutable.HashMap[Currency, mutable.HashMap[Currency, Rate]] = {
    val innerMap = map.getOrElse(from, mutable.HashMap())
    innerMap.put(to, rate)

    map.put(from, innerMap)
    map
  }

  private val selfRate = Rate(1, 1, 1)

  private val one:BigDecimal = BigDecimal(1)

  private def oppositeRate(rate: Rate) =
    Rate(opposite(rate.bid), opposite(rate.bid), opposite(rate.price))


  private def opposite(bigDecimal: BigDecimal): BigDecimal = {
    (one / bigDecimal).setScale(2, BigDecimal.RoundingMode.HALF_EVEN)
  }


}

case class Rate(bid: BigDecimal,
                ask: BigDecimal,
                price: BigDecimal)

object Rate {

  implicit val format: OFormat[Rate] = Json.format[Rate]
}