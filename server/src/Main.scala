package server

import cask.*
import os.*

import Util.*
import Event.*

object UrmServer extends cask.MainRoutes {
  override def host = "0.0.0.0"
  override def port = 8080
  println(s"starting server on ${host}:${port}")

  @cask.get("/")
  def index() = {
    logEvent(Access("/"), "anonymous")
    "Welcome to URM Server!"
  }

  initialize()
}

object Config {
  val schedulePath = "schedule.json"
  val logfilePath = "server.log"

}
enum Event(val message: String):
  case Access(resource: String) extends Event(s"Access:$resource")
  case StartTracking extends Event("StartTracking")
  case StopTracking extends Event("StopTracking")

object Util {
  // logs in csv format: timestamp, userId, event
  def logEvent(event: Event, userId: String): Unit =  {
    // timestamp with timezone info
    val timestamp = java.time.ZonedDateTime.now().toString
    val logEntry = s"$timestamp, $userId, ${event.message}\n"
    // TODO: also log ip, ua; as well as condition, day-number
    // log using os-lib, ensure no concurrency issues
    this.synchronized {
      if !os.exists(os.pwd / Config.logfilePath) then
        val header = "timestamp, userId, event\n"
        os.write(os.pwd / Config.logfilePath, header, createFolders = true)
      os.write.append(os.pwd / Config.logfilePath, logEntry)
    }
  }
}


