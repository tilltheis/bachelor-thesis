package examples.real_time_web

import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.{Comet, EventSource}

object Controller extends Controller {

  def weirdEventSource = Action {
    Ok.chunked(Enumerator("44", "34", "50").through(EventSource()))
      .as("text/event-stream")
  }

  def correctEventSource = Action {
    implicit val stringMessage = Comet.CometMessage[String](identity)
    Ok.chunked(Enumerator("44", "34", "50").through(EventSource()))
      .as("text/event-stream")
  }

}