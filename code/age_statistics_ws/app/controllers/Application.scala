package controllers

import scala.util.control.Exception._

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.iteratee._
import play.api.libs.json._

import models.AgeStatistics

object Application extends Controller {
  var ageStatistics = AgeStatistics.empty

  val (outEnumerator, outChannel) = Concurrent.broadcast[String]

  def index = Action { implicit request =>
    Ok(views.html.index(ageStatistics))
  }

  def input = WebSocket.using[String] { request =>
    val in = Iteratee.foreach[String] { ageString =>
      type NFE = NumberFormatException
      catching(classOf[NFE]).opt(ageString.toInt).foreach { age =>
        if (age > 0 && age < 100) {
          ageStatistics =
            ageStatistics.updated(age, ageStatistics(age) + 1)
          outChannel.push(age.toString)
        }
      }
    }

    (in, outEnumerator)
  }
}