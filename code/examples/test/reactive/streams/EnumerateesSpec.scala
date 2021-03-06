import scala.concurrent._
import ExecutionContext.Implicits.global

import play.api.libs.iteratee._

import play.api.test._

import examples.reactive.streams.Enumeratees

class EnumerateesSpec extends PlaySpecification {
  Seq(
      ("inheritance", Enumeratees.Creation.Rejuvinating.enumerateeFromInheritance),
      ("constructor", Enumeratees.Creation.Rejuvinating.enumerateeFromConstructor)
    ).foreach { pair =>
      val (kind, enumeratee) = pair

    "creating a rejuvinating enumeratee from " + kind should {
      val enumerator = Enumerator(22, 25, 54)
      // val enumeratee = enumerateeFromInheritance

      "yield a correct result" in {
        val iteratee: Iteratee[Int, Int] = Iteratee.fold(0)(_ + _)

        val transformedIteratee = enumeratee.transform(iteratee)
        await(enumerator.run(transformedIteratee)) === 91
      }

      "work with prematurely done iteratees" in {
        val iteratee = Cont[Int, Int] {
          case Input.El(n) => Done(n, Input.Empty)
          case in => Error("not an element", in)
        }

        val transformedIteratee = enumeratee.transform(iteratee)
        await(enumerator.run(transformedIteratee)) === 22

        val transformedIteratee2 = enumeratee.transform(iteratee)
        await(Enumerator.enumInput(Input.Empty).run(transformedIteratee2)) must throwAn[Exception]
      }
    }
  }


  "applying an enumeratee to an iteratee" should {
    "get back the original iteratee afterwards" in {
      val result = Enumeratees.ApplicationOnIteratees.result
      await(result) === List(22, 44, 22, 54)
    }
  }

  "applying an enumeratee to an enumerator" should {
    "transform the enumerator" in {
      val e = Enumeratees.ApplicationOnEnumerators.transformedE
      val i = Iteratee.getChunks[String]
      val result = e.run(i)
      await(result) === List("1", "2", "3")
    }
  }

  "applying an enumeratee to an enumeratee" should {
    "chain the enumeratees together" in {
      await(Enumeratees.ApplicationOnEnumeratees.result) === List(22, 25, 44)
    }
  }
}