import org.specs2.mutable._

import scala.concurrent._
import scala.concurrent.duration._

import play.api.libs.iteratee._

import examples.reactive.streams.Iteratees

class IterateesSpec extends Specification {
  // "The logger iteratee" should {
  //   "do side effects" in {
  //     val flattened = Iteratee.flatten(Iteratees.appliedLoggerIteratee)
  //     println(flattened)
  //     Await.result(flattened.run, Duration(1, "second")) === ()
  //   }
  // }

  Seq(
    ("inheritance", Iteratees.Creation.sumIterateeFromInheritance),
    ("constructor", Iteratees.Creation.sumIterateeFromConstructor),
    ("helper",      Iteratees.Creation.sumIterateeFromHelper)
  ).foreach { pair =>
    val (kind, iteratee) = pair

    kind + " sum iteratees" should {
      "have a sum of zero after creation" in {
        Await.result(iteratee.run, Duration(1, "second")) === 0
      }

      "sum correctly" in {
        val e = Enumerator(3, 4, -2)
        val summed = Iteratee.flatten(e(iteratee))
        Await.result(summed.run, Duration(1, "second")) === 5
      }
    }
  }
}