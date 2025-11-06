package server

import cask.*

import Util.*
import Event.*

object UrmServer extends cask.MainRoutes {
  override def host = "0.0.0.0"
  override def port = 8080
  println(s"starting server on ${host}:${port}")
  
  // read schedule using upickle
  val schedules: Map[String, SubjectSchedule] = {
    val scheduleFile = os.read(Config.schedulePath)
    val parsed       = upickle.default.read[ScheduleFile](scheduleFile)
    println(parsed.assignment_description)
    parsed.schedules.map(s => s.nickname -> s).toMap
  }

  @cask.get("/")
  def index() = {
    logEvent(Access("/"), "anonymous")
    "Welcome to URM Server!"
  }

  @cask.get("/p/:userId")
  def serve(userId: String) = {
    schedules.get(userId) match {
      case Some(schedule) =>
        logEvent(Access("/p/"), userId)
        val msg = upickle.default.write(schedule)
        cask.Response(msg, 200, Seq("Content-Type" -> "application/json; charset=utf-8"))
        // TODO: check if user is allowed to access based on date
      case None =>
        logEvent(Access("/p/"), s"INVALID:$userId")
        val msg = "<!doctype html><h1>404</h1>Unknown participant ID"
        cask.Response(msg, 404, Seq("Content-Type" -> "text/html; charset=utf-8"))
    }
  }

  initialize()
}

object Config {
  val schedulePath = os.pwd / "schedule.json"
  val logfilePath  = "server.log"
}
