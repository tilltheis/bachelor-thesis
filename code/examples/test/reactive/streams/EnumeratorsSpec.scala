import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator.enumInput

import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers // for await

import examples.reactive.streams.Enumerators

class EnumeratorsSpec extends Specification with NoTimeConversions {
  Seq(
    ("inheritance", Enumerators.Creation.numberEnumeratorFromInheritance),
    ("apply",       Enumerators.Creation.numberEnumeratorFromApply),
    ("unicast",     Enumerators.Creation.numberEnumeratorFromUnicast)
  ).foreach { pair =>
    val (kind, enumerator) = pair

    kind + " enumerators" should {
      "enumerate correctly" in {
        val iteratee = Enumerators.sumIteratee
        val sum = Await.result(enumerator.run(iteratee), 1 second)
        sum === 101
      }
    }
  }


  "applying an enumerator to an iteratee" should {
    import Enumerators.Application._

    "yield a correct result for separate application and result extraction" in {
      val sum = Helpers.await(futureResult)
      sum === 101
    }

    "yield a correct result for combined application and result extraction" in {
      val sum = Helpers.await(futureResult2)
      sum === 101
    }
  }

}