import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.util.{Try, Failure}

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator.enumInput

import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers // for await

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
        Helpers.await(iteratee.run) === 0
      }

      "sum correctly" in {
        val e = Enumerator(3, 4) >>>
                enumInput(Input.Empty) >>>
                Enumerator(-2) >>>
                enumInput(Input.Empty)
        val summed = Iteratee.flatten(e(iteratee))
        Helpers.await(summed.run) === 5
      }

      "sum correctly with folder" in {
        val result = iteratee.fold(Iteratees.Creation.folder(1, 4, -2))
        Helpers.await(result) === 3
      }
    }
  }

  "sum result with folder" should {
    Seq(
      ("Done", Done(1): Iteratee[Int, Int]),
      ("Error", Error("error", Input.Empty): Iteratee[Int, Int]),
      ("Cont then Cont", Cont(_ => Cont(_ => Done(1))): Iteratee[Int, Int]),
      ("Cont then Error", Cont(_ => Error("error", Input.Empty)): Iteratee[Int, Int])
    ).foreach { pair =>
      val (kind, iteratee) = pair

      s"fail for $kind iteratees" in {
        Helpers.await(iteratee.fold(Iteratees.Creation.folder(1))) must throwA[Exception].like {
          case e => e.getMessage === "invalid state"
        }

        Helpers.await(iteratee.fold(Iteratees.Creation.folder())) must throwA[Exception].like {
          case e => e.getMessage === "invalid state"
        }
      }
    }

    // must be last because block must return org.specs2.specification.Fragment
    "be correct" in {
      Helpers.await(Iteratees.Creation.sumResult) === 101
    }
  }
}