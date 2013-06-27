import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator.enumInput

import examples.reactive.streams.Composition

import test.Helpers.await

class CompositionSpec extends Specification with NoTimeConversions {

  "sequential composition for iteratees" should {
    "run the first iteratee and then second iteratee" in {
      val result = Composition.Iteratees.Sequential.result
      await(result) === (Some(1), 2)
    }
  }

  "parallel composition for iteratees" should {
    "run the first iteratee together with the second iteratee" in {
      val result = Composition.Iteratees.Parallel.result
      await(result) === (Some(1), 3)
    }
  }

  "selective parallel composition for iteratees" should {
    "pass filtered elements to each iteratee" in {
      val i = Composition.Iteratees.ParallelOneSourceToSeveralSinks.splittingIteratee
      val (evenI, oddI) = await(Enumerator(1,2,3,7,6,5,7,3).run(i))
      val result = (await(evenI.run), await(oddI.run))
      result === (List(2, 6), List(1, 3, 7, 5, 7, 3))
    }
  }

  "parallel composition with several sources to one sink for iteratees" should {
    "yield a correct result" in {
      val i1: Iteratee[Int, Iteratee[String, (Int, String)]] =
        Composition.Iteratees.ParallelManySourcesToOneSink.iteratee

      val i2: Iteratee[String, (Int, String)] =
        Iteratee.flatten(Enumerator(1, 2, 3).run(i1))

      val result = Enumerator("foo", "bar", "baz").run(i2)

      await(result) === (1, "foo")
    }
  }

  "iteratee usage" should {
    "work" in {
      val i = Composition.Iteratees.OneSourceToOneSink.dropFirstIteratee
      val result = Enumerator(3, 1, 2, 3, 4, 5).run(i)

      await(result) === List(4, 5)
    }
  }


  "sequential composition for enumerators" should {
    "run the first enumerator and then the second enumerator" in {
      val result = Composition.Enumerators.Sequential.result
      await(result) === List(1, 2, 3)
    }
  }

  "parallel composition for enumerators" should {
    "run the first enumerator together with the second enumerator" in {
      val result = Composition.Enumerators.Parallel.result
      await(result) === List(2, 3, 1)
    }
  }
}