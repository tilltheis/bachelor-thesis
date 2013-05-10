package examples.reactive.streams

import play.api.libs.iteratee._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.language.reflectiveCalls

object Composition {
  object Iteratees {
    object Sequential {

      val i1 = Iteratee.head[String]
      val i2 = Iteratee.consume[String]()

      val i12 = for {
        res1 <- i1
        res2 <- i2
      } yield (res1, res2)

      val e = Enumerator("foo", "bar", "baz")
      val result = e.run(i12)

    }

    object Parallel {

      val i1 = Iteratee.head[String]
      val i2 = Iteratee.consume[String]()
      val i12 = Enumeratee.zip(i1, i2)

      val e = Enumerator("foo", "bar", "baz")
      val result = e.run(i12)

    }

    // object Free {
    //   val i1 = Iteratee.consume[String]()
    //   val i2 = Iteratee.consume[String]()

    //   def composeAlternating[E, A1, A2](odd: Iteratee[E, A1], even: Iteratee[E, A2]): Iteratee[E, (A1, A2)] = {

    //   }

    // }
  }

  object Enumerators {
    object Sequential {

      val i = Iteratee.consume[String]()
      val e = Enumerator("foo").andThen(Enumerator("bar", "baz"))
      val result = e.run(i)

    }

    object Parallel {

      val i = Iteratee.consume[String]()
      val e = Enumerator.interleave(Enumerator("foo"), Enumerator("bar", "baz"))
      val result = e.run(i)

    }
  }
}