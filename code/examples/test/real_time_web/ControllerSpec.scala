package examples.real_time_web

import play.api.test._

class ControllerSpec extends PlaySpecification {

  "the weirdEventSource-Action" should {
    "stream numbers within single quotes" in {
      val resultM = Controller.weirdEventSource.apply(FakeRequest())
      val result = contentAsString(resultM)

      Seq(44, 34, 50).map { x =>
        result must contain(s"'$x'")
      }
    }
  }

  Seq(
    ("correct", Controller.correctEventSource),
    ("int", Controller.intEventSource)
  ).foreach { case (kind, eventSource) =>
    s"the ${kind}EventSource-Action" should {
      "stream numbers without single quotes" in {
        val resultM = eventSource.apply(FakeRequest())
        val result = contentAsString(resultM)

        Seq(44, 34, 50).map { x =>
          result must not contain(s"'$x'")
          result must contain(x.toString)
        }
      }
    }
  }

}