package controllers

import akka.actor.ActorSystem
import be.objectify.deadbolt.scala.ActionBuilders
import cats.free.Free
import controllers.auth.AuthUser
import controllers.formats.HttpFormats._
import controllers.formats.request.CreateAppointmentRequest
import controllers.util.ControllerUtils
import controllers.util.ControllerUtils._
import db.DbConnectionUtils
import db.data.Appointment.AppointmentId
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import db.data.{Appointment, AppointmentData, Schedule}
import doobie.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import io.scalaland.chimney.dsl._

import scala.concurrent.ExecutionContext

@Singleton
class AppointmentController @Inject()(ab: ActionBuilders,
                                      bp: PlayBodyParsers,
                                      cc: ControllerComponents,
                                      cu: DbConnectionUtils,
                                      system: ActorSystem) extends AbstractController(cc) {

  private implicit val _bp: PlayBodyParsers = bp
  private implicit val _ec: ExecutionContext = ControllerUtils.getExecutionContext(system)

  //todo unique constraint errors

  def create: Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() { implicit r =>
    extractJsObjectAsync[CreateAppointmentRequest] { req =>

      Schedule.select(req.scheduleId)
        .flatMap[Result] {
          case Some(s) =>
            Appointment
              .insert(req.into[AppointmentData].withFieldConst(_.hostId, s.data.hostId).transform)
              .map(_ => Ok)

          case None =>
            Free.pure(BadRequest)
        }
        .transact(cu.transactor)
        .attempt
        .map {
          case Left(e) =>
            println(e)
            BadRequest //todo error

          case Right(res) => res
        }
        .unsafeToFuture()
    }
  }

  def get(id: AppointmentId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    Appointment.selectById(id)
      .transact(cu.transactor)
      .map { r =>
        Ok(r.toJson).withSession(new Session())
      }
      .unsafeToFuture()
  }

  def cancel(id: AppointmentId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() { implicit r =>
    Appointment.checkAppointmentUser(id, r.subject.get.asInstanceOf[AuthUser].id)
      .flatMap[Result] {
        case Some(true) => Appointment.delete(id).map(_ => Ok)

        case Some(false) => Free.pure(Forbidden)

        case None => Free.pure(NotFound)
      }
      .transact(cu.transactor)
      .unsafeToFuture()
  }

  def byUserId(id: UserId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    Appointment.selectByUserId(id)
      .transact(cu.transactor)
      .unsafeToFuture()
      .map(r => Ok(r.toJson))
  }

  def byScheduleId(id: ScheduleId): Action[AnyContent] = ab.SubjectPresentAction().defaultHandler() {
    Appointment.selectByScheduleId(id)
      .transact(cu.transactor)
      .unsafeToFuture()
      .map(r => Ok(r.toJson))
  }
}
