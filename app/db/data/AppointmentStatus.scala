package db.data

import doobie.postgres.implicits._
import doobie.util.meta.Meta
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class AppointmentStatus(val dbName: String) extends EnumEntry

object AppointmentStatus extends Enum[AppointmentStatus] {

  val values: immutable.IndexedSeq[AppointmentStatus] = findValues

  case object Pending extends AppointmentStatus("pending")
  case object Finished extends AppointmentStatus("finished")
  case object CancelledByUser extends AppointmentStatus("cancelledByUser")
  case object CancelledByHost extends AppointmentStatus("cancelledByHost")

  implicit val AppointmentStatusMeta: Meta[AppointmentStatus] = pgEnumStringOpt(
    "appointment_status",
    name => AppointmentStatus.lowerCaseNamesToValuesMap.get(name.toLowerCase),
    enum => enum.dbName
  )
}
