import play.api.test._

import examples.reactive.streams.Enumerators

class EnumeratorsSpec extends PlaySpecification {
  Seq(
    ("inheritance", Enumerators.Creation.numberEnumeratorFromInheritance),
    ("apply",       Enumerators.Creation.numberEnumeratorFromApply),
    ("unicast",     Enumerators.Creation.numberEnumeratorFromUnicast)
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