import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Try, Failure}

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
        val result = iteratee.fold(Iteratees.Creation.folder(1, 4, -2))
        Await.result(result, 1 second) === 3
      }
    }
  }

  "sum result with folder" should {
    "be correct" in {
      Await.result(Iteratees.Creation.sumResult, 1 second) === 3
    }

    Seq(
      ("Done", Done(1): Iteratee[Int, Int]),
      ("Error", Error("error", Input.Empty): Iteratee[Int, Int]),
      ("Cont then Cont", Cont(_ => Cont(_ => Done(1))): Iteratee[Int, Int]),
      ("Cont then Error", Cont(_ => Error("error", Input.Empty)): Iteratee[Int, Int])
    ).foreach { pair =>
      val (kind, iteratee) = pair

      s"fail for $kind iteratees" in {
        (iteratee.fold(Iteratees.Creation.folder(1)).value) must beLike {
          case Some(Failure(_)) => ok
        }

        iteratee.fold(Iteratees.Creation.folder()).value must beLike {
          case Some(Failure(_)) => ok
        }
      }
    }
  }
}