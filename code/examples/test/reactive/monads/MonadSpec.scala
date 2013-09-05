package examples.reactive.monads

import org.specs2.mutable._

class MonadSpec extends Specification {
  val ev: Monad[Option] = Monad.OptionMonad

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

  "join on Some" should {
    "return the inner Some for Some[Some[A]]" in {
      ev.join(Some(Some(1))) === Some(1)
    }

    "return the inner None for Some[None]" in {
      ev.join(Some(None)) === None
    }

    "return the outer None for None" in {
      ev.join(None) === None
    }
  }


  "map on None" should {
    "do nothing" in {
      ev.map((x: Int) => x + 1, None) == None
    }
  }

  "join on None" should {
    "do nothing" in {
      ev.join(None) === None
    }
  }
}