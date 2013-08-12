package reactive.streams

import scala.language.reflectiveCalls

import org.specs2.mutable._
import org.specs2.time.NoTimeConversions
import play.api.libs.iteratee._
import test.Helpers.await
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RulesSpec extends Specification with NoTimeConversions {
  type E = Char
  type A = List[Char]
  type I = Iteratee[E, A]

  def prefixIteratee(prefix: String): I = Iteratee.foldM[E, String]("") {
    case (`prefix`, _) => Future(prefix)
    case (s, c) if prefix.charAt(s.length) == c => Future(s + c)
    case _ => Future.failed(new Exception("prefix not found"))
  }.map(_.toList)

  val s1 = "foo"
  val s2 = "bar"

  def unitIteratee(x: A): I = Iteratee.ignore.map(_ => x)
  def en_str(s: String)(i: I): I =
    Iteratee.flatten(Enumerator.enumerate(s).apply(i))

  val i: I = prefixIteratee("foo")
  val f: A => I = unitIteratee(_)

  "an enumerator from concatenated inputs" should {
    "be the same as concatenating enumerators" in {
      val left: I => I = en_str(s1 + s2)(_)
      val right: I => I = (en_str(s1)(_)).andThen(en_str(s2))

//      println(await(left(i).run))
//      println(await(right(i).run))

//      await(e1.run(i)) === await(e2.run(i))
      await(left(i).run) === await(right(i).run)
    }
  }


  "an iteratee that recognizes the first of two inputs" should {
    "ignore the second input" in {
      val left: I = en_str(s1 + s2)(i.flatMap(f))
      val right: I = en_str(s1)(i).flatMap(x => en_str(s2)(f(x)))

//      println(await(left.run))
//      println(await(right.run))

      await(left.run) === await(right.run)
    }
  }
}
