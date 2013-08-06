package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._

import scala.collection.immutable.SortedMap

import models.AgeStatistics

object Application extends Controller {
  var ageStatistics = AgeStatistics.exampleStatistics

  val ageForm = Form("age" -> number)

  def index = Action {
    // Ok(views.html.index(AgeStatistics.exampleStatistics))
    Ok(views.html.index(SortedMap(ageStatistics.toSeq: _*)))
  }

  def input = Action { implicit request =>
    ageForm.bindFromRequest.fold(
      invalidForm => BadRequest(invalidForm.errorsAsJson.toString),
      { age =>
        ageStatistics = ageStatistics.updated(age, ageStatistics(age) + 1)
        Redirect(routes.Application.index)
      }
    )
  }

}