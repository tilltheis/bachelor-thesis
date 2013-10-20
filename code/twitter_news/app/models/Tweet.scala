package models

import org.joda.time.DateTime

case class Tweet(id: Long,
                 text: String,
                 userId: Long,
                 userName: String,
                 userScreenName: String,
                 avatarUrl: String,
                 date: DateTime,
                 retweetOfTweet: Option[Tweet],
                 replyToTweetId: Option[Long]) {

}
