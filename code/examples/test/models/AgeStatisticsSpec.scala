package examples.models

import org.specs2.mutable._

class AgeStatisticsSpec extends Specification {
  "empty" should {
    "create a map" in {
      val statistics = AgeStatistics.empty

      "that is empty" in {
        statistics must be empty
      }

      "with a default value of zero" in {
        statistics(3) === 0
      }
    }
  }

  "sample" should {
    "create a map with sample data" in {
      AgeStatistics.sample must not be empty
    }
  }

  "apply" should {
    "create a map" in {
      val statistics = AgeStatistics(20 -> 4, 30 -> 5, 45 -> 1)

      "with the supplied arguments" in {
        val map = Map(20 -> 4, 30 -> 5, 45 -> 1)
        statistics.toSeq must containTheSameElementsAs(map.toSeq)
      }

      "with a default value of zero" in {
        statistics(3) === 0
      }
    }
  }
}