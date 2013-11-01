package models

import java.util.concurrent.TimeoutException

import scala.util.Failure
import scala.concurrent.duration._

import play.api.test._
import play.api.mvc.Results._
import play.api.libs.iteratee.{Iteratee, Enumeratee, Enumerator}
import play.api.test.FakeApplication
import play.api.mvc.{Handler, Action}
import play.api.libs.ws.SignatureCalculator
import play.api.libs.ws.WS.WSRequest

import helpers.TweetSamples._


class TwitterImplSpec extends PlaySpecification {

  trait TestSignatureComponent extends TwitterSignatureComponent {
    val signature: SignatureCalculator = new SignatureCalculator {
      def sign(request: WSRequest): Unit = ()
    }
  }

  trait TestUrlComponent extends TwitterUrlComponent {
    def tweetUrlFromId(id: Long): String = s"http://localhost:$testServerPort/$id"
    def statusStreamUrl: String = "http://localhost:" + testServerPort
  }

  class TestTwitter extends TwitterImpl with TestUrlComponent with TwitterTimeoutComponent with TestSignatureComponent {
    def reconnectTimeout: FiniteDuration = 1.hour
    def tweetFetchTimeout: FiniteDuration = 5.seconds
  }



  "the statusStream enumerator" should {
    val webService = FakeApplication(withRoutes = {
      case ("GET", "/") => Action {
        Ok.chunked(Enumerator(tweetJsonString, tweetWithRetweetJsonString, tweetWithReplyJsonString, tweetWithRetweetAndReplyJsonString)).as("application/json")
      }
    })

    "fetch tweets from a web service" in new WithServer(webService) {
      val twitter: Twitter = new TestTwitter
      val i = Enumeratee.take(4).transform(Iteratee.getChunks[Tweet])
      val streamedTweets = await(twitter.statusStream.run(i))

      streamedTweets === List(tweet, tweetWithRetweet, tweetWithReply, tweetWithRetweetAndReply)
    }

    val firstRequestIgnoringWebService = FakeApplication(withRoutes = new PartialFunction[(String, String), Handler] {
      private var requestCount = 0

      def apply(x: (String, String)): Handler = x match {
        case ("GET", "/") => Action {
          requestCount = requestCount + 1

          if (requestCount <= 1) {
            Ok.chunked(Enumerator.empty[String]).as("application/json")
          } else {
            Ok.chunked(Enumerator(tweetJsonString, tweetWithRetweetJsonString, tweetWithReplyJsonString, tweetWithRetweetAndReplyJsonString)).as("application/json")
          }
        }
      }

      def isDefinedAt(x: (String, String)): Boolean = x._1 == "GET" && x._2 == "/"
    })

    "reconnect to the web service after not receiving any messages for a given time period" in new WithServer(firstRequestIgnoringWebService) {
      val twitter: Twitter = new TestTwitter {
        override val reconnectTimeout = 1.second
      }

      val i = Enumeratee.take(4).transform(Iteratee.getChunks[Tweet])
      val streamedTweets = await(twitter.statusStream.run(i))

      streamedTweets === List(tweet, tweetWithRetweet, tweetWithReply, tweetWithRetweetAndReply)
    }


    var requestCount = 0
    val requestCountingWebService = FakeApplication(withRoutes = {
      case ("GET", "/") => Action {
        requestCount += 1
        Ok.chunked(Enumerator.empty[String]).as("application/json")
      }
    })

    "only connect once" in new WithServer(requestCountingWebService) {
      val twitter = new TestTwitter
      twitter.statusStream
      twitter.statusStream
      requestCount === 1
    }
  }


  val tweetWebService = {
    val tweetUrl = "/" + tweet.id
    val tweetWithRetweetUrl = "/" + tweetWithRetweet.id
    val tweetWithReplyUrl = "/" + tweetWithReply.id

    FakeApplication(withRoutes = {
      case (GET, `tweetUrl`) => Action {
        Ok(tweetJsonString)
      }
      case (GET, `tweetWithRetweetUrl`) => Action {
        Ok(tweetWithRetweetJsonString)
      }
      case (GET, `tweetWithReplyUrl`) => Action {
        Ok(tweetWithReplyJsonString)
      }
    })
  }

  "fetchTweet" should {
    "load a single tweet from the web service" in new WithServer(tweetWebService) {
      await((new TestTwitter).fetchTweet(tweet.id)) === tweet
    }

    val tweetUrl = "/" + tweet.id
    val slowWebService = FakeApplication(withRoutes = {
      case (GET, `tweetUrl`) => Action {
        Thread.sleep(1000)
        Ok(tweetJsonString)
      }
    })

    "cache fetched tweets" in new WithServer(slowWebService) {
      val twitter = new TestTwitter
      await(twitter.fetchTweet(tweet.id), 500) must throwA[TimeoutException]
      await(twitter.fetchTweet(tweet.id)) // let it cache
      await(twitter.fetchTweet(tweet.id), 500) === tweet
    }

    "time out after a specified duration" in new WithServer(slowWebService) {
      val twitter = new TestTwitter {
        override val tweetFetchTimeout = 500.millis
      }

      val f = twitter.fetchTweet(tweet.id)
      Thread.sleep(500) // don't use await/Await.{ready,result) because they throw on Future.failed
      f.value must beAnInstanceOf[Some[Failure[TimeoutException]]]
    }


    val invalidFormatWebService = FakeApplication(withRoutes = {
      case (GET, _) => Action(Ok("invalid format"))
    })

    "fail when it receives tweets in an unknown format" in new WithServer(invalidFormatWebService) {
      val twitter = new TestTwitter
      await(twitter.fetchTweet(1)) must throwA[InvalidTweetFormatException]
    }
  }

  "fetchTweets" should {
    "load multiple tweets from the web service" in new WithServer(tweetWebService) {
      val actual = await((new TestTwitter).fetchTweets(Seq(tweet.id, tweetWithRetweet.id, tweetWithReply.id)))
      val expected = Seq(tweet, tweetWithRetweet, tweetWithReply)
      actual === expected
    }
  }
}
