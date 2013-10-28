package models

import scala.concurrent.duration._

import org.joda.time.{Duration => JodaDuration}

import play.api.Play
import play.api.Play.current
import scala.io.{Codec, Source}
import java.util.Locale


object Config {
  val relevantDuration: JodaDuration = readDuration("twitter.relevant_duration")

  val mostTweetedUpdateInterval: JodaDuration =
    readDuration("twitter.most_tweeted_update_interval")
  val mostRetweetedUpdateInterval: JodaDuration =
    readDuration("twitter.most_retweeted_update_interval")
  val mostDiscussedUpdateInterval: JodaDuration =
    readDuration("twitter.most_discussed_update_interval")


  val reconnectTimeout: FiniteDuration =
    Play.configuration.getMilliseconds("twitter.reconnect_timeout").get.millis
  val tweetFetchTimeout: FiniteDuration =
    Play.configuration.getMilliseconds("twitter.tweet_fetch_timeout").get.millis


  val consumerKey: String =
    Play.configuration.getString("twitter.consumer_key").get
  val consumerKeySecret: String =
    Play.configuration.getString("twitter.consumer_key_secret").get
  val accessToken: String =
    Play.configuration.getString("twitter.access_token").get
  val accessTokenSecret: String =
    Play.configuration.getString("twitter.access_token_secret").get


  val mostTweetedLimit = Play.configuration.getInt("twitter.most_tweeted_limit").get
  val mostRetweetedLimit = Play.configuration.getInt("twitter.most_retweeted_limit").get
  val mostDiscussedLimit = Play.configuration.getInt("twitter.most_discussed_limit").get


  val ignoredWords: Seq[String] =
    Source
      .fromFile(Play.getFile("conf/ignored-words.txt"))(Codec.UTF8)
      .getLines()
      .filterNot(w => w.isEmpty || w.startsWith("//"))
      .map(_.toLowerCase(Locale.ENGLISH)).toSeq


  private def readDuration(key: String): JodaDuration = {
    val ms = Play.configuration.getMilliseconds(key).get
    JodaDuration.millis(ms)
  }
}
