package models

import org.joda.time.{Duration, DateTime}

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumeratee.CheckDone


object SleepingEnumeratee {
  def apply[E](sleepDuration: Duration): SleepingEnumeratee[E] =
    new SleepingEnumeratee[E](sleepDuration)
}

// ignores all elements except Input.EOF and the very first element while sleeping
class SleepingEnumeratee[E](sleepDuration: Duration) extends Enumeratee[E, E] {

  def applyOn[A](inner: Iteratee[E, A]): Iteratee[E, Iteratee[E, A]] =
    if (sleepDuration == Duration.ZERO) {
      // special case because it can happen that no measurable time passes between elements
      Enumeratee.passAlong.applyOn(inner)
    } else {
      enumeratee.applyOn(inner)
    }

  // code inspired by the predefined enumerators in https://github.com/playframework/playframework/blob/2.2.0/framework/src/iteratees/src/main/scala/play/api/libs/iteratee/Enumeratee.scala
  private val enumeratee = new CheckDone[E, E] {
    def step[A](sleepingSince: DateTime)(k: K[E, A]): K[E, Iteratee[E, A]] = {
      case Input.EOF => Done(Cont(k), Input.EOF)
      case in@Input.El(el) if sleepingSince.plus(sleepDuration).isBeforeNow() =>
        new CheckDone[E, E] { def continue[A](k: K[E, A]) = Cont(step(DateTime.now())(k)) } &> k(in)
      case _ => Cont(step(sleepingSince)(k))
    }

    def continue[A](k: K[E, A]) = Cont(step(new DateTime(Long.MinValue))(k))
  }
}
