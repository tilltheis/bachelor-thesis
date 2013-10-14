package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.{Comet, EventSource}
import play.api.libs.iteratee._

import models.AgeStatistics

object Application extends Controller {
  var ageStatistics = AgeStatistics.empty

  val (outEnumerator, outChannel) = Concurrent.broadcast[Int]

  val ageForm = Form("age" -> number(1, 99))

  def index = Action { implicit request =>
    Ok(views.html.index(ageStatistics))
  }

  def input = Action { implicit request =>
    ageForm.bindFromRequest.fold(
      invalidForm => BadRequest(invalidForm.errorsAsJson.toString),
      { age =>
        ageStatistics =
          ageStatistics.updated(age, ageStatistics(age) + 1)
        outChannel.push(age)
        Ok
      }
    )
  }

  implicit val intMessage = Comet.CometMessage[Int](_.toString)
  def eventSource = Action {
    Ok.chunked(outEnumerator.through(EventSource()))
      .as("text/event-stream")
  }
}