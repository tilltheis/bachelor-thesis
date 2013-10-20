package models

import org.joda.time.DateTime

import play.api.test.PlaySpecification
import play.api.libs.json.{JsSuccess, Json}

import helpers.TweetSamples
import TweetSamples._

import JsonImplicits._

class JsonImplicitsSpec extends PlaySpecification {
  "JsonImplicits" should {
    "provide a correct Reads instance for joda.time.DateTime" in {
      Json.parse("\"Wed Oct 16 14:14:52 +0000 2013\"").validate[DateTime] === JsSuccess(stringToDateTime("Wed Oct 16 14:14:52 +0000 2013"))
    }

    "provide a correct Reads instance for the Tweet model" in {
      Json.parse(tweetJsonString).validate[Tweet] === JsSuccess(tweet)
      Json.parse(tweetWithRetweetJsonString).validate[Tweet] === JsSuccess(tweetWithRetweet)
      Json.parse(tweetWithReplyJsonString).validate[Tweet] === JsSuccess(tweetWithReply)
      Json.parse(tweetWithRetweetAndReplyJsonString).validate[Tweet] === JsSuccess(tweetWithRetweetAndReply)
    }

    "provide a correct Writes instance for the Tweet model" in {
      Json.toJson(tweet) === tweetJson
      Json.toJson(tweetWithRetweet) === tweetWithRetweetJson
      Json.toJson(tweetWithReply) === tweetWithReplyJson
      Json.toJson(tweetWithRetweetAndReply) === tweetWithRetweetAndReplyJson
    }
  }
}
