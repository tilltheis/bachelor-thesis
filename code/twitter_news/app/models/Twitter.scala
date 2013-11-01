package models

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.iteratee.Enumerator


trait Twitter {
  // the future may fail with java.util.concurrent.TimeoutException or InvalidTweetFormatException
  def fetchTweet(id: Long): Future[Tweet]

  def fetchTweets(ids: Seq[Long]): Future[Seq[Tweet]] =
    Future.sequence(ids.map(fetchTweet))

  def statusStream: Enumerator[Tweet]
}
