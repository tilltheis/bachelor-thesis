package examples.reactive.streams

import play.api.libs.iteratee._
import scala.concurrent._
import ExecutionContext.Implicits.global

object Enumerators {
  val sumIteratee: Iteratee[Int, Int] = Iteratee.fold(0)(_ + _)

  val e1: Enumerator[Int] = Enumerator(1, 2, 3)
  val e2: Enumerator[Int] = Enumerator(4, 5)

  object Creation {
    case class NumberEnumerator(xs: Int*) extends Enumerator[Int] {
      def apply[A](iteratee: Iteratee[Int, A]):
          Future[Iteratee[Int, A]] = {
        xs.foldLeft(Future(iteratee)) { (futureIteratee, x) =>
          futureIteratee.flatMap { iteratee =>
            iteratee.fold {
              case Step.Cont(k) => Future(k(Input.El(x)))
              case _ => Future(iteratee)
            }
          }
        }
      }
    }

    val numberEnumeratorFromInheritance: Enumerator[Int] =
      NumberEnumerator(22, 25, 54)


    val numberEnumeratorFromApply: Enumerator[Int] =
      Enumerator(22, 25, 54)


    val (numberEnumeratorFromBroadcast, broadcastChannel) =
      Concurrent.broadcast[Int]
  }

  object Application {
    val iteratee: Iteratee[Int, Int] = Iteratee.fold(0)(_ + _)
    val enumerator: Enumerator[Int] = Enumerator(22, 25, 54)
    val futureIterateeAfterApplication: Future[Iteratee[Int, Int]] =
      enumerator(iteratee)

    val iterateeAfterApplication: Iteratee[Int, Int] =
      Iteratee.flatten(futureIterateeAfterApplication)

    val futureResult: Future[Int] = iterateeAfterApplication.run
    // futureResult hat den Wert Future(101)

    val futureResult2: Future[Int] = enumerator.run(iteratee)
    // futureResult2 hat den Wert Future(101)
  }

}