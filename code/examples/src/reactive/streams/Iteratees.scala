package examples.reactive.streams

import play.api.libs.iteratee._
import scala.concurrent._
import ExecutionContext.Implicits.global

object Iteratees extends App {
  object Creation {

    case class SumIteratee(sum: Int = 0) extends Iteratee[Int, Int] {
      def fold[B](folder: Step[Int, Int] => Future[B]): Future[B] = {
        folder(Step.Cont(_ match {
          case Input.El(i) => SumIteratee(sum + i)
          case Input.Empty => this
          case Input.EOF   => new Iteratee[Int, Int] {
            def fold[B](folder: Step[Int, Int] => Future[B]) = {
              folder(Step.Done(sum, Input.EOF))
            }
          }
        }))
      }
    }

    val sumIterateeFromInheritance: Iteratee[Int, Int] = SumIteratee()


    def sumIteratee(sum: Int = 0): Iteratee[Int, Int] = Cont(_ match {
      case Input.El(i) => sumIteratee(sum + i)
      case Input.Empty => sumIteratee(sum)
      case Input.EOF   => Done(sum, Input.EOF)
    })

    val sumIterateeFromConstructor: Iteratee[Int, Int] = sumIteratee()


    val sumIterateeFromHelper: Iteratee[Int, Int] =
      Iteratee.fold(0)(_ + _)


    def folder(xs: List[Int])(step: Step[Int, Int]): Future[Int] =
      xs match {
        case Nil => step match {
          case Step.Cont(k) => k(Input.EOF).fold(_ match {
            case Step.Done(sum, Input.EOF) => Future(sum)
            case _ => Future.failed(new Exception("invalid state"))
          })
          case _ => Future.failed(new Exception("invalid state"))
        }
        case x :: xs => step match {
          case Step.Cont(k) => k(Input.El(x)).fold(folder(xs))
          case _ => Future.failed(new Exception("invalid state"))
        }
      }

    val sumResult: Future[Int] =
      sumIterateeFromHelper.fold(folder(List(1, 4, -2)))
  }
}