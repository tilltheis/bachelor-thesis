package examples.reactive.streams

import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise

import scala.concurrent.{Promise => _, _}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.language.reflectiveCalls
import scala.language.postfixOps

object Composition {
  object Iteratees {
    object Sequential {

      val i1: Iteratee[Int, Option[Int]] = Iteratee.head
      val i2: Iteratee[Int, Int] = Iteratee.fold(0)(_ + _)

      // val i12: Iteratee[Int, (Option[Int], Int)] = for {
      //   res1 <- i1
      //   res2 <- i2
      // } yield (res1, res2)

      val i12: Iteratee[Int, (Option[Int], Int)] =
        i1.flatMap(res1 => i2.map(res2 => (res1, res2)))

      val e: Enumerator[Int] = Enumerator(1, 4, -2)
      val result: Future[(Option[Int], Int)] = e.run(i12)
      // result = Future.successful((Some(1), 2))

    }

    object Parallel {

      val i1: Iteratee[Int, Option[Int]] = Iteratee.head
      val i2: Iteratee[Int, Int] = Iteratee.fold(0)(_ + _)

      val i12: Iteratee[Int, (Option[Int], Int)] =
        Enumeratee.zip(i1, i2)

      val e: Enumerator[Int] = Enumerator(1, 4, -2)
      val result: Future[(Option[Int], Int)] = e.run(i12)
      // result = Future.successful((Some(1), 3))

    }

    object OneSourceToOneSink {
      // val dropFirstIter: Iteratee[Int, List[Int]] = for {
      //   firstOption: Option[Int] <- Iteratee.head
      //   xs: List[Int] <- Enumeratee.drop(firstOption.get).transform(Iteratee.getChunks)
      // } yield xs

      val dropFirstIteratee: Iteratee[Int, List[Int]] =
        Iteratee.head.flatMap { firstOption =>
          Enumeratee.drop(firstOption.get).transform(Iteratee.getChunks)
        }
    }

    object ParallelOneSourceToSeveralSinks {
      val evenIteratee: Iteratee[Int, List[Int]] = Iteratee.getChunks
      val oddIteratee: Iteratee[Int, List[Int]] = Iteratee.getChunks
      val splittingIteratee: Iteratee[Int, (Iteratee[Int, List[Int]], Iteratee[Int, List[Int]])] =
        Iteratee.fold((evenIteratee, oddIteratee)) { case ((evenI, oddI), n) =>
          def feed(i: Iteratee[Int, List[Int]]) = Iteratee.flatten(i.feed(Input.El(n)))
          if (n % 2 == 0) (feed(evenI), oddI) else (evenI, feed(oddI))
        }
    }

    object ParallelManySourcesToOneSink {
      type Word = String
      type Line = String

      def lineWithNthWord(n: Int): Iteratee[Word, Iteratee[Line, Option[(Line, Word)]]] = {
        Enumeratee.drop(n).transform(Iteratee.head[Word]).map {
          case Some(word) =>
            val t = Enumeratee.dropWhile[Line](!_.startsWith(word))
            t.transform(Iteratee.head[Line]).map {
              case Some(line) => Some(line, word)
              case _ => None
            }
          case _ => Done(None)
        }
      }
    }
  }

  object Enumerators {
    object Sequential {

      val i: Iteratee[Int, List[Int]] = Iteratee.getChunks
      val e1 = Enumerator(1, 2)
      val e2 = Enumerator(3)
      val e12: Enumerator[Int] = e1.andThen(e2)

      val result: Future[List[Int]] = e12.run(i)
      // result = Future.successful(List(1, 2, 3))

    }

    object Parallel {


      def timeoutEnumerator[A](x: A, d: Duration): Enumerator[A] =
        Enumerator.flatten(Promise.timeout(Enumerator(x), d))

      val i: Iteratee[Int, List[Int]] = Iteratee.getChunks
      val e1 = timeoutEnumerator(1, 3 seconds)
      val e2 = timeoutEnumerator(2, 1 second)
      val e3 = timeoutEnumerator(3, 2 seconds)
      val e123: Enumerator[Int] = Enumerator.interleave(e1, e2, e3)

      val result: Future[List[Int]] = e123.run(i)
      // result = Future.successful(List(2, 3, 1))

    }
  }
}