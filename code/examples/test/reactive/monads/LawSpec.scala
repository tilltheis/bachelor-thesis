package examples.reactive.monads

import scala.language.higherKinds

import scala.util.Try

import org.specs2.mutable._

class LawSpec extends Specification {

  implicit class Monad[M[_], A](m: M[A])(implicit kleisli: KleisliTriple[M]) {
    def map[B](f: A => B): M[B] = kleisli.map(f, m)
    def flatMap[B](f: A => M[B]): M[B] = kleisli.bind(f)(m)
  }

  type T = Char
  type U = Int
  type V = String

  testMonad(Option('a'), "Option (Some)")
  testMonad(Option.empty[T], "Option (None)")
  testMonad(List('a', 'b', 'c'), "List")
  testMonad(List.empty[T], "List (empty)")
  testMonad(Try('a'), "Try (Success)")
  testMonad(Try[T](throw new RuntimeException), "Try (Failure)")

  def testMonad[M[_]](m: M[T], description: String)(implicit kleisli: KleisliTriple[M]) = {
    def unit[A](x: A): M[A] = kleisli.unit(x)

    def f(x: T): M[U] = unit(x.toInt)
    def g(x: U): M[V] = unit(x.toString)

    val x: T = 'a'

    "left neutral element law" should {
      s"hold for $description" in {
        unit(x).flatMap(f) === f(x)
      }
    }

    "right neutral element law" should {
      s"hold for $description" in {
        m.flatMap(unit) === m
      }
    }

    "associativity law" should {
      s"hold for $description" in {
        m.flatMap(f).flatMap(g) === m.flatMap(x => f(x).flatMap(g))
      }
    }

  }
}