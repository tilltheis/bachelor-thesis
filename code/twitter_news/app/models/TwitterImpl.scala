package models

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.collection.mutable

import akka.actor.Cancellable

import play.api.{Logger, Play}
import play.api.Play.current
import play.api.libs.iteratee.{Enumeratee, Concurrent, Enumerator, Iteratee}
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.libs.ws.{SignatureCalculator, WS}
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}

import JsonImplicits._


// cake pattern for testing
//object Twitter extends Twitter with RealTwitterUrlComponent with DefaultTwitterTimeoutComponent
object TwitterImpl extends TwitterImpl with TwitterUrlComponent with TwitterTimeoutComponent with TwitterSignatureComponent {
  // news accounts from http://memeburn.com/2010/09/the-100-most-influential-news-media-twitter-accounts/
  private val userNamesWithIds = Seq(("mashable",972651L),("cnnbrk",428333L),("big_picture",18735898L),("theonion",14075928L),("time",14293310L),("breakingnews",6017542L),("bbcbreaking",5402612L),("espn",2557521L),("harvardbiz",14800270L),("gizmodo",2890961L),("techcrunch",816653L),("wired",1344951L),("wsj",3108351L),("smashingmag",15736190L),("pitchforkmedia",14089195L),("rollingstone",14780915L),("whitehouse",30313925L),("cnn",759251L),("tweetmeme",11883132L),("peoplemag",25589776L),("natgeosociety",300974581L),("nytimes",807095L),("lifehacker",7144422L),("foxnews",1367531L),("waitwait",13784322L),("newsweek",2884771L),("huffingtonpost",14511951L),("newscientist",19658826L),("mental_floss",20065936L),("theeconomist",5988062L),("emarketer",21217761L),("engadget",14372486L),("cracked",12513472L),("slate",15164565L),("bbcclick",7400702L),("fastcompany",2735591L),("reuters",1652541L),("incmagazine",476158046L),("eonline",2883841L),("rww",4641021L),("gdgt",15738725L),("instyle",14934818L),("mckquarterly",15308469L),("enews",22179997L),("nprnews",5392522L),("usatoday",15754281L),("mtv",2367911L),("freakonomics",14514804L),("boingboing",5971922L),("billboarddotcom",372355166L),("empiremagazine",3646911L),("todayshow",7744592L),("good",19621110L),("gawker",8936082L),("msnbc_breaking",11857072L),("cbsnews",15012486L),("guardiantech",7905122L),("usweekly",20012204L),("life",18665800L),("sciam",14647570L),("pastemagazine",6604072L),("drudge_report",14669951L),("parisreview",71256932L),("latimes",16664681L),("telegraphnews",14138785L),("abc7",16374678L),("arstechnica",717313L),("cnnmoney",16184358L),("nprpolitics",5741722L),("nytimesphoto",22411875L),("nybooks",11178902L),("nielsenwire",1253232744L),("io9",13215132L),("sciencechannel",16895274L),("usabreakingnews",15716039L),("vanityfairmag",199379744L),("cw_network",22083910L),("bbcworld",742143L),("abc",28785486L),("themoment",583138771L),("socialmedia2day",15441074L),("slashdot",1068831L),("washingtonpost",2467791L),("tpmmedia",273630640L),("msnbc",2836421L),("wnycradiolab",493265691L),("cnnlive",9245812L),("davos",5120691L),("planetmoney",15905103L),("cnetnews",819800L),("politico",9300262L),("tvnewser",14245378L),("guardiannews",788524L),("yahoonews",7309052L),("seedmag",17374092L),("tvguide",11350892L),("travlandleisure",16211434L),("newyorkpost",16497528L),("discovermag",23962323L),("sciencenewsorg",19402238L))
  private val (_userNames, userIds) = userNamesWithIds.unzip

  def tweetUrlFromId(id: Long): String =
    s"https://api.twitter.com/1.1/statuses/show/$id.json"

  val statusStreamUrl: String =
    "https://stream.twitter.com/1.1/statuses/filter.json?follow=" + userIds.mkString(",")


  val timeout: FiniteDuration = 90.seconds


  // use get() because we cannot work without the values
  private val consumerKey = Play.configuration.getString("twitter.consumer_key").get
  private val consumerKeySecret = Play.configuration.getString("twitter.consumer_key_secret").get
  private val accessToken = Play.configuration.getString("twitter.access_token").get
  private val accessTokenSecret = Play.configuration.getString("twitter.access_token_secret").get

  val signature =
    OAuthCalculator(ConsumerKey(consumerKey, consumerKeySecret),
                    RequestToken(accessToken, accessTokenSecret))
}

trait TwitterUrlComponent {
  def tweetUrlFromId(id: Long): String
  def statusStreamUrl: String
}

trait TwitterTimeoutComponent {
  // the amount of time to wait for messages from twitter before reconnecting
  def timeout: FiniteDuration
}

trait TwitterSignatureComponent {
  def signature: SignatureCalculator
}


trait TwitterImpl extends Twitter { this: TwitterUrlComponent with TwitterTimeoutComponent with TwitterSignatureComponent =>

  // can't use play.api.cache.Cache for testing (https://groups.google.com/forum/#!topic/play-framework/PBIfeiwl5rU)
  // therefore roll out a new simple solution
  object TweetCache {
    // the vars must only be accessed via set()
    private val size = 1000
    private var cache: mutable.Map[Long, Tweet] = mutable.Map.empty
    private var insertionOrder: mutable.Queue[Long] = mutable.Queue.empty
  
    def get(id: Long): Option[Tweet] = cache.get(id)
    
    def set(tweet: Tweet): Unit = synchronized {
      cache.update(tweet.id, tweet)
      insertionOrder.enqueue(tweet.id)
  
      if (cache.size > size) {
        val keysToRemove = insertionOrder.take(cache.size - size)
        insertionOrder = insertionOrder.drop(cache.size - size)
        cache --= keysToRemove
      }
    }
  }


  def fetchTweet(id: Long): Future[Tweet] =
    TweetCache.get(id).map(Future.successful).getOrElse {
      WS.url(tweetUrlFromId(id)).sign(signature).get().flatMap { response =>
        response.json.validate[Tweet].fold(
          invalid = _ => Future.failed(new RuntimeException("invalid tweet format")),
          valid = tweet => {
            TweetCache.set(tweet)
            Future.successful(tweet)
          }
        )
      }
    }


  def statusStream: Enumerator[Tweet] = {
    plainStatusStream.map(byteArrayToTweet).through(Enumeratee.collect {
      case Some(tweet) => tweet // with replies/retweets/...
      //      case Some(tweet) if userIds.contains(tweet.userId) => tweet // without replies/reetweets/...
    })
  }


  private def plainStatusStream: Enumerator[Array[Byte]] = {
    // the data stram will sometimes stop generating elements
    // therefore we need to be able to reconnection after timeout
    def connect(i: Iteratee[Array[Byte], Unit]) =
      WS.url(statusStreamUrl).sign(signature).get { headers =>
        Logger.info(s"connection response headers: $headers")
        i
      }

    // use vars instead of vals because the callbacks that use them must be defined before the var-values are known
    @volatile var iterateeM: Option[Iteratee[Array[Byte], Unit]] = None
    @volatile var reconnectTaskM: Option[Cancellable] =
      Some(Akka.system.scheduler.scheduleOnce(timeout)(iterateeM.foreach(connect)))

    val (enumerator, channel) = Concurrent.broadcast[Array[Byte]]
    val timeOutResettingT = Enumeratee.mapInput[Array[Byte]] { in =>
      reconnectTaskM.foreach(_.cancel())
      reconnectTaskM = Some(Akka.system.scheduler.scheduleOnce(timeout)(iterateeM.foreach(connect)))
      in
    }
    val channelPushingI = Iteratee.foreach[Array[Byte]](channel.push)
    val iteratee = timeOutResettingT.transform[Unit](channelPushingI)

    iterateeM = Some(iteratee)

    connect(iteratee) // run

    enumerator
  }

  private def byteArrayToTweet(bytes: Array[Byte]): Option[Tweet] = {
    // some invalid tweets are expected. e.g. tweet deletion messages
    val tweetM = Json.parse(bytes).validate[Tweet].asOpt
    if (tweetM.isEmpty) {
      val s = new String(bytes, "UTF-8")
      Logger.info(s"invalid tweet format: $s")
    }
    tweetM
  }
}
