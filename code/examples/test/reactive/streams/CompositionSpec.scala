import play.api.test._

import examples.reactive.streams.Composition

class CompositionSpec extends PlaySpecification {

  "sequential composition for iteratees" should {
    "run the first iteratee and then second iteratee" in {
      val result = Composition.Iteratees.Sequential.result
      await(result) === (Some(1), 2)
    }
  }

  "parallel composition with one source to may sinks for iteratees" should {
    "run the first iteratee together with the second iteratee" in {
      val result = Composition.Iteratees.ParallelOneSourceToManySinks.result
      await(result) === (Some(1), 3)
    }
  }

  "parallel composition with several sources to one sink for iteratees" should {
      "work for headheadIteratee" in {
        val result = Composition.Iteratees.ParallelManySourcesToOneSink.result
        await(result) === (Some('1'), None)
      }
  }


  "sequential composition for enumerators" should {
    "run the first enumerator and then the second enumerator" in {
      val result = Composition.Enumerators.Sequential.result
      await(result) === List(1, 2, 3)
    }
  }

  "parallel composition for enumerators" should {
    "run the first enumerator together with the second enumerator" in new WithApplication {
      // play.api.concurrent.Promise requires a running application
      val result = Composition.Enumerators.Parallel.result
      await(result) === List(2, 3, 1)
    }
  }

}