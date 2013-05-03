package examples.reactive.streams

import play.api.libs.iteratee._
import scala.concurrent.Future

object Iteratees extends App {
  // val loggerIteratee: Iteratee[String, Unit] =
  //   Iteratee.foreach(println)
  // val namesEnumerator: Enumerator[String] =
  //   Enumerator("Foo", "Bar", "Baz")
  // val appliedLoggerIteratee: Future[Iteratee[String, Unit]] =
  //   namesEnumerator(loggerIteratee)
  // // loggerIteratee.run


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

  }

  // Thread.sleep(1000)
  // // sys.exit
  // val threadSet = Thread.getAllStackTraces.keySet
  // threadSet.toArray(new Thread(threadSet.size)).foreach(_.interrupt)
}