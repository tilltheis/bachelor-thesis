package examples.reactive.monads

import scala.language.higherKinds

trait Monad[M[_]] {
  def unit[A](a: A): M[A]
  def map[A, B](f: A => B, m: M[A]): M[B]
  def join[A](mm: M[M[A]]): M[A]
}

object Monad {
  implicit object OptionMonad extends Monad[Option] {
    def unit[A](a: A): Option[A] = Some(a)
    def map[A, B](f: A => B, m: Option[A]): Option[B] = m.map(f)
    def join[A](mm: Option[Option[A]]): Option[A] = mm.flatMap((m: Option[A]) => m)
  }
}