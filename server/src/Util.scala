package server

import java.time.*
import upickle.default.*

enum ValidEvent(val message: String):
  case Access(resource: String) extends ValidEvent(s"Access:$resource")
  case StartTracking            extends ValidEvent("StartTracking")
  case StopTracking             extends ValidEvent("StopTracking")

enum InvalidEvent(val message: String):
  case InvalidTime(start: Instant) extends InvalidEvent(s"INVALID_TIME:${start.toString}")
  case InvalidUser(userId: String) extends InvalidEvent("UNKNOWN_USERID")

object Util {
  // logs in csv format: timestamp, userId, event
  def logEvent(event: ValidEvent, userId: String): Unit = {
    import os.*
    // timestamp with timezone info
    val timestamp = java.time.ZonedDateTime.now().toString
    val logEntry  = s"$timestamp, $userId, ${event.message}\n"
    // TODO: also log ip, ua; as well as condition, day-number
    // log using os-lib, ensure no concurrency issues
    this.synchronized {
      if !exists(pwd / Config.logfilePath) then
        val header = "timestamp, userId, event\n"
        write(pwd / Config.logfilePath, header, createFolders = true)
      write.append(pwd / Config.logfilePath, logEntry)
    }
  }

}

// create json for server to load
case class SubjectSchedule(nickname: String, pattern: String, start_date: String) derives ReadWriter {
  def daysAfterStart: Long =
    Duration.between(Instant.now(), Instant.parse(start_date)).toDays

  def isWithinAccessPeriod: Boolean =
    daysAfterStart >= 0 && daysAfterStart < 5

  def currentCondition: Option[String] =
    if isWithinAccessPeriod then Some(pattern(daysAfterStart.toInt).toString) else None
}

// json include assignment description
case class ScheduleFile(assignment_description: String, schedules: Seq[SubjectSchedule]) derives ReadWriter

// value of None for tracking_timestamp means currently not tracking)
case class ParticipantState(nickname: String, current_condition: String, tracking_timestamp: Option[String])
    derives ReadWriter
