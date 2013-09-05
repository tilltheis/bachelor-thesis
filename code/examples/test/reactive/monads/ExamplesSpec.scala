package examples.reactive.monads

import org.specs2.mutable._

class ExamplesSpec extends Specification {
  "option example" should {
    "be correct" in {
      Examples.OptionExample.twentyAndThirtyYearOldCount === Some(21)
    }
  }

  "for-comprehension example" should {
    "be correct" in {
      Examples.ForComprehensionExample.twentyAndThirtyYearOldCount === Some(21)
    }
  }
}