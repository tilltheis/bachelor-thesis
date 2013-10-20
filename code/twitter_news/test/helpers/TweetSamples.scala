package helpers

import java.util.Locale

import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import models.Tweet

object TweetSamples {
  val tweet = Tweet(390480969875996673L, "tweet1", 1277548441L, "name1", "screenName1", "http://example.org/img1.png", stringToDateTime("Wed Oct 16 14:14:52 +0000 2013"), None, None)
  val tweetWithRetweet = Tweet(390480969875996674L, "tweet2", 1277548442L, "name2", "screenName2", "http://example.org/img2.png", stringToDateTime("Wed Oct 16 14:15:52 +0000 2013"), Some(tweet), None)
  val tweetWithReply = Tweet(390480969875996675L, "tweet3", 1277548443L, "name3", "screenName3", "http://example.org/img3.png", stringToDateTime("Wed Oct 16 14:16:52 +0000 2013"), None, Some(390480969875996673L))
  val tweetWithRetweetAndReply = Tweet(390480969875996676L, "tweet4", 1277548444L, "name4", "screenName4", "http://example.org/img4.png", stringToDateTime("Wed Oct 16 14:17:52 +0000 2013"), Some(tweet), Some(390480969875996673L))
  
  val tweetJsonString = "{\"created_at\":\"Wed Oct 16 14:14:52 +0000 2013\",\"id\":390480969875996673,\"text\":\"tweet1\",\"user\":{\"id\":1277548441,\"name\":\"name1\",\"screen_name\":\"screenName1\",\"profile_image_url\":\"http://example.org/img1.png\"}}"
  val tweetWithRetweetJsonString = "{\"created_at\":\"Wed Oct 16 14:15:52 +0000 2013\",\"id\":390480969875996674,\"text\":\"tweet2\",\"user\":{\"id\":1277548442,\"name\":\"name2\",\"screen_name\":\"screenName2\",\"profile_image_url\":\"http://example.org/img2.png\"},\"retweeted_status\":{\"created_at\":\"Wed Oct 16 14:14:52 +0000 2013\",\"id\":390480969875996673,\"text\":\"tweet1\",\"user\":{\"id\":1277548441,\"name\":\"name1\",\"screen_name\":\"screenName1\",\"profile_image_url\":\"http://example.org/img1.png\"}}}"
  val tweetWithReplyJsonString = "{\"created_at\":\"Wed Oct 16 14:16:52 +0000 2013\",\"id\":390480969875996675,\"text\":\"tweet3\",\"user\":{\"id\":1277548443,\"name\":\"name3\",\"screen_name\":\"screenName3\",\"profile_image_url\":\"http://example.org/img3.png\"},\"in_reply_to_status_id\":390480969875996673}"
  val tweetWithRetweetAndReplyJsonString = "{\"created_at\":\"Wed Oct 16 14:17:52 +0000 2013\",\"id\":390480969875996676,\"text\":\"tweet4\",\"user\":{\"id\":1277548444,\"name\":\"name4\",\"screen_name\":\"screenName4\",\"profile_image_url\":\"http://example.org/img4.png\"},\"retweeted_status\":{\"created_at\":\"Wed Oct 16 14:14:52 +0000 2013\",\"id\":390480969875996673,\"text\":\"tweet1\",\"user\":{\"id\":1277548441,\"name\":\"name1\",\"screen_name\":\"screenName1\",\"profile_image_url\":\"http://example.org/img1.png\"}},\"in_reply_to_status_id\":390480969875996673}"

  val tweetJson = Json.obj(
    "id" -> "390480969875996673",
    "text" -> "tweet1",
    "userId" -> "1277548441",
    "userName" -> "name1",
    "userScreenName" -> "screenName1",
    "avatarUrl" -> "http://example.org/img1.png",
    "date" -> stringToDateTime("Wed Oct 16 14:14:52 +0000 2013")
  )
  val tweetWithRetweetJson = Json.obj(
    "id" -> "390480969875996674",
    "text" -> "tweet2",
    "userId" -> "1277548442",
    "userName" -> "name2",
    "userScreenName" -> "screenName2",
    "avatarUrl" -> "http://example.org/img2.png",
    "date" -> stringToDateTime("Wed Oct 16 14:15:52 +0000 2013"),
    "retweetOfTweet" -> tweetJson
  )
  val tweetWithReplyJson = Json.obj(
    "id" -> "390480969875996675",
    "text" -> "tweet3",
    "userId" -> "1277548443",
    "userName" -> "name3",
    "userScreenName" -> "screenName3",
    "avatarUrl" -> "http://example.org/img3.png",
    "date" -> stringToDateTime("Wed Oct 16 14:16:52 +0000 2013"),
    "replyToTweetId" -> "390480969875996673"
  )
  val tweetWithRetweetAndReplyJson = Json.obj(
    "id" -> "390480969875996676",
    "text" -> "tweet4",
    "userId" -> "1277548444",
    "userName" -> "name4",
    "userScreenName" -> "screenName4",
    "avatarUrl" -> "http://example.org/img4.png",
    "date" -> stringToDateTime("Wed Oct 16 14:17:52 +0000 2013"),
    "retweetOfTweet" -> tweetJson,
    "replyToTweetId" -> "390480969875996673"
  )

  def stringToDateTime(s: String) =
    DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss Z yyyy")
                  .withLocale(Locale.ENGLISH)
                  .parseDateTime(s)
}
