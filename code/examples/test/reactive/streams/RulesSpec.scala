package reactive.streams

import scala.language.reflectiveCalls

import org.specs2.mutable._
import org.specs2.time.NoTimeConversions
import play.api.libs.iteratee._
import test.Helpers.await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.io.{PrintWriter, FileOutputStream}

class RulesSpec extends Specification with NoTimeConversions {

  type E     = Char           // Der Eingabeelementtyp (Char)
  type I[A]  = Iteratee[E, A] // Ein Iteratee von Char nach A
  type M[A]  = Future[A]      // Die Monade (Future)
  type IM[A] = M[I[A]]        // Ein Iteratee in der Future-Monade

  def en_str(s: String): Enumerator[E] = Enumerator.enumerate(s)

  def runM[A](mi: IM[A]): M[A] = Iteratee.flatten(mi).run

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

  "the composition of effectful enumerators" should {
    "correspond to the concatenation of their sources." in {
      val s1: String = "foo"
      val s2: String = "bar"

      val left: Enumerator[E] = en_str(s1 + s2)
      val right: Enumerator[E] = en_str(s1).andThen(en_str(s2))

      val i: I[String] = prefixIteratee("foo")

      // runM(left(i)).foreach(println) // with this enabled there will be a wrong result
      // runM(right(i)).foreach(println) // with this enabled there will be a wrong result

      await(runM(left(i))) === await(runM(right(i)))
    }
  }


  "an effectful iteratee that recognizes the string s" should {
    "recognize (s+s2) for any s2" in {
      def flatMap[A, B](m: IM[A], f: A => IM[B]): IM[B] =
        m.map(_.flatMapM(f))

      val s1: String = "foo"
      val s2: String = "bar"

      val i: I[String] = prefixIteratee("foo")
      val f: String => I[String] = x => Done(s"f($x)")

      val left: IM[String] = en_str(s1 + s2)(i.flatMap(f))
      val right: IM[String] = flatMap(en_str(s1)(i), (x: String) => en_str(s2)(f(x)))

      // runM(left).foreach(println)
      // runM(right).foreach(println)

      await(runM(left)) === await(runM(right))
    }
  }


  "an effectful iteratee that does not recognize the string s" should {
    "not recognize anything" in {
      val s: String = "baz"

      val i: I[String] = prefixIteratee("foo")
      val f: String => I[String] = x => Done(s"f($x)")

      val left: IM[String] = en_str(s)(i.flatMap(f))
      val right: IM[String] = en_str(s)(i).map(_.flatMap(f))

      // runM(left).foreach(println)
      // runM(right).foreach(println)

      def matchNotRecognizingIteratee(i: IM[String]) =
        await(runM(i)) must throwA[RuntimeException].like {
          case e => e.getMessage === "prefix not found"
        }

      matchNotRecognizingIteratee(left) and matchNotRecognizingIteratee(right)
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
        await(i.run) must throwA[RuntimeException].like {
          case e => e.getMessage === "diverging iteratee after Input.EOF"
        }

      matchDivergingIteratee(left) and matchDivergingIteratee(right)
    }
  }


  "flatMap over effectful iteratees" should {
    "be right distributive if it is idempotent" in {
      def toFuturePair[A](p: (Future[A], Future[A])): Future[(A, A)] =
        Future.sequence(Seq(p._1, p._2)).map { case Seq(x, y) => (x, y) }

      def isIdempotent[A](i: I[A]) = {
        val s: String = "foobar"
        val left: M[(I[A], I[A])] = en_str(s)(i).flatMap(x => Future((x, x)))
        val right: M[(I[A], I[A])] = en_str(s)(i).flatMap(x => en_str(s)(i).flatMap(y => Future((x, y))))
        val leftResult: M[(A, A)] = left.flatMap(p => toFuturePair(p._1.run, p._2.run))
        val rightResult: M[(A, A)] = right.flatMap(p => toFuturePair(p._1.run, p._2.run))
        // println("left: " + await(leftResult) + " right: " + await(rightResult))
        await(leftResult) == await(rightResult)
      }

      // left biased alternative
      def alternative[A](i1: I[A], i2: I[A]): I[A] = {
        def flatFeed(i: I[A], in: Input[E]): I[A] = Iteratee.flatten(i.feed(in))

        val doneM = toFuturePair(Iteratee.isDoneOrError(i1), Iteratee.isDoneOrError(i2))
        Iteratee.flatten(doneM.map {
          case (true, _) => i1
          case (_, true) => i2
          case _         => new Iteratee[E, A] {
            def fold[B](folder: (Step[E, A]) => Future[B]): Future[B] = folder(Step.Cont {
              in => alternative(flatFeed(i1, in), flatFeed(i2, in))
            })
          }
        })
      }

      val i: I[String] = Iteratee.head.map(_ => "i")

      val k1: String => I[String] = x => Iteratee.head.map(x => s"k1($x)")
      val k2: String => I[String] = x => Done(s"k2($x)")

      val left: I[String] = i.flatMap(x => alternative(k1(x), k2(x)))
      val right: I[String] = alternative(i.flatMap(k1), i.flatMap(k2))

      def run[A](i: I[A]) = runM(en_str("foobar")(i))

      // run(left).foreach(println)
      // run(right).foreach(println)

       val matchIdempotence = isIdempotent(i) === true
      val matchEquality = await(run(left)) === await(run(right))

      matchIdempotence and matchEquality
    }
  }
}
