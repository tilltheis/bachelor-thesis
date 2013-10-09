package examples.reactive.streams

import play.api.libs.iteratee._
import scala.concurrent._
import ExecutionContext.Implicits.global

object Enumeratees {
  object Creation {

    object Rejuvinating {

      case object RejuvinatingEnumeratee extends Enumeratee[Int, Int] {
        def applyOn[A](inner: Iteratee[Int, A]):
            Iteratee[Int, Iteratee[Int, A]] =
          Iteratee.flatten(inner.fold {
            case Step.Cont(k) => Future(Cont {
              case Input.El(number) if number >= 50 =>
                RejuvinatingEnumeratee(k(Input.El(number - 10)))
              case Input.El(number) =>
                RejuvinatingEnumeratee(k(Input.El(number)))
              case Input.Empty => RejuvinatingEnumeratee(k(Input.Empty))
              case Input.EOF => Done(Cont(k))
            })
            case _ => Future(Done(inner, Input.Empty))
          })
      }

      val enumerateeFromInheritance: Enumeratee[Int, Int] =
        RejuvinatingEnumeratee


      val enumerateeFromConstructor: Enumeratee[Int, Int] =
        Enumeratee.map(x => if (x >= 50) x - 10 else x)

    }

  }

  object ApplicationOnIteratees {
    val t: Enumeratee[Int, Int] =
      Enumeratee.map(x => if (x >= 50) x - 10 else x)
    val i: Iteratee[Int, List[Int]] = Iteratee.getChunks
    val e: Enumerator[Int] = Enumerator(22, 54)

    val transformedI: Iteratee[Int, Iteratee[Int, List[Int]]] = t(i)
    val originalI: Iteratee[Int, List[Int]] =
      Iteratee.flatten(e.run(transformedI))

    val result: Future[List[Int]] = e.run(originalI)
    // result hat den Wert Future(List(22, 44, 22, 54))
  }

  object ApplicationOnEnumerators {
    val t: Enumeratee[Int, String] = Enumeratee.map(_.toString)
    val e: Enumerator[Int] = Enumerator(1, 2, 3)
    val transformedE: Enumerator[String] = e.through(t)
  }

  object ApplicationOnEnumeratees {
    val t1: Enumeratee[Int, Int] = Enumeratee.filter(_ < 70)
    val t2: Enumeratee[Int, Int] =
      Enumeratee.map(x => if (x >= 50) x - 10 else x)
    val t12: Enumeratee[Int, Int] = t1.compose(t2)

    val e: Enumerator[Int] = Enumerator(22, 25, 54, 76)
    val i: Iteratee[Int, List[Int]] = Iteratee.getChunks

    val result: Future[List[Int]] = e.through(t12).run(i)
    // result hat den Wert Future(List(22, 25, 44))
  }

}