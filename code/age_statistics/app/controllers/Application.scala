package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._

import models.AgeStatistics

object Application extends Controller {
  var ageStatistics = AgeStatistics.sample

  val ageForm = Form("age" -> number)

  def index = Action {
    Ok(views.html.index(AgeStatistics.sample))
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