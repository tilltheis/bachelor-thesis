import play.api.libs.iteratee._
import scala.concurrent.Future

object Iteratees {
  val loggerIteratee: Iteratee[String, Unit] =
    Iteratee.foreach(println)
  val namesEnumerator: Enumerator[String] =
    Enumerator("Foo", "Bar", "Baz")
  val appliedLoggerIteratee: Future[Iteratee[String, Unit]] =
    namesEnumerator(loggerIteratee)
}