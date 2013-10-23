package models

import play.api.test.PlaySpecification

import helpers.TweetSamples._


class TweetSpec extends PlaySpecification {
  "equals" should {
    "not care about the avatar url" in {
      tweet === tweet.copy(avatarUrl = "foo")
    }
  }

  "hashCode" should {
    "not care about the avatar url" in {
      tweet.hashCode === tweet.copy(avatarUrl = "foo").hashCode
    }
  }
}
