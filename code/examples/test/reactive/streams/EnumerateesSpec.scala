import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator.enumInput

import examples.reactive.streams.Enumeratees

class EnumerateesSpec extends Specification with NoTimeConversions {
  def await[A](f: Future[A]): A = Await.result(f, 1 second)

  Seq(
      ("inheritance", Enumeratees.Creation.enumerateeFromInheritance),
      ("constructor", Enumeratees.Creation.enumerateeFromConstructor)
    ).foreach { pair =>
      val (kind, enumeratee) = pair

    "creating an enumeratee from " + kind should {
      val enumerator = Enumerator(1, 4, -2)
      // val enumeratee = enumerateeFromInheritance

      "yield a correct result" in {
        val iteratee: Iteratee[Int, Int] = Iteratee.fold(0)(_ + _)

        val transformedIteratee = enumeratee.transform(iteratee)
        await(enumerator.run(transformedIteratee)) === 6
      }

      "work with prematurely done iteratees" in {
        val iteratee = Cont[Int, Int] {
          case Input.El(n) => Done(n, Input.Empty)
          case in => Error("not an element", in)
        }

        val transformedIteratee = enumeratee.transform(iteratee)
        await(enumerator.run(transformedIteratee)) === 2

        val transformedIteratee2 = enumeratee.transform(iteratee)
        await(Enumerator.enumInput(Input.Empty).run(transformedIteratee2)) must throwAn[Exception]
      }
    }
  }
}