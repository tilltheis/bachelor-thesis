package models

import java.util.Locale

import scala.util.control.Exception.catching

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsString
import play.api.data.validation.ValidationError
import play.api.libs.json.{Json => Js}


object Json {

  def tweetOccurenceMapToJson(jsonOccurenceKey: String)(map: Map[Tweet, Int]): JsValue = {
    import Implicits.tweetWrites
    Js.toJson(map.toSeq.sortBy(-_._2).map { case (tweet, occurenceCount) =>
      Js.obj("tweet" -> tweet, jsonOccurenceKey -> occurenceCount)
    })
  }

  
  // json reads and writes for tweets (see https://dev.twitter.com/docs/platform-objects/tweets)
  object Implicits {
    // we cannot use play.api.libs.json.Reads.jodaDateReads because we need the english locale
    implicit object DateTimeReads extends Reads[DateTime] {
      private val error = JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.datetime"))))
      private val dateTimePattern = "EEE MMM dd HH:mm:ss Z yyyy"
  
      def reads(json: JsValue): JsResult[DateTime] = json match {
        case JsString(s) => {
          val dateTimeOption = catching(classOf[IllegalArgumentException]).opt {
            DateTimeFormat.forPattern(dateTimePattern).withLocale(Locale.ENGLISH).parseDateTime(s)
          }
          dateTimeOption.map(JsSuccess(_)).getOrElse(error)
        }
        case _ => error
      }
    }
  
  
    // reads json messages from https://dev.twitter.com/docs/api/1.1/post/statuses/filter
    implicit val tweetReads: Reads[Tweet] = (
      (__ \ "id").read[Long] and
      (__ \ "text").read[String] and
      (__ \ "user" \ "id").read[Long] and
      (__ \ "user" \ "name").read[String] and
      (__ \ "user" \ "screen_name").read[String] and
      (__ \ "user" \ "profile_image_url").read[String] and
      (__ \ "created_at").read[DateTime] and
      (__ \ "retweeted_status").lazyReadNullable(tweetReads) and // recursive! throws NullPointerException if not lazy
      (__ \ "in_reply_to_status_id").readNullable[Long]
    )(Tweet)
  
    // don't auto generate Writes instance because we need to transform some values
    implicit val tweetWrites: Writes[Tweet] = Writes {
      case Tweet(id, text, userId, userName, userScreenName, avatarUrl, date, retweetOfTweet, replyToTweetId) => {
        val json = Js.obj(
          "id" -> id.toString,
          "text" -> text,
          "userId" -> userId.toString,
          "userName" -> userName,
          "userScreenName" -> userScreenName,
          "avatarUrl" -> avatarUrl,
          "date" -> date.getMillis
        )
        val retweetJson = retweetOfTweet.map(tweet => Js.obj("retweetOfTweet" -> tweetWrites.writes(tweet))).getOrElse(Js.obj())
        val replyJson = replyToTweetId.map(id => Js.obj("replyToTweetId" -> id.toString)).getOrElse(Js.obj())
        json ++ retweetJson ++ replyJson
      }
    }
  }
}
