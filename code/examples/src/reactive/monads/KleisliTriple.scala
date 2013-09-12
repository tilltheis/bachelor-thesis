package examples.reactive.monads

import scala.language.higherKinds

import scala.util.Try

trait KleisliTriple[M[_]] {
  def unit[A](a: A): M[A]
  def bind[A, B](f: A => M[B]): M[A] => M[B]
}

object KleisliTriple {
  implicit object OptionKleisliTriple extends KleisliTriple[Option] {
    def unit[A](a: A): Option[A] = Some(a)
    def bind[A, B](f: A => Option[B]): Option[A] => Option[B] = m => m.flatMap(f)
  }

  implicit object ListKleisliTriple extends KleisliTriple[List] {
    def unit[A](a: A): List[A] = List(a)
    def bind[A, B](f: A => List[B]): List[A] => List[B] = m => m.flatMap(f)
  }

  implicit object TryKleisliTriple extends KleisliTriple[Try] {
    def unit[A](a: A): Try[A] = Try(a)
    def bind[A, B](f: A => Try[B]): Try[A] => Try[B] = m => m.flatMap(f)
  }
}