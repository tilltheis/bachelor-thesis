package examples.reactive.streams

import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise

import scala.concurrent.{Promise => _, _}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.language.reflectiveCalls
import scala.language.postfixOps
import scala.Some

object Composition {
  object Iteratees {
    object Sequential {

      val i1: Iteratee[Int, Option[Int]] = Iteratee.head
      val i2: Iteratee[Int, Int] = Iteratee.fold(0)(_ + _)

      // val i12: Iteratee[Int, (Option[Int], Int)] = for {
      //   res1 <- i1
      //   res2 <- i2
      // } yield (res1, res2)

      val i12: Iteratee[Int, (Option[Int], Int)] =
        i1.flatMap(res1 => i2.map(res2 => (res1, res2)))

      val e: Enumerator[Int] = Enumerator(1, 4, -2)
      val result: Future[(Option[Int], Int)] = e.run(i12)
      // result hat den Wert Future((Some(1), 2))

    }

    object Parallel {

      val i1: Iteratee[Int, Option[Int]] = Iteratee.head
      val i2: Iteratee[Int, Int] = Iteratee.fold(0)(_ + _)

      val i12: Iteratee[Int, (Option[Int], Int)] =
        Enumeratee.zip(i1, i2)

      val e: Enumerator[Int] = Enumerator(1, 4, -2)
      val result: Future[(Option[Int], Int)] = e.run(i12)
      // result hat den Wert Future((Some(1), 3))

    }

  //   object OneSourceToOneSink {
  //     // val dropFirstIter: Iteratee[Int, List[Int]] = for {
  //     //   firstOption: Option[Int] <- Iteratee.head
  //     //   xs: List[Int] <- Enumeratee.drop(firstOption.get).transform(Iteratee.getChunks)
  //     // } yield xs

  //     val dropFirstIteratee: Iteratee[Int, List[Int]] =
  //       Iteratee.head.flatMap { firstOption =>
  //         Enumeratee.drop(firstOption.get).transform(Iteratee.getChunks)
  //       }
  //   }

  //   object ParallelOneSourceToSeveralSinks {
  //     val evenIteratee: Iteratee[Int, List[Int]] = Iteratee.getChunks
  //     val oddIteratee: Iteratee[Int, List[Int]] = Iteratee.getChunks
  //     val splittingIteratee: Iteratee[Int, (Iteratee[Int, List[Int]], Iteratee[Int, List[Int]])] =
  //       Iteratee.fold((evenIteratee, oddIteratee)) { case ((evenI, oddI), n) =>
  //         def feed(i: Iteratee[Int, List[Int]]) = Iteratee.flatten(i.feed(Input.El(n)))
  //         if (n % 2 == 0) (feed(evenI), oddI) else (evenI, feed(oddI))
  //       }
  //   }

     object ParallelManySourcesToOneSink {
       def headHeadIteratee[A, B]:
         Iteratee[A, Iteratee[B, (Option[A], Option[B])]] =
           Iteratee.head.map(a => Iteratee.head.map(b => (a, b)))

       // read first element from outer iteratee
       // read first element from inner iteratee
       // read second element from outer iteratee
       def firstFirstNthIteratee[A]: Iteratee[A, Iteratee[Int, (Option[A], Option[Int], Option[A])]] = {
         val firstAI: Iteratee[A, Option[A]] = Iteratee.head[A]
         val firstBI: Iteratee[Int, Option[Int]] = Iteratee.head[Int]

         val firstFirst: Iteratee[A, Iteratee[Int, (Option[A], Option[Int])]] =
           firstAI.map(a => firstBI.map(b => (a, b)))

         firstFirst.flatMap { innerI =>
           Done(innerI.map {
             case (ma, mb) => (ma, mb, None)
           })
         }

//         firstFirst.flatMap { i =>
//           i.flatMap {
//             case (ma, mb) => Done(Done((ma, mb, None: Option[A])))
//           }
//
////           i.flatMap {
//////             case (ma, None)    => Done(Done((ma, None: Option[Int], None: Option[A])))
////             case (ma, mb)    => Done(Done((ma: Option[A], mb: Option[Int], None: Option[A])))
//////             case (ma, mb@Some(b)) => {
//////               val t = Enumeratee.drop(b)
//////               val inner = Iteratee.head[A].map(mc => (ma, mb, mc))
//////               t.transform(inner)
//////             }
////           }
////           Iteratee.head[A].map { c =>
////             i.map { case (a, b) => (a, b, c) }
////           }
//         }
       }


       type Word = String
       type Line = String

       def lineWithNthWord(n: Int): Iteratee[Word, Iteratee[Line, Option[(Line, Word)]]] = {
         Enumeratee.drop(n).transform(Iteratee.head[Word]).map {
           case Some(word) =>
             val t = Enumeratee.dropWhile[Line](!_.startsWith(word))
             t.transform(Iteratee.head[Line]).map {
               case Some(line) => Some(line, word)
               case _ => None
             }
           case _ => Done(None)
         }
       }

  //     def rotatingSourceIteratee(timesPerSource: Int):
  //         Iteratee[Int, Iteratee[Int, List[Int]]] = {
  //       val t = Enumeratee.take[Int](timesPerSource)
  //       val i = t.transform(Iteratee.getChunks)

  //       val groupedT = Enumeratee.grouped[Int] {
  //         i.map(l1 => i.map(l2 => l1 ::: l2))
  //       }
  //       groupedT.transform(Iteratee.fold(Done[Int, List[Int]](Nil)) {
  //           (resultI, groupedI) =>
  //         for {
  //           result <- resultI
  //           group <- groupedI
  //         } yield result ::: group
  //       })
  //     }
     }
  }

  object Enumerators {
    object Sequential {

      val i: Iteratee[Int, List[Int]] = Iteratee.getChunks
      val e1 = Enumerator(1, 2)
      val e2 = Enumerator(3)
      val e12: Enumerator[Int] = e1.andThen(e2)

      val result: Future[List[Int]] = e12.run(i)
      // result hat den Wert Future(List(1, 2, 3))

    }

    object Parallel {


      def timeoutEnumerator[A](x: A, d: Duration): Enumerator[A] =
        Enumerator.flatten(Promise.timeout(Enumerator(x), d))

      val i: Iteratee[Int, List[Int]] = Iteratee.getChunks
      val e1 = timeoutEnumerator(1, 3 seconds)
      val e2 = timeoutEnumerator(2, 1 second)
      val e3 = timeoutEnumerator(3, 2 seconds)
      val e123: Enumerator[Int] = Enumerator.interleave(e1, e2, e3)

      val result: Future[List[Int]] = e123.run(i)
      // result hat den Wert Future(List(2, 3, 1))

    }

    // object ParallelManyToOne {
    //   val e1 = Enumerator(1, 1, 2)
    //   val e2 = Enumerator(2, 3, 2)
    //   val e3 = Enumerator(3, 2, 3)

    //   def enumeratorFromOutput(e1: Enumerator[Int], e2: Enumerator[Int], e3: Enumerator[Int], initialE: Enumerator[Int]): Enumerator[Int] = {
    //     initialE.flatMap {
    //       case 1 => enumeratorFromOutput(e1, e2, e3, e1)
    //       case 2 => enumeratorFromOutput(e1, e2, e3, e2)
    //       case 3 => enumeratorFromOutput(e1, e2, e3, e3)
    //     }
    //   }

    //   val i = Iteratee.getChunks[Int]
    //   val e = enumeratorFromOutput(e1, e2, e3, e1)
    //   val result = e.run(i)
    // }

  }
}