package examples.models

object AgeStatistics {
  def apply(statistics: (Int, Int)*): AgeStatistics =
    empty ++ Map(statistics: _*)

  val empty: AgeStatistics = Map.empty.withDefaultValue(0)

  val sample: AgeStatistics =
    apply(  6 -> 1
         , 10 -> 2
         , 16 -> 5
         , 20 -> 6
         , 28 -> 1
         , 30 -> 4
         , 42 -> 9
         , 51 -> 3
         , 64 -> 1
         )
}