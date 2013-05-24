import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator.enumInput

import examples.reactive.streams.Composition

class CompositionSpec extends Specification with NoTimeConversions {

  "sequential composition for iteratees" should {
    "run the first iteratee and then second iteratee" in {
      val result = Composition.Iteratees.Sequential.result
      Await.result(result, 1 second) ===((Some("foo"), "barbaz"))
    }
  }

  "parallel composition for iteratees" should {
    "run the first iteratee together with the second iteratee" in {
      val result = Composition.Iteratees.Parallel.result
      Await.result(result, 1 second) ===((Some("foo"), "foobarbaz"))
    }
  }


  "sequential composition for enumerators" should {
    "run the first enumerator and then the second enumerator" in {
      val result = Composition.Enumerators.Sequential.result
      Await.result(result, 1 second) === "foobarbaz"
    }
  }

  "parallel composition for enumerators" should {
    "run the first enumerator together with the second enumerator" in {
      val result = Composition.Enumerators.Sequential.result
      val string = Await.result(result, 1 second)

      Seq("foo", "bar", "baz").foreach { word =>
        string must contain(word)
      }
    }
  }
}