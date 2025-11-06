package server

import scala.collection.mutable
import java.time.*
import cask.*

import Util.*
import ValidEvent.*

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

  val participantState: mutable.Map[String, ParticipantState] = mutable.HashMap().withDefault(userId => ParticipantState(userId, "", None))

  @cask.get("/")
  def index() = {
    logEvent(Access("/"), "anonymous")
    "Welcome to URM Server!"
  }

  @cask.get("/p/:userId")
  def serve(userId: String) = {
     
    schedules.get(userId) match {
      case Some(schedule) if schedule.isWithinAccessPeriod =>
        logEvent(Access("/p/"), userId)
        val msg = upickle.default.write(schedule)
        cask.Response(msg, 200, Seq("Content-Type" -> "application/json; charset=utf-8"))
      case Some(_) =>
        logEvent(Access("/p/"), s"TIME_INVALID:$userId")
        val msg = "<!doctype html><h1>403</h1>You don't have acces (yet/anymore)."
        cask.Response(msg, 403, Seq("Content-Type" -> "text/html; charset=utf-8"))
      case None =>
        logEvent(Access("/p/"), s"USERID_INVALID:$userId")
        val msg = "<!doctype html><h1>404</h1>Unknown participant ID"
        cask.Response(msg, 404, Seq("Content-Type" -> "text/html; charset=utf-8"))
    }
  }

  @cask.get("/api/:userId/state")
  def getState(userId: String) = {
    schedules.get(userId) match {
      case Some(schedule) => 
        logEvent(Access("/api/state/"), userId)
        val state = participantState(userId)
        schedule.currentCondition match {
          case Some(cond) if cond == state.current_condition =>
            val msg = upickle.default.write(state)
            cask.Response(msg, 200, Seq("Content-Type" -> "application/json; charset=utf-8"))
          case Some(cond) =>
            // if condition changed during tracking, reset it
            if state.tracking_timestamp.isDefined then {
              logEvent(Access("/api/state/"), s"TRACKING_AUTORESET:$userId")
            }
            // update condition
            val newState = state.copy(current_condition = cond, tracking_timestamp = None)
            participantState.update(userId, newState)
            val msg = upickle.default.write(newState)
            cask.Response(msg, 200, Seq("Content-Type" -> "application/json; charset=utf-8"))
          case None =>
            val msg = "<!doctype html><h1>403</h1>You don't have acces (yet/anymore)."
            cask.Response(msg, 403, Seq("Content-Type" -> "text/html; charset=utf-8"))
        }
      case None =>
        logEvent(Access("/api/state/"), s"USERID_INVALID:$userId")
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
