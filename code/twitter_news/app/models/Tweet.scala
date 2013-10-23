package models

import org.joda.time.DateTime


sealed case class Tweet(id: Long,
                        text: String,
                        userId: Long,
                        userName: String,
                        userScreenName: String,
                        avatarUrl: String,
                        date: DateTime,
                        retweetOfTweet: Option[Tweet],
                        replyToTweetId: Option[Long]) {

  // avatarUrl may change but all that matters is the id
  override def equals(o: Any) = o match {
    case tweet: Tweet => tweet.id == id
    case _ => false
  }

  override val hashCode = id.hashCode
}
