package examples.reactive.monads

import org.specs2.mutable._

class LawSpec extends Specification {

  type T = Char
  type U = Int
  type V = String

  def f(x: T): M[U] = unit(x.toInt)
  def g(x: U): M[V] = unit(x.toString)

  val x: T = 'a'

  type M[A] = Option[A]
  def unit[A](x: A): M[A] = Some(x)
  val m: M[T] = Some('a')

  // type M[A] = List[A]
  // def unit[A](x: A): M[A] = List(x)
  // val m: M[T] = List('a', 'b', 'c')


  "left neutral element law" should {
    "hold" in {
      unit(x).flatMap(f) === f(x)
    }
  }

  "right neutral element law" should {
    "hold" in {
      m.flatMap(unit) === m
    }
  }

  "associativity law" should {
    "hold" in {
      m.flatMap(f).flatMap(g) === m.flatMap(x => f(x).flatMap(g))
    }
  }
}