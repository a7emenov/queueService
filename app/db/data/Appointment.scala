package db.data

import cats.data.NonEmptyList
import db.DatabaseFormats._
import doobie.postgres.implicits._
import db.data.Appointment.AppointmentId
import db.data.AppointmentStatus.Pending
import db.data.Schedule.ScheduleId
import db.data.User.UserId
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.joda.time.{LocalDate, LocalTime}

object Appointment {

  type AppointmentId = Long

  def insert(a: AppointmentData): ConnectionIO[AppointmentId] = {
    sql"""INSERT INTO "Appointment" (hostid, visitorId, date, start, "end", status) VALUES (${a.hostId}, ${a.visitorId}, ${a.date}, ${a.start}, ${a.end}, ${a.status})"""
      .update()
      .withUniqueGeneratedKeys[AppointmentId]("id")
  }

  def selectById(id: AppointmentId): ConnectionIO[Option[Appointment]] = {
    sql"""SELECT "id", hostid, visitorId, date, start, "end", status FROM "Appointment" WHERE id = $id"""
      .query[Appointment]
      .option
  }

  def selectByIds(ids: NonEmptyList[AppointmentId]): ConnectionIO[List[Appointment]] = {
    (fr"""SELECT "id", hostid, visitorId, date, start, "end", status FROM "Appointment" WHERE""" ++ Fragments.in(fr"id", ids))
      .query[Appointment]
      .to[List]
  }

  def selectGeneric(hostId: UserId,
                    date: LocalDate,
                    scheduleIds: List[Int], //todo -> schedule id
                    isCustom: Boolean): ConnectionIO[List[GenericAppointment]] = {
    sql"""SELECT G.g_visitorid, G.g_visitorfullname, G.g_start, G.g_end, G.g_status FROM get_appointments($hostId, $date, $scheduleIds, $isCustom) AS G"""
      .query[GenericAppointment]
      .to[List]
  }
}

case class AppointmentData(hostId: UserId,
                           visitorId: UserId,
                           date: LocalDate,
                           start: LocalTime,
                           end: LocalTime,
                           status: AppointmentStatus = Pending)

case class Appointment(id: AppointmentId, data: AppointmentData) extends IdEntity[AppointmentId, AppointmentData]

case class GenericAppointment(visitorId: Option[UserId],
                              visitorFullName: Option[String],
                              start: LocalTime,
                              end: LocalTime,
                              status: Option[AppointmentStatus])

