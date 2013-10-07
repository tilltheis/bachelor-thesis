package examples.reactive.futures

import play.api.test._

class ExamplesSpec extends PlaySpecification {
  "combination example" should {
    "be correct" in {
      await(Examples.CombinationExample.f1f2) === (35, 40)
    }
  }

  "promise example" should {
    "be correct" in {
      val future = Examples.PromiseExample.future
      Thread.sleep(1000) // future is redeemed after 2 seconds
      future.value === None
      await(Examples.PromiseExample.future) === 42
    }
  }
}
