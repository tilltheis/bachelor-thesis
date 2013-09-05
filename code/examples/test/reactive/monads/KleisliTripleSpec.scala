package examples.reactive.monads

import org.specs2.mutable._

class KleisliTripleSpec extends Specification {
  val ev: KleisliTriple[Option] = KleisliTriple.OptionKleisliTriple

  "unit" should {
    "create a new object of that monad" in {
      ev.unit(1) === Some(1)
    }
  }

  "map on Some" should {
    "apply a function to the inner value" in {
      ev.map((x: Int) => x + 1, Some(1)) === Some(2)
    }
  }

  "bind on Some" should {
    "apply a function to the inner value and return its result" in {
      ev.bind((x: Int) => Some(x + 1))(Some(1)) === Some(2)
    }
  }


  "map on None" should {
    "do nothing" in {
      ev.map((x: Int) => x + 1, None) == None
    }
  }

  "bind on None" should {
    "do nothing" in {
      ev.bind((x: Int) => Some(x + 1))(None) === None
    }
  }
}