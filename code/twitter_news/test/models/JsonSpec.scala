package models

import org.joda.time.DateTime

import play.api.test.PlaySpecification
import play.api.libs.json.{Json => Js, JsValue, JsSuccess}

import Json.tweetOccurenceMapToJson
import Json.Implicits._
import helpers.TweetSamples._


class JsonSpec extends PlaySpecification {
  "tweetOccurenceMapToJson" should {
    "convert Map[Tweet, Int] to JsValue sorted by most occurences first" in {
      val map = Map(tweet -> 1, tweetWithReply -> 2)
      val json: JsValue = Js.arr(
        Js.obj("tweet" -> Js.toJson(tweetWithReply), "replyCount" -> 2),
        Js.obj("tweet" -> Js.toJson(tweet), "replyCount" -> 1)
      )
      tweetOccurenceMapToJson("replyCount")(map) === json
    }
  }

  "Implicits" should {
    "provide a correct Reads instance for joda.time.DateTime" in {
      Js.parse("\"Wed Oct 16 14:14:52 +0000 2013\"").validate[DateTime] === JsSuccess(stringToDateTime("Wed Oct 16 14:14:52 +0000 2013"))
    }

    "provide a correct Reads instance for the Tweet model" in {
      Js.parse(tweetJsonString).validate[Tweet] === JsSuccess(tweet)
      Js.parse(tweetWithRetweetJsonString).validate[Tweet] === JsSuccess(tweetWithRetweet)
      Js.parse(tweetWithReplyJsonString).validate[Tweet] === JsSuccess(tweetWithReply)
      Js.parse(tweetWithRetweetAndReplyJsonString).validate[Tweet] === JsSuccess(tweetWithRetweetAndReply)
    }

    "provide a correct Writes instance for the Tweet model" in {
      Js.toJson(tweet) === tweetJson
      Js.toJson(tweetWithRetweet) === tweetWithRetweetJson
      Js.toJson(tweetWithReply) === tweetWithReplyJson
      Js.toJson(tweetWithRetweetAndReply) === tweetWithRetweetAndReplyJson
    }
  }
}
