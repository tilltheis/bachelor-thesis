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
      NumberEnumerator(1, 4, -2)

    val numberEnumeratorFromApply: Enumerator[Int] =
      Enumerator(1, 4, -2)

    def numberEnumerator(xs: Int*): Enumerator[Int] = {
      Concurrent.unicast { channel =>
        xs.foreach(channel.push(_))
        channel.end
      }
    }

    val numberEnumeratorFromUnicast: Enumerator[Int] =
      numberEnumerator(1, 4, -2)
  }

  object Application {
    val iteratee: Iteratee[Int, Int] = Iteratee.fold(0)(_ + _)
    val enumerator: Enumerator[Int] = Enumerator(1, 4, -2)
    val futureIterateeAfterApplication: Future[Iteratee[Int, Int]] =
      enumerator(iteratee)

    val iterateeAfterApplication: Iteratee[Int, Int] =
      Iteratee.flatten(futureIterateeAfterApplication)

    val futureResult: Future[Int] = iterateeAfterApplication.run

    val futureResult2: Future[Int] = enumerator.run(iteratee)
  }

  // object NestedApplication {
  //   val sumIterateeAfterE1: Future[Iteratee[Int, Int]] =
  //     e1(sumIteratee)

  //   val sumIterateeAfterE2: Future[Future[Iteratee[Int, Int]]] =
  //     sumIterateeAfterE1.map(e2(_))

  //   val sumResult: Future[Future[Future[Int]]] =
  //     sumIterateeAfterE2.map(_.map(_.run))
  // }

  // object FlattenedApplication {
  //   val sumIterateeAfterE1: Iteratee[Int, Int] =
  //     Iteratee.flatten(e1(sumIteratee))

  //   val sumIterateeAfterE2: Iteratee[Int, Int] =
  //     Iteratee.flatten(e2(sumIterateeAfterE1))

  //   val sumResult: Future[Int] = sumIterateeAfterE2.run
  // }

  // object CombinedApplication {
  //   val e1e2: Enumerator[Int] = e1.andThen(e2)

  //   val sumIterateeAfterE1E2: Iteratee[Int, Int] =
  //     Iteratee.flatten(e1e2(sumIteratee))

  //   val sumResult: Future[Int] = sumIterateeAfterE1E2.run
  // }

  // object ApplicationWithRun {
  //   val e1e2: Enumerator[Int] = e1.andThen(e2)
  //   val sumResult: Future[Int] = e1e2.run(sumIteratee)
  // }
}