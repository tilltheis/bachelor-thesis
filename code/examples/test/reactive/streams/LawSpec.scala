package reactive.streams

import org.specs2.mutable._
import org.specs2.ScalaCheck
import org.scalacheck._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.libs.iteratee._

import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers // for await

class LawSpec extends Specification with ScalaCheck {

  type E     = Char           // Der Eingabeelementtyp (Char)
  type I[A]  = Iteratee[E, A] // Ein Iteratee von Char nach A
  type M[A]  = Future[A]      // Die Monade (Future)
  type IM[A] = M[I[A]]        // Ein Iteratee in der Future-Monade

  def en_str(s: String): Enumerator[E] = Enumerator.enumerate(s)

  def runM[A](mi: IM[A]): M[A] = Iteratee.flatten(mi).run

  def unsafeHeadIteratee[E]: Iteratee[E, E] = Cont {
    case Input.El(el) => Done(el, Input.Empty)
    case Input.Empty  => unsafeHeadIteratee
    case Input.EOF    => Error("premature EOF", Input.EOF)
  }

  def prefixIteratee(prefix: String): I[String] = {
    def go(s: String): I[String] = s match {
      case "" => Done(prefix)
      case _  => Cont {
        case Input.El(c) if s.head == c => go(s.tail)
        case Input.Empty => go(s)
        case input => Error("prefix not found", input)
      }
    }

    go(prefix)
  }


  "unsafeHeadIteratee" should {
    "recognize inputs with at least one element" in check {
      (s: String) => !s.isEmpty ==> {
        Helpers.await(runM(en_str(s)(unsafeHeadIteratee))) === s.head
      }
    }

    "not recognize empty inputs" in {
      Helpers.await(unsafeHeadIteratee[Any].run) must throwA[RuntimeException].like {
        case e => e.getMessage === "premature EOF"
      }
    }
  }

  "prefixIteratee" should {
    "recognize inputs that begin with a given prefix" in check {
      (s: String) => {
        val prefix = s.take(scala.util.Random.nextInt(s.length + 1))
        Helpers.await(runM(en_str(s)(prefixIteratee(prefix)))) === prefix
      }
    }

    "not recognize inputs that do not begin with a given prefix" in check {
      (s: String, prefix: String) => !s.startsWith(prefix) ==> {
        Helpers.await(prefixIteratee(prefix).run) must throwA[RuntimeException].like {
          case e => e.getMessage === "prefix not found"
        }
      }
    }
  }



  "the composition of effectful enumerators" should {
    "correspond to the concatenation of their sources." in check {
        (s1: String, s2: String) =>
      val left = en_str(s1 + s2)
      val right = en_str(s1).andThen(en_str(s2))

      val i = Iteratee.head[Char]

      Helpers.await(runM(left(i))) === Helpers.await(runM(right(i)))
    }
  }


  "an effectful iteratee that recognizes the string s" should {
    "recognize (s+s2) for any s2" in check {
      (s1: String, s2: String) => s1.length > 0 ==> {
        def flatMap[A, B](m: IM[A], f: A => IM[B]): IM[B] =
          m.map(_.flatMapM(f))

        val i: I[E] = unsafeHeadIteratee
        val f: E => I[String] = c => Done(s"f($c)")

        val left: IM[String] = en_str(s1 + s2)(i.flatMap(f))
        val right: IM[String] = flatMap(en_str(s1)(i), (x: E) => en_str(s2)(f(x)))

        Helpers.await(runM(left)) === Helpers.await(runM(right))
      }
    }
  }


  "an effectful iteratee that does not recognize the string s" should {
    "not recognize anything" in check {
      (s: String) => (s.isEmpty || s.charAt(0) != 'a') ==> {
        val i: I[String] = prefixIteratee("a")
        val f: String => I[String] = x => Done(s"f($x)")

        val left: IM[String] = en_str(s)(i.flatMap(f))
        val right: IM[String] = en_str(s)(i).map(_.flatMap(f))

        runM(left).foreach(x => println(s"left: $x"))
        runM(right).foreach(x => println(s"right: $x"))

        def matchNotRecognizingIteratee(i: IM[String]) =
          Helpers.await(runM(i)) must throwA[RuntimeException].like {
            case e => e.getMessage === "prefix not found"
          }

        matchNotRecognizingIteratee(left) and matchNotRecognizingIteratee(right)
      }
    }
  }


  "flatMap'ing over a diverging effectful iteratee" should {
    "result in another diverging effectful iteratee" in {
      // the absorbing or zero element of flatMap
      def failure[A]: I[A] = Cont(_ => failure)

      val f: String => I[String] = x => Done(s"f(${x.mkString})")

      val left: I[String] = failure.flatMap(f)
      val right: I[String] = failure

      def matchDivergingIteratee[A](i: I[A]) =
        Helpers.await(i.run) must throwA[RuntimeException].like {
          case e => e.getMessage === "diverging iteratee after Input.EOF"
        }

      matchDivergingIteratee(left) and matchDivergingIteratee(right)
    }
  }


  "flatMap over effectful iteratees" should {
    "be right distributive if it is idempotent" in check {
      (s: String) => !s.isEmpty ==> {
        def toFuturePair[A](p: (Future[A], Future[A])): Future[(A, A)] =
          Future.sequence(Seq(p._1, p._2)).map { case Seq(x, y) => (x, y) }

        def isIdempotent[A](i: I[A]) = {
          val left: M[(I[A], I[A])] = en_str(s)(i).flatMap(x => Future((x, x)))
          val right: M[(I[A], I[A])] = en_str(s)(i).flatMap(x => en_str(s)(i).flatMap(y => Future((x, y))))

          val leftResult: M[(A, A)] = left.flatMap(p => toFuturePair(p._1.run, p._2.run))
          val rightResult: M[(A, A)] = right.flatMap(p => toFuturePair(p._1.run, p._2.run))

          // println("left: " + Helpers.await(leftResult) + " right: " + Helpers.await(rightResult))

          Helpers.await(leftResult) == Helpers.await(rightResult)
        }

        // left biased alternative
        def alternative[A](i1: I[A], i2: I[A]): I[A] =
          Iteratee.flatten(toFuturePair((i1.unflatten, i2.unflatten)).map {
            case (Step.Done(value, rest), _)    => Done(value, rest)
            case (_, Step.Done(value, rest))    => Done(value, rest)
            case (Step.Error(message, rest), _) => Error(message, rest)
            case (_, Step.Error(message, rest)) => Error(message, rest)
            case (Step.Cont(k1), Step.Cont(k2)) =>
              Cont(in => alternative(k1(in), k2(in)))
          })

        val i: I[String] = Iteratee.head.map(_ => "i")

        val k1: String => I[String] = x => Iteratee.head.map(_ => s"k1($x)")
        val k2: String => I[String] = x => Done(s"k2($x)")

        val left: I[String] = i.flatMap(x => alternative(k1(x), k2(x)))
        val right: I[String] = alternative(i.flatMap(k1), i.flatMap(k2))

        def run[A](i: I[A]) = runM(en_str(s)(i))

        // run(left).foreach(x => println(s"left: $x"))
        // run(right).foreach(x => println(s"right: $x"))

        val matchIdempotence = isIdempotent(i) === true
        val matchEquality = Helpers.await(run(left)) === Helpers.await(run(right))

        matchIdempotence and matchEquality
      }
    }
  }

}