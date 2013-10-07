package examples.reactive.futures

import scala.concurrent._
import ExecutionContext.Implicits.global

object Examples {
  object CombinationExample {
    val f1: Future[Int] = Future(35)
    val f2: Future[Int] = Future(40)

    val f1f2: Future[(Int, Int)] = for {
      age1 <- f1
      age2 <- f2
    } yield (age1, age2)
    // f1f2 hat den Wert Future((35, 40))
  }

  object PromiseExample {
    val promise: Promise[Int] = Promise()
    val future: Future[Int] = promise.future

    future.foreach(x => println(s"Die Zahl ist $x."))
    // gibt nach 2 Sekunden "Die Zahl ist 42." aus

    Future {
      Thread.sleep(2000)
      promise.success(42)
    }
  }
}
