import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator.enumInput

import examples.reactive.streams.Enumerators

import test.Helpers.await

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
        sum === 3
      }
    }
  }


  "applying an enumerator to an iteratee" should {
    import Enumerators.Application._

    "yield a correct result for separate application and result extraction" in {
      val sum = await(futureResult)
      sum === 3
    }

    "yield a correct result for combined application and result extraction" in {
      val sum = await(futureResult2)
      sum === 3
    }
  }



  // "Enumerators with nested application" should {
  //   import Enumerators.NestedApplication._

  //   "enumerate the initial iteratee correctly" in {
  //     val iteratee = Await.result(sumIterateeAfterE1, 1 second)
  //     val sum = Await.result(iteratee.run, 1 second)
  //     sum === 6
  //   }

  //   "enumerate the resulting iteratee correctly" in {
  //     val futureIteratee = Await.result(sumIterateeAfterE2, 1 second)
  //     val iteratee = Await.result(futureIteratee, 1 second)
  //     val sum = Await.result(iteratee.run, 1 second)
  //     sum === 15
  //   }

  //   "lead to a correct result" in {
  //     val d = 1 second
  //     val sum: Int = Await.result(Await.result(Await.result(sumResult, d), d), d)
  //     sum === 15
  //   }
  // }

  // "Enumerators with flattened application" should {
  //   import Enumerators.FlattenedApplication._

  //   "enumerate the initial iteratee correctly" in {
  //     val sum = Await.result(sumIterateeAfterE1.run, 1 second)
  //     sum === 6
  //   }

  //   "enumerate the resulting iteratee correctly" in {
  //     val sum = Await.result(sumIterateeAfterE2.run, 1 second)
  //     sum === 15
  //   }

  //   "lead to a correct result" in {
  //     val sum: Int = Await.result(sumResult, 1 second)
  //     sum === 15
  //   }
  // }

  // "Enumerators with combined application" should {
  //   import Enumerators.CombinedApplication._

  //   "enumerate the initial iteratee correctly" in {
  //     val sum = Await.result(sumIterateeAfterE1E2.run, 1 second)
  //     sum === 15
  //   }

  //   "lead to a correct result" in {
  //     val sum: Int = Await.result(sumResult, 1 second)
  //     sum === 15
  //   }
  // }

  // "Enumerators with combined application" should {
  //   import Enumerators.ApplicationWithRun._

  //   "lead to a correct result" in {
  //     val sum: Int = Await.result(sumResult, 1 second)
  //     sum === 15
  //   }
  // }
}