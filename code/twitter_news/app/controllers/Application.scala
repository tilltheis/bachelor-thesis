package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.{Duration => JodaDuration}

import play.api.mvc._
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.libs.iteratee.Enumerator
import play.api.Play
import play.api.Play.current

import models.{SleepingEnumeratee, TwitterNews}
import models.Json.tweetOccurenceMapToJson
import models.Json.Implicits._


// tweet format: https://dev.twitter.com/docs/platform-objects/tweets
// tweet display requirements: https://dev.twitter.com/terms/display-requirements
// stream api: https://dev.twitter.com/docs/api/1.1/post/statuses/filter
// http-post method problem: http://stackoverflow.com/questions/11166356/sending-post-request-to-twitter-api-with-play-framework-2-0

// use cake pattern to make controller testable
object Application extends Controller {
  private val relevantDuration = readDuration("twitter.relevant_duration")
  private val twitterNews = TwitterNews(relevantDuration, 150)

  private val mostTweetedUpdateInterval =
    readDuration("twitter.most_tweeted_update_interval")
  private val mostRetweetedUpdateInterval =
    readDuration("twitter.most_retweeted_update_interval")
  private val mostDiscussedUpdateInterval =
    readDuration("twitter.most_discussed_update_interval")


  def index = Action {
    Ok(views.html.index(twitterNews.mostTweeted,
                        twitterNews.mostRetweeted,
                        twitterNews.mostDiscussed))
  }

  def mostTweetedEventSource =
    jsonEventSourceHandler(throttle(twitterNews.mostTweetedEnumerator, mostTweetedUpdateInterval))

  def mostRetweetedEventSource = {
    val e = throttle(twitterNews.mostRetweetedEnumerator, mostRetweetedUpdateInterval)
    jsonEventSourceHandler(e.map(tweetOccurenceMapToJson("retweetCount")))
  }

  def mostDiscussedEventSource = {
    val e = throttle(twitterNews.mostDiscussedEnumerator, mostDiscussedUpdateInterval)
    jsonEventSourceHandler(e.map(tweetOccurenceMapToJson("replyCount")))
  }


  private def readDuration(key: String): JodaDuration = {
    val ms = Play.configuration.getMilliseconds(key).get
    JodaDuration.millis(ms)
  }


  private def jsonEventSourceHandler[A](e: Enumerator[A])(implicit ev: Writes[A]) = Action {
    Ok.chunked(e.map(Json.toJson(_)).through(EventSource())).as("text/event-stream")
  }

  private def throttle[E](e: Enumerator[E], d: JodaDuration): Enumerator[E] =
    e.through(SleepingEnumeratee(d))

}