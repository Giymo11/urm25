package server

import upickle.default.*

enum Event(val message: String):
  case Access(resource: String) extends Event(s"Access:$resource")
  case StartTracking            extends Event("StartTracking")
  case StopTracking             extends Event("StopTracking")

object Util {
  // logs in csv format: timestamp, userId, event
  def logEvent(event: Event, userId: String): Unit = {
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
case class SubjectSchedule(nickname: String, pattern: String, start_date: String) derives ReadWriter

// json include assignment description
case class ScheduleFile(assignment_description: String, schedules: Seq[SubjectSchedule]) derives ReadWriter


