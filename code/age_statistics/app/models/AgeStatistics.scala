package models

object AgeStatistics {
  val emptyStatistics: AgeStatistics = Map.empty.withDefaultValue(0)

  val exampleStatistics: AgeStatistics =
    emptyStatistics ++ Map(  6 -> 1
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