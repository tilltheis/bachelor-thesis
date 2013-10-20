package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.{Duration => JodaDuration}

import play.api.mvc._
import play.api.libs.EventSource
import play.api.libs.json._

import models.TwitterNews
import models.JsonImplicits._
import play.api.libs.iteratee.Enumerator


// tweet format: https://dev.twitter.com/docs/platform-objects/tweets
// tweet display requirements: https://dev.twitter.com/terms/display-requirements
// stream api: https://dev.twitter.com/docs/api/1.1/post/statuses/filter
// http-post method problem: http://stackoverflow.com/questions/11166356/sending-post-request-to-twitter-api-with-play-framework-2-0

// use cake pattern to make controller testable
object Application extends Controller {
  private val twitterNews = TwitterNews(JodaDuration.standardHours(1))

  private val mostTweetedUpdateInterval = JodaDuration.standardSeconds(1)
  private val mostRetweetedUpdateInterval = JodaDuration.standardSeconds(5)
  private val mostDiscussedUpdateInterval = JodaDuration.standardSeconds(10)


  def index = Action {
    Ok(views.html.index(twitterNews.mostTweeted,
                        twitterNews.mostRetweeted,
                        twitterNews.mostDiscussed))
  }

  def mostTweetedEventSource =
    jsonEventSourceHandler(twitterNews.throttledMostTweetedEnumerator(mostTweetedUpdateInterval))

  def mostRetweetedEventSource =
    jsonEventSourceHandler(twitterNews.throttledMostRetweetedEnumerator(mostRetweetedUpdateInterval))

  def mostDiscussedEventSource =
    jsonEventSourceHandler(twitterNews.throttledMostDiscussedEnumerator(mostDiscussedUpdateInterval))


  private def jsonEventSourceHandler[A](e: Enumerator[A])(implicit ev: Writes[A]) = Action {
    Ok.chunked(e.map(Json.toJson(_)).through(EventSource())).as("text/event-stream")
  }

}