package examples.reactive.streams

import play.api.libs.iteratee._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.language.reflectiveCalls

object Enumeratees {
  object Creation {

    case object MultiplyingEnumeratee extends Enumeratee[Int, Int] {
      def applyOn[A](inner: Iteratee[Int, A]):
          Iteratee[Int, Iteratee[Int, A]] = {
        Iteratee.flatten(inner.fold {
          case Step.Cont(k) => Future(Cont {
            case Input.El(number) =>
              MultiplyingEnumeratee(k(Input.El(number * 2)))
            case Input.Empty => MultiplyingEnumeratee(k(Input.Empty))
            case Input.EOF => Done(Cont(k))
          })
          case _ => Future(Done(inner, Input.Empty))
        })
      }
    }

   val enumerateeFromInheritance: Enumeratee[Int, Int] =
     MultiplyingEnumeratee


    val enumerateeFromConstructor: Enumeratee[Int, Int] =
      Enumeratee.map(_ * 2)

  }

  object ApplicationOnIteratees {
    val t: Enumeratee[Int, Int] = Enumeratee.map(_ * 2)
    val i: Iteratee[Int, List[Int]] = Iteratee.getChunks
    val e1: Enumerator[Int] = Enumerator(1, 2)
    val e2: Enumerator[Int] = Enumerator(3, 4)

    val transformedI: Iteratee[Int, Iteratee[Int, List[Int]]] = t(i)
    val originalI: Iteratee[Int, List[Int]] =
      Iteratee.flatten(e1.run(transformedI))
    val result: Future[List[Int]] = e2.run(originalI)
    // result = Future.successful(List(2, 4, 3, 4))
  }

  object ApplicationOnEnumerators {
    val t: Enumeratee[Int, String] = Enumeratee.map(_.toString)
    val e: Enumerator[Int] = Enumerator(1, 2, 3)
    val transformedE: Enumerator[String] = e.through(t)
  }

  object ApplicationOnEnumeratees {
    val t1: Enumeratee[Int, Int] = Enumeratee.filter(_ % 2 == 0)
    val t2: Enumeratee[Int, String] = Enumeratee.map(_.toString)
    val t12: Enumeratee[Int, String] = t1.compose(t2)

    val e: Enumerator[Int] = Enumerator(1, 2, 3)
    val i: Iteratee[String, List[String]] = Iteratee.getChunks

    val result: Future[List[String]] = e.through(t12).run(i)
    // result = Future.successful(List("2"))
  }

}