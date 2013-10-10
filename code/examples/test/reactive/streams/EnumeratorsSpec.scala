package examples.reactive.streams

import play.api.test._
import play.api.libs.iteratee.Iteratee

class EnumeratorsSpec extends PlaySpecification {
  Seq(
    ("inheritance", Enumerators.Creation.numberEnumeratorFromInheritance),
    ("apply",       Enumerators.Creation.numberEnumeratorFromApply)
  ).foreach { pair =>
    val (kind, enumerator) = pair

    kind + " enumerators" should {
      "enumerate correctly" in {
        val iteratee = Enumerators.sumIteratee
        val sum = await(enumerator.run(iteratee))
        sum === 101
      }
    }
  }

  "broadcast enumerators" should {
    import Enumerators.Creation._

    "enumerate correctly" in {
      val enumerator = numberEnumeratorFromBroadcast
      val iteratee = Iteratee.flatten(enumerator(Enumerators.sumIteratee))
      broadcastChannel.push(22)
      broadcastChannel.push(25)
      broadcastChannel.push(54)
      broadcastChannel.end()
      await(iteratee.run) === 101
    }
  }


  "applying an enumerator to an iteratee" should {
    import Enumerators.Application._

    "yield a correct result for separate application and result extraction" in {
      val sum = await(futureResult)
      sum === 101
    }

    "yield a correct result for combined application and result extraction" in {
      val sum = await(futureResult2)
      sum === 101
    }
  }

}