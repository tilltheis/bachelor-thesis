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

//    def multiplyingEnumeratee = new Enumeratee[Int, Int] {
//      def applyOn[A](inner: Iteratee[Int, A]):
//          Iteratee[Int, Iteratee[Int, A]] = {
//        Iteratee.fold(inner) { (inner, number) =>
//          Iteratee.flatten(Enumerator(number * 2).apply(inner))
//        }
//
////        new Iteratee[Int, Iteratee[Int, A]] {
////          def fold()
////        }
//
//        def step(inner: Iteratee[Int, A]) =
//          Cont[Int, Iteratee[Int, A]] { in =>
//            Iteratee.flatten(Enumerator.enumInput(in.map(_ * 2)).apply(inner))
//          }
//
//        inner.unflatten.
//      }
//    }

//    val enumerateeFromInheritance: Enumeratee[Int, Int] = Enumeratee.map(_ * 2)
   val enumerateeFromInheritance: Enumeratee[Int, Int] =
     MultiplyingEnumeratee
    // val enumerateeFromInheritance: Enumeratee[Int, Int] =
    //   multiplyingEnumeratee
  }
}