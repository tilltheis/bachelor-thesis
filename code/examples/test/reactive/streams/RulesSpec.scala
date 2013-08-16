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
  // EFFECTLESS ITERATEES
  {
    type E = Char
    type A = List[Char]
    type I = Iteratee[E, A]

    val s1 = "foo"
    val s2 = "bar"

    //  def prefixIteratee(prefix: String): I = Iteratee.foldM[E, String]("") {
    //    case (`prefix`, _) => Future(prefix)
    //    case (s, c) if prefix.charAt(s.length) == c => Future(s + c)
    //    case _ => Future.failed(new Exception("prefix not found"))
    //  }.map(_.toList)

    def prefixIteratee(prefix: String): I = {
      def go(s: String): I = s match {
        case "" => Done(prefix.toList)
        case _  => Cont {
          case Input.El(c) if s.head == c => go(s.tail)
          case Input.Empty => go(s)
          case input => Error("prefix not found", input)
        }
      }

      go(prefix)
    }


    val i: I = prefixIteratee("foo")
    //  val f: A => I = Done(_)
    val f: A => I = x => Done(s"f(${x.mkString})".toList)


    def en_str(s: String): I => I = (i: I) =>
      Iteratee.flatten(Enumerator.enumerate(s).apply(i))


    "the composition of enumerators" should {
      "correspond to the concatenation of their sources." in {
        val left: I => I = en_str(s1 + s2)
        val right: I => I = en_str(s2).compose(en_str(s1))

  //      println(await(left(i).run))
  //      println(await(right(i).run))

        await(left(i).run) === await(right(i).run)
      }
    }


    "an iteratee that recognizes the string s" should {
      "recognize (s+s2) for any s2" in {
        val left: I = en_str(s1 + s2)(i.flatMap(f))
        val right: I = en_str(s1)(i).flatMap(en_str(s2).compose(f))

  //      println(await(left.run))
  //      println(await(right.run))

        await(left.run) === await(right.run)
      }
    }


    "flatMap'ing over a diverging iteratee" should {
      "result in another diverging iteratee" in {

        // http://wwwmath.uni-muenster.de/u/lammers/EDU/ss11/DiskreteStrukturen/Folien/V-2011-05-03.pdf
        // http://de.wikipedia.org/wiki/Absorbierendes_Element
        // http://en.wikipedia.org/wiki/Absorbing_element
        // http://en.wikipedia.org/wiki/Semigroup#Identity_and_zero
        // http://de.wikipedia.org/wiki/Halbgruppe#Absorption

        // the absorbing or zero element of flatMap
        lazy val failure: I = Cont(_ => failure)

        val left: I = failure.flatMap(f)
        val right: I = failure

        def matchDivergingIteratee(i: I) =
          await(i.run) must throwA[RuntimeException].like {
            case e => e.getMessage === "diverging iteratee after Input.EOF"
          }

        matchDivergingIteratee(left) and matchDivergingIteratee(right)
      }
    }


    "flatMap over iteratees" should {
      "be right distributive" in {
        // left biased alternative
        def alternative(i1: I, i2: I): I = {
          def toFuturePair[A](p: (Future[A], Future[A])): Future[(A, A)] =
            Future.sequence(Seq(p._1, p._2)) map { case Seq(x, y) => (x, y) }

          def flatFeed(i: I, in: Input[E]): I = Iteratee.flatten(i.feed(in))

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

        val i: I = Iteratee.head.map(_ => "i".toList)
  //      val k1: A => I = x => Done(x)
        val k1: A => I = x => Iteratee.head.map(x => s"k1(${x.mkString})".toList)
        val k2: A => I = x => Done(s"k2(${x.mkString})".toList)

        val left: I = i.flatMap(x => alternative(k1(x), k2(x)))
        val right: I = alternative(i.flatMap(k1), i.flatMap(k2))

        def run(i: I) = en_str("foobar")(i).run

//        println(await(run(left)))
//        println(await(run(right)))

        await(run(left)) === await(run(right))
      }
    }
  }


  // EFFECTFUL ITERATEES
  {
    type E = Char
    type I[A] = Iteratee[E, A]
    type M[A] = Future[A]
    type IM[A] = M[I[A]]

    def en_str[A](s: String): I[A] => IM[A] =
      Enumerator.enumerate(s).apply(_)

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
        implicit class KleisliOperator[A, B](f1: A => M[B]) {
          def >>>[C](f2: B => M[C]): A => M[C] = x => f1(x).flatMap(f2)
        }

        val s1: String = "foo"
        val s2: String = "bar"

        val left: I[String] => IM[String] = en_str(s1 + s2)
        val right: I[String] => IM[String] = en_str[String](s1) >>> en_str[String](s2)

        val i: I[String] = prefixIteratee("foo")

        runM(left(i)).foreach(println)
        runM(right(i)).foreach(println)

        await(runM(left(i))) === await(runM(right(i)))
      }
    }


    "an effectful iteratee that recognizes the string s" should {
      "recognize (s+s2) for any s2" in {
        def bind[A, B](m: IM[A], f: A => IM[B]): IM[B] =
          m.map(_.flatMapM(f))

        val s1: String = "foo"
        val s2: String = "bar"

        val i: I[String] = prefixIteratee("foo")
        val f: String => I[String] = x => Done(s"f($x)")

        val left: IM[String] = en_str(s1 + s2)(i.flatMap(f))
        val right: IM[String] = bind(en_str(s1)(i), en_str(s2).compose(f))

        runM(left).foreach(println)
        runM(right).foreach(println)

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

        runM(left).foreach(println)
        runM(right).foreach(println)

        def matchNotRecognizingIteratee(i: IM[String]) =
          await(runM(i)) must throwA[RuntimeException].like {
            case e => e.getMessage === "prefix not found"
          }

        matchNotRecognizingIteratee(left) and matchNotRecognizingIteratee(right)
      }
    }


    "flatMap'ing over a diverging effectful iteratee" should {
      "result in another diverging effectful iteratee" in {

        // http://wwwmath.uni-muenster.de/u/lammers/EDU/ss11/DiskreteStrukturen/Folien/V-2011-05-03.pdf
        // http://de.wikipedia.org/wiki/Absorbierendes_Element
        // http://en.wikipedia.org/wiki/Absorbing_element
        // http://en.wikipedia.org/wiki/Semigroup#Identity_and_zero
        // http://de.wikipedia.org/wiki/Halbgruppe#Absorption

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
      "be right distributive" in {
        implicit class LeftBiasedAlternativeOperator[A](i1: I[A]) {
          def <|(i2: I[A]): I[A] = {
            def toFuturePair[A](p: (Future[A], Future[A])): Future[(A, A)] =
              Future.sequence(Seq(p._1, p._2)) map { case Seq(x, y) => (x, y) }

            def flatFeed(i: I[A], in: Input[E]): I[A] = Iteratee.flatten(i.feed(in))

            val doneM = toFuturePair(Iteratee.isDoneOrError(i1), Iteratee.isDoneOrError(i2))
            Iteratee.flatten(doneM.map {
              case (true, _) => i1
              case (_, true) => i2
              case _         => new Iteratee[E, A] {
                def fold[B](folder: (Step[E, A]) => Future[B]): Future[B] = folder(Step.Cont {
                  in => flatFeed(i1, in) <| flatFeed(i2, in)
                })
              }
            })
          }
        }

        val i: I[String] = Iteratee.head.map(_ => "i")

        val k1: String => I[String] = x => Iteratee.head.map(x => s"k1($x)")
        val k2: String => I[String] = x => Done(s"k2($x)")

        val left: I[String] = i.flatMap(x => k1(x) <| k2(x))
        val right: I[String] = i.flatMap(k1) <| i.flatMap(k2)

        def run[A](i: I[A]) = runM(en_str("foobar")(i))

        run(left).foreach(println)
        run(right).foreach(println)

        await(run(left)) === await(run(right))
      }
    }
  }
}
