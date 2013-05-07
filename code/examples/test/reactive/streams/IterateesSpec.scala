import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator.enumInput

import examples.reactive.streams.Iteratees

class IterateesSpec extends Specification with NoTimeConversions {
  Seq(
    ("inheritance", Iteratees.Creation.sumIterateeFromInheritance),
    ("constructor", Iteratees.Creation.sumIterateeFromConstructor),
    ("helper",      Iteratees.Creation.sumIterateeFromHelper)
  ).foreach { pair =>
    val (kind, iteratee) = pair

    kind + " sum iteratees" should {
      "have a sum of zero after creation" in {
        Await.result(iteratee.run, 1 second) === 0
      }

      "sum correctly" in {
        val e = Enumerator(3, 4) >>>
                enumInput(Input.Empty) >>>
                Enumerator(-2) >>>
                enumInput(Input.Empty)
        val summed = Iteratee.flatten(e(iteratee))
        Await.result(summed.run, 1 second) === 5
      }

      "sum correctly with folder" in {
        val result = iteratee.fold(Iteratees.Creation.folder(List(1, 4, -2)))
        Await.result(result, 1 second) === 3
      }
    }
  }

  "sum result with folder" should {
    "be correct" in {
      Await.result(Iteratees.Creation.sumResult, 1 second) === 3
    }
  }
}