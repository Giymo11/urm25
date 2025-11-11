package server

import java.time.*
import upickle.default.*

enum AccessError: // Only invalid events
  case InvalidTime(start: Instant)
  case InvalidUser(userId: String)

enum AccessSuccess: // Only valid events
  case Access(resource: String)
  case StartTracking(wasTracking: Boolean)
  case StopTracking(wasTracking: Boolean)

type AccessEvent = AccessError | AccessSuccess

extension (event: AccessEvent)
  def logMessage: String                                = event match
    case AccessError.InvalidTime(start)           => s"INVALID_TIME, access starts: ${start.toString}"
    case AccessError.InvalidUser(userId)          => s"INVALID_USER, userId: $userId"
    case AccessSuccess.Access(resource)           => s"ACCESS, resource: $resource"
    case AccessSuccess.StartTracking(wasTracking) => s"START_TRACKING, previously tracking: $wasTracking"
    case AccessSuccess.StopTracking(wasTracking)  => s"STOP_TRACKING, previously tracking: $wasTracking"
  def logEvent(userId: String = "unknown"): AccessEvent =
    Util.logEvent(event, userId)
    event

object Util:
  // logs in csv format: timestamp, userId, event
  private def logEntry(msg: String): Unit =
    import os.*
    // log using os-lib, ensure no concurrency issues
    this.synchronized {
      if !exists(pwd / Config.logfilePath) then
        val header = "timestamp, userId, event, message\n"
        write(pwd / Config.logfilePath, header, createFolders = true)
      write.append(pwd / Config.logfilePath, msg + "\n")
    }

  def logMessage(msg: String, userId: String = "unknown"): Unit =
    // timestamp with timezone info
    val timestamp = java.time.ZonedDateTime.now().toString
    val logMsg    = s"$timestamp, $userId, $msg"
    logEntry(logMsg)

  def logEvent(event: AccessEvent, userId: String): Unit = logMessage(event.logMessage, userId)

// create json for server to load
case class SubjectSchedule(nickname: String, pattern: String, start_date: String) derives ReadWriter {
  def daysAfterStart: Long =
    Duration.between(Instant.parse(start_date), Instant.now()).toDays

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
