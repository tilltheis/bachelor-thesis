package examples.real_time_web

import play.api.test._

class ControllerSpec extends PlaySpecification {

  "the weirdEventSource-Action" should {
    "stream numbers within single quotes" in new WithApplication {
      val resultM = Controller.weirdEventSource.apply(FakeRequest())
      val result = contentAsString(resultM)

      Seq(44, 34, 50).foreach { x =>
        result must contain(s"'$x'")
      }
    }
  }

  "the correctEventSource-Action" should {
    "stream numbers without single quotes" in new WithApplication {
      val resultM = Controller.correctEventSource.apply(FakeRequest())
      val result = contentAsString(resultM)

      Seq(44, 34, 50).foreach { x =>
        result must contain(x.toString)
      }
    }
  }

}