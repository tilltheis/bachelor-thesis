package examples.reactive.monads

import scala.language.higherKinds

trait KleisliTriple[M[_]] {
  def unit[A](a: A): M[A]
  def map[A, B](f: A => B, m: M[A]): M[B]
  def bind[A, B](f: A => M[B]): M[A] => M[B]
}

object KleisliTriple {
  implicit object OptionKleisliTriple extends KleisliTriple[Option] {
    def unit[A](a: A): Option[A] = Some(a)
    def map[A, B](f: A => B, m: Option[A]): Option[B] = m.map(f)
    def bind[A, B](f: A => Option[B]): Option[A] => Option[B] = m => m.flatMap(f)
  }
}