package models

import java.util.Locale

import scala.io.{Codec, Source}
import scala.util.{Success, Try}
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.{Duration => JodaDuration}

import play.api.libs.iteratee.{Concurrent, Enumeratee, Enumerator}
import play.api.Play
import play.api.Play.current


object TwitterNews {
  private lazy val ignoredWords: Seq[String] =
    Source.fromFile(Play.getFile("conf/ignored-words.txt"))(Codec.UTF8)
          .getLines().filterNot(_.isEmpty).map(_.toLowerCase(Locale.ENGLISH)).toSeq

  def apply(relevantDuration: JodaDuration,
            mostTweetedLimit: Int = 10,
            mostRetweetedLimit: Int = 10,
            mostDiscussedLimit: Int = 10): TwitterNews =
    new TwitterNews(TwitterImpl,
                    relevantDuration,
                    mostTweetedLimit,
                    mostRetweetedLimit,
                    mostDiscussedLimit)
}

class TwitterNews(val twitter: Twitter,
                  val relevantDuration: JodaDuration,
                  val mostTweetedLimit: Int,
                  val mostRetweetedLimit: Int,
                  val mostDiscussedLimit: Int) {

  val tweetEnumerator: Enumerator[Tweet] = twitter.statusStream

  // old tweets to the left, new tweets to the right
  private var relevantTweets: Vector[Tweet] = Vector.empty

  // these vars are accessed inside of different iteratees/enumerators/enumeratees
  // better make them volatile to make sure changes are immediately visible
  @volatile private var mostDiscussedIds: Seq[Long] = Seq.empty

  @volatile private var _mostTweeted: Map[String, Int] = Map.empty
  @volatile private var _mostRetweeted: Seq[Tweet] = Seq.empty
  @volatile private var _mostDiscussed: Seq[Tweet] = Seq.empty

  def mostTweeted: Map[String, Int] = _mostTweeted
  private def mostTweeted_=(value: Map[String, Int]): Unit = _mostTweeted = value

  def mostRetweeted: Seq[Tweet] = _mostRetweeted
  private def mostRetweeted_=(value: Seq[Tweet]): Unit = _mostRetweeted = value

  def mostDiscussed: Seq[Tweet] = _mostDiscussed
  private def mostDiscussed_=(value: Seq[Tweet]): Unit = _mostDiscussed = value


  private val newsEnumerator: Enumerator[(Map[String, Int], Seq[Tweet], Seq[Long])] = {
    def isOutdated(tweet: Tweet): Boolean =
      tweet.date.withDurationAdded(relevantDuration, 1).isBeforeNow

    tweetEnumerator.map { tweet =>
      // clean up old tweets and add new one in one step
      val newIrrelevantTweets = relevantTweets.takeWhile(isOutdated)
      relevantTweets = relevantTweets.dropWhile(isOutdated)
      if (!isOutdated(tweet)) {
        relevantTweets = relevantTweets :+ tweet
      }

      mostTweeted = updatedMostTweeted(mostTweeted, newIrrelevantTweets, tweet)
      mostRetweeted = updatedMostRetweeted
      mostDiscussedIds = updatedMostDiscussedIds
      // mostDiscussed will be updated by mostDiscussedEnumerator

      (mostTweeted, mostRetweeted, mostDiscussedIds)
    }
  }


  val mostTweetedEnumerator: Enumerator[Map[String, Int]] = newsEnumerator.map(_._1)
  val mostRetweetedEnumerator: Enumerator[Seq[Tweet]] = newsEnumerator.map(_._2)
  val mostDiscussedIdsEnumerator: Enumerator[Seq[Long]] = newsEnumerator.map(_._3)


  // translate tweet ids to real tweets by fetching tweets from twitter
  // times out after <tweetFetchingTimeout> and continues with the next list of ids
  // it will buffer one set of incoming ids and will skip old ids if fetching takes too long
  val tweetFetchingTimeout: Duration = 10.seconds
  val mostDiscussedEnumerator: Enumerator[Seq[Tweet]] =
    mostDiscussedIdsEnumerator.through(
      Concurrent.buffer(1).compose(
        Enumeratee.map[Seq[Long]] { ids =>
          Try(Await.result(twitter.fetchTweets(ids), tweetFetchingTimeout))
        }
      ).compose(
        Enumeratee.collect {
          case Success(tweets) =>
            mostDiscussed = tweets
            tweets
        }
      )
    )

//  the following doesn't work because the updateMostDiscussedEnumeratee at the end will make the whole thing do nothing (don't know why)
//    mostDiscussedIdsEnumerator.through(discussionTweetFetchingEnumeratee(tweetFetchingTimeout)) // would return Seq[Tweet]
//                              .through(updateMostDiscussedEnumeratee)                           // would update mostDiscussed-var



  private def updatedMostTweeted(mostTweeted: Map[String, Int],
                                 newIrrelevantTweets: Seq[Tweet],
                                 newRelevantTweet: Tweet): Map[String, Int] = {
    def isAllowedWord(word: String): Boolean =
      word.length > 1 &&
      !TwitterNews.ignoredWords.contains(word.toLowerCase(Locale.ENGLISH)) &&
      !word.startsWith("http://") &&
      !word.startsWith("@") &&
      word.startsWith("#")

    val withoutIrrelevantTweets = newIrrelevantTweets.foldLeft(mostTweeted) { case (map, tweet) =>
      tweet.text.split(' ').filter(isAllowedWord).foldLeft(map) { case (map, word) =>
        val oldCount = map.getOrElse(word, 1)
        if (oldCount == 1) {
          map - word
        } else {
          map.updated(word, oldCount - 1)
        }
      }
    }

    val newRelevantWords = newRelevantTweet.text.split(' ').filter(isAllowedWord)

    val withNewRelevantTweet = newRelevantWords.foldLeft(withoutIrrelevantTweets) { (map, word) =>
      map.updated(word, map.getOrElse(word, 0) + 1)
    }

    withNewRelevantTweet.toSeq.sortBy { case (word, count) => -count }.take(mostTweetedLimit).toMap
  }


  private def updatedMostRetweeted: Seq[Tweet] = {
    val map = relevantTweets.foldLeft(Map.empty[Tweet, Int]) { case (map, tweet) =>
      tweet.retweetOfTweet.map { originalTweet =>
        map.updated(originalTweet, map.getOrElse(originalTweet, 0) + 1)
      }.getOrElse(map)
    }

    map.toSeq.sortBy { case (id, count) => -count }.take(mostRetweetedLimit).map(_._1)
  }


  private def updatedMostDiscussedIds: Seq[Long] = {
    val map = relevantTweets.foldLeft(Map.empty[Long, Int]) { case (map, tweet) =>
      tweet.replyToTweetId.map { discussedTweetId =>
        map.updated(discussedTweetId, map.getOrElse(discussedTweetId, 0) + 1)
      }.getOrElse(map)
    }

    map.toSeq.sortBy { case (id, count) => -count }.take(mostDiscussedLimit).map(_._1)
  }
}
