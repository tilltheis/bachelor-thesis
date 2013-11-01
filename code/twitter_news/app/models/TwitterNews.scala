package models

import java.util.Locale
import java.util.concurrent.TimeoutException

import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.{Duration => JodaDuration}

import play.api.libs.iteratee.{Iteratee, Concurrent, Enumeratee, Enumerator}


object TwitterNews {
  def apply(relevantDuration: JodaDuration,
            mostTweetedLimit: Int = Config.mostTweetedLimit,
            mostRetweetedLimit: Int = Config.mostRetweetedLimit,
            mostDiscussedLimit: Int = Config.mostDiscussedLimit): TwitterNews =
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

  // old tweets to the left, new tweets to the right
  private var relevantTweets: Vector[Tweet] = Vector.empty

  // these vars are accessed inside of different iteratees/enumerators/enumeratees
  // better make them volatile to make sure changes are immediately visible
  @volatile private var _mostTweeted: Map[String, Int] = Map.empty
  @volatile private var _mostRetweeted: Map[Tweet, Int] = Map.empty
  @volatile private var _mostDiscussedIds: Map[Long, Int] = Map.empty
  @volatile private var _mostDiscussed: Map[Tweet, Int] = Map.empty

  def mostTweeted = _mostTweeted
  def mostRetweeted = _mostRetweeted
  def mostDiscussedIds = _mostDiscussedIds
  def mostDiscussed = _mostDiscussed


  private val (_mostTweetedEnumerator, _mostTweetedChannel) = Concurrent.broadcast[Map[String, Int]]
  private val (_mostRetweetedEnumerator, _mostRetweetedChannel) = Concurrent.broadcast[Map[Tweet, Int]]
  private val (_mostDiscussedIdsEnumerator, _mostDiscussedIdsChannel) = Concurrent.broadcast[Map[Long, Int]]

  val mostTweetedEnumerator = _mostTweetedEnumerator
  val mostRetweetedEnumerator = _mostRetweetedEnumerator
  val mostDiscussedIdsEnumerator = _mostDiscussedIdsEnumerator
  // mostDiscussedEnumerator is defined further down


  private def isOutdated(tweet: Tweet): Boolean =
    tweet.date.withDurationAdded(relevantDuration, 1).isBeforeNow

  twitter.statusStream(Iteratee.foreach[Tweet] { tweet =>
    // clean up old tweets and add new one in one step
    val newIrrelevantTweets = relevantTweets.takeWhile(isOutdated)
    relevantTweets = relevantTweets.dropWhile(isOutdated)
    if (!isOutdated(tweet)) {
      relevantTweets = relevantTweets :+ tweet
    }

    _mostTweeted = updatedMostTweeted(mostTweeted, newIrrelevantTweets, tweet)
    _mostRetweeted = updatedMostRetweeted
    _mostDiscussedIds = updatedMostDiscussedIds
    // mostDiscussed will be updated by mostDiscussedEnumerator

    _mostTweetedChannel.push(mostTweeted)
    _mostRetweetedChannel.push(mostRetweeted)
    _mostDiscussedIdsChannel.push(mostDiscussedIds)
  })


  // translate tweet ids to real tweets by fetching tweets from twitter
  // times out after <tweetFetchingTimeout> and continues with the next list of ids
  // it will buffer one set of incoming ids and will skip old ids if fetching takes too long
  val mostDiscussedEnumerator: Enumerator[Map[Tweet, Int]] =
    mostDiscussedIdsEnumerator.through(
      Concurrent.buffer(1).compose(
        Enumeratee.mapM[Map[Long, Int]] { map =>
          val (ids, replyCounts) = map.unzip
          val tweetsM = twitter.fetchTweets(ids.toSeq)
          val mapM = tweetsM.map(_.zip(replyCounts).toMap)
          mapM.map(Some(_)).recover {
            case _: TimeoutException |
                 _: InvalidTweetFormatException => None
          }
        }
      ).compose(
        Enumeratee.collect {
          case Some(tweets) =>
            _mostDiscussed = tweets
            tweets
        }
      )
    )



  private def updatedMostTweeted(mostTweeted: Map[String, Int],
                                 newIrrelevantTweets: Seq[Tweet],
                                 newRelevantTweet: Tweet): Map[String, Int] = {

    // word should already be lower case
    def isAllowedWord(word: String) =
      word.length > 1 &&
      !Config.ignoredWords.contains(word)

    def isSpecialWord(word: String) =
      word.startsWith("http://") ||
      word.startsWith("@") ||
      word.startsWith("#") ||
      word == "&amp;"


    def relevantWords(text: String): Seq[String] = {
      val words = text.split("\\s+").map(_.toLowerCase(Locale.ENGLISH))
      val withoutSpecialWords = words.filterNot(isSpecialWord)
      val simpleLetterWords = withoutSpecialWords.map(_.dropWhile(!_.isLetterOrDigit).takeWhile(_.isLetterOrDigit))
      simpleLetterWords.filter(isAllowedWord)
    }

    val withoutIrrelevantTweets = newIrrelevantTweets.foldLeft(mostTweeted) { case (map, tweet) =>
      relevantWords(tweet.text).foldLeft(map) { case (map, word) =>
        val oldCount = map.getOrElse(word, 1)
        if (oldCount == 1) {
          map - word
        } else {
          map.updated(word, oldCount - 1)
        }
      }
    }

    val newRelevantWords = relevantWords(newRelevantTweet.text)

    val withNewRelevantTweet = newRelevantWords.foldLeft(withoutIrrelevantTweets) { (map, word) =>
      map.updated(word, map.getOrElse(word, 0) + 1)
    }

    withNewRelevantTweet.toSeq.sortBy { case (word, count) => -count }.take(mostTweetedLimit).toMap
  }


  private def updatedMostRetweeted: Map[Tweet, Int] = {
    val map = relevantTweets.foldLeft(Map.empty[Tweet, Int]) { case (map, tweet) =>
      tweet.retweetOfTweet.map { originalTweet =>
        map.updated(originalTweet, map.getOrElse(originalTweet, 0) + 1)
      }.getOrElse(map)
    }

    map.toSeq.sortBy { case (id, count) => -count }.take(mostRetweetedLimit).toMap
  }


  private def updatedMostDiscussedIds: Map[Long, Int] = {
    val map = relevantTweets.foldLeft(Map.empty[Long, Int]) { case (map, tweet) =>
      tweet.replyToTweetId.map { discussedTweetId =>
        map.updated(discussedTweetId, map.getOrElse(discussedTweetId, 0) + 1)
      }.getOrElse(map)
    }

    map.toSeq.sortBy { case (id, count) => -count }.take(mostDiscussedLimit).toMap
  }
}
