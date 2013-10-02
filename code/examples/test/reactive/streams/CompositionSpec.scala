import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator.enumInput

import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.{Helpers, WithApplication} // for await

import examples.reactive.streams.Composition

class CompositionSpec extends Specification with NoTimeConversions {

  "sequential composition for iteratees" should {
    "run the first iteratee and then second iteratee" in {
      val result = Composition.Iteratees.Sequential.result
      // (await(result)) === (Some(1), 2)
      Helpers.await(result) === (Some(1), 2)
      // (Some(1), 2) === (Some(1), 2)
    }
  }

  "parallel composition with one source to may sinks for iteratees" should {
    "run the first iteratee together with the second iteratee" in {
      val result = Composition.Iteratees.ParallelOneSourceToManySinks.result
      Helpers.await(result) === (Some(1), 3)
    }
  }

  "parallel composition with several sources to one sink for iteratees" should {
      "work for headheadIteratee" in {
        val result = Composition.Iteratees.ParallelManySourcesToOneSink.result
        Helpers.await(result) === (Some('1'), None)
      }
  }


  "sequential composition for enumerators" should {
    "run the first enumerator and then the second enumerator" in {
      val result = Composition.Enumerators.Sequential.result
      Helpers.await(result) === List(1, 2, 3)
    }
  }

  "parallel composition for enumerators" should {
    "run the first enumerator together with the second enumerator" in new WithApplication {
      // play.api.concurrent.Promise requires a running application
      val result = Composition.Enumerators.Parallel.result
      Helpers.await(result) === List(2, 3, 1)
    }
  }

}