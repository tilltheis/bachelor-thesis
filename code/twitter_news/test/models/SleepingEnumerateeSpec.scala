package models

import org.joda.time.Duration

import play.api.test._

import play.api.libs.iteratee.{Enumerator, Enumeratee, Iteratee, Concurrent}


class SleepingEnumerateeSpec extends PlaySpecification {
  "The SleepingEnumeratee" should {
    "ignore all elements while sleeping" in {
      val t = SleepingEnumeratee[Int](Duration.millis(500))
      val (e, c) = Concurrent.broadcast[Int]
      val i = Iteratee.getChunks[Int]
      val resultM = e.run(t.transform(Enumeratee.take(3).transform(i)))

      c.push(1)
      Thread.sleep(500)
      c.push(2)
      c.push(3)
      Thread.sleep(500)
      c.push(4)

      List(1, 2, 4) === await(resultM)
    }

    "not ignore Input.EOF" in {
      val t = SleepingEnumeratee[Int](Duration.millis(500))
      val e = Enumerator(1, 2, 3)
      val i = Iteratee.getChunks[Int]
      val resultM = e.run(t.transform(i))

      List(1) === await(resultM)
    }

    "ignore nothing if sleep duration is zero" in {
      val t = SleepingEnumeratee[Int](Duration.ZERO)
      val e = Enumerator(1, 2, 3)
      val i = Iteratee.getChunks[Int]
      val resultM = e.run(t.transform(i))

      List(1, 2, 3) === await(resultM)
    }
  }
}
