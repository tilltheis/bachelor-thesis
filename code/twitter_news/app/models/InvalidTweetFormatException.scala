package models


case class InvalidTweetFormatException(tweet: String) extends RuntimeException
