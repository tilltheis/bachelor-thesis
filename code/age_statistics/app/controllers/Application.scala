package controllers

import scala.util.control.Exception.catching

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.{Comet, EventSource}
import play.api.libs.iteratee._

import models.AgeStatistics


object Application extends Controller {

  // HTTP Communication

  var ageStatistics = AgeStatistics.empty

  val ageForm = Form("age" -> number(1, 99))

  def index = Action { implicit request =>
    Ok(views.html.index(ageStatistics))
  }

  def input = Action { implicit request =>
    ageForm.bindFromRequest.fold(
      invalidForm => BadRequest(invalidForm.errorsAsJson.toString),
      { age =>
        ageStatistics = ageStatistics.updated(age, ageStatistics(age) + 1)
        sseOutChannel.push(age) // only needed for server sent events
        Redirect(routes.Application.index)
      }
    )
  }


  // Web Socket Communication

  val (wsOutEnumerator, wsOutChannel) = Concurrent.broadcast[String]

  def socket = WebSocket.using[String] { request =>
    val in = Iteratee.foreach[String] { ageString =>
      type NFE = NumberFormatException
      catching(classOf[NFE]).opt(ageString.toInt).foreach { age =>
        if (age > 0 && age < 100) {
          ageStatistics =
            ageStatistics.updated(age, ageStatistics(age) + 1)
          wsOutChannel.push(age.toString)
        }
      }
    }

    (in, wsOutEnumerator)
  }


  // Server Sent Events Communication

  val (sseOutEnumerator, sseOutChannel) = Concurrent.broadcast[Int]

  implicit val intMessage = Comet.CometMessage[Int](_.toString)
  def eventSource = Action {
    Ok.stream(sseOutEnumerator.through(EventSource())).as("text/event-stream")
  }
}