package test

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

object Helpers {
  def await[A](future: Future[A], duration: Duration = 1.second): A =
    Await.result(future, duration)
}