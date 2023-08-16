package forex.model

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object Currency extends Enumeration {
  type Currency = Value

  val AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD = Value

  implicit val currFormat: Format[Currency] = new Format[Currency] {
    override def reads(json: JsValue): JsResult[Currency] = {
      fromString(json.as[String])

      Try {
        Currency.withName(json.as[String])
      } match {
        case Failure(exception) => JsError(s"Failed to parse Currency ${exception.getMessage}")
        case Success(value) => JsSuccess(value)
      }
    }

    override def writes(o: Currency): JsValue = JsString(o.toString)
  }

  def fromString(s:String): Either[String, Currency] = {
    Try {
      Currency.withName(s)
    } match {
      case Failure(exception) => Left(exception.getMessage)
      case Success(value) => Right(value)
    }
  }

  def getAllCurrencyPairs:List[(String, String)] =
    getCurrencyCombinations(List(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD), List.empty)
      .map(tuple => (tuple._1.toString, tuple._2.toString))

  @scala.annotation.tailrec
  private def getCurrencyCombinations(currencyList: List[Currency],
                                      acc: List[(Currency, Currency)]): List[(Currency, Currency)] = {
    if (currencyList.size > 1) {
      val head = currencyList.head
      val pairs = currencyList.tail.map(cur => (head, cur))

      getCurrencyCombinations(currencyList.tail, pairs ::: acc)
    } else acc
  }
}
