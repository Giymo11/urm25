package server

import scala.collection.mutable
import scala.collection.concurrent.TrieMap
import java.time.*
import cask.*

import Util.*
import AccessSuccess.*, AccessError.*

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

  val participantState: mutable.Map[String, ParticipantState] =
    TrieMap().withDefault(userId => ParticipantState(userId, "", None))

  @cask.get("/")
  def index() =
    Access("/").logEvent()
    "Welcome to URM Server!"

  def userResponse(path: String, userId: String, func: SubjectSchedule => cask.Response[String]) =
    Access(path).logEvent(userId)
    schedules.get(userId) match {
      case Some(schedule) if schedule.isWithinAccessPeriod => func(schedule)
      case Some(schedule)                                  => InvalidTime(Instant.parse(schedule.start_date)).toResponse
      case None                                            => InvalidUser(userId).toResponse
    }

  @cask.get("/p/:userId")
  def serve(userId: String) =
    // Serve the experiment page; all dynamic data is fetched client-side via /api/:userId/state
    val htmlPath = os.resource / "participant.html"
    val content  = os.read(htmlPath)
    cask.Response(content, 200, Seq("Content-Type" -> "text/html; charset=utf-8"))

  def conditionResponse(path: String, userId: String, func: (String, ParticipantState) => cask.Response[String]) =
    val respFunc = (schedule: SubjectSchedule) =>
      schedule.currentCondition match {
        case Some(cond) if cond != participantState(userId).current_condition =>
          val state    = participantState(userId)
          // check if currently tracking
          if state.tracking_timestamp.isDefined then
            Util.logMessage(
              s"WARN, User $userId condition changed from ${state.current_condition} to $cond while tracking.",
              userId
            )
          // update state
          val newState = state.copy(current_condition = cond, tracking_timestamp = None)
          participantState.update(userId, newState)
          // respond with new condition
          func(cond, participantState(userId))
        case Some(cond)                                                       => func(cond, participantState(userId))
        case None                                                             => InvalidTime(Instant.parse(schedule.start_date)).toResponse
      }
    userResponse(path, userId, respFunc)

  @cask.get("/api/:userId/state")
  def getState(userId: String) =
    val respFunc = (condition: String, state: ParticipantState) =>
      val msg = upickle.default.write(state)
      cask.Response(msg, 200, Seq("Content-Type" -> "application/json; charset=utf-8"))
    conditionResponse("/api/userId/state", userId, respFunc)

  @cask.post("/api/:userId/start_tracking")
  def startTracking(userId: String) =
    val respFunc = (cond: String, state: ParticipantState) => {
      // log if already tracking
      val wasAlreadyTracking = state.tracking_timestamp.isDefined
      if wasAlreadyTracking then
        Util.logMessage(s"WARN, User $userId started tracking again without stopping first.", userId)
      StartTracking(wasAlreadyTracking).logEvent(userId)
      // update state
      val timestamp          = Instant.now().toString
      val newState           = state.copy(current_condition = cond, tracking_timestamp = Some(timestamp))
      participantState.update(userId, newState)
      // respond with new state
      val msg                = upickle.default.write(newState)
      cask.Response(msg, 200, Seq("Content-Type" -> "application/json; charset=utf-8"))
    }
    conditionResponse("/api/userId/start_tracking", userId, respFunc)

  @cask.post("/api/:userId/stop_tracking")
  def stopTracking(userId: String) =
    val respFunc = (cond: String, state: ParticipantState) => {
      // log if not tracking
      val wasAlreadyTracking = state.tracking_timestamp.isDefined
      if !wasAlreadyTracking then
        Util.logMessage(s"WARN, User $userId stopped tracking without starting first.", userId)
      StopTracking(wasAlreadyTracking).logEvent(userId)
      // update state
      val newState           = state.copy(current_condition = cond, tracking_timestamp = None)
      participantState.update(userId, newState)
      // respond with new state
      val msg                = upickle.default.write(newState)
      cask.Response(msg, 200, Seq("Content-Type" -> "application/json; charset=utf-8"))
    }
    conditionResponse("/api/userId/stop_tracking", userId, respFunc)

  // initialize server at the end
  initialize()
}

object Config {
  // Resolve paths from environment variables if set, otherwise default to /data inside container
  private def envPath(name: String, default: os.Path): os.Path =
    sys.env.get(name).map(os.Path(_, os.pwd)).getOrElse(default)

  val schedulePath = envPath("SCHEDULE_PATH", os.Path("./data/schedule.json", os.pwd))
  val logfilePath  = envPath("SERVER_LOG_PATH", os.Path("./data/server.log", os.pwd))
}

extension (error: AccessError)
  def toResponse: cask.Response[String] =
    error.logEvent()
    error match {
      case AccessError.InvalidTime(_) =>
        val msg = "<!doctype html><h1>403</h1>You don't have access (yet/anymore)."
        cask.Response(msg, 403, Seq("Content-Type" -> "text/html; charset=utf-8"))
      case AccessError.InvalidUser(_) =>
        val msg = "<!doctype html><h1>404</h1>User not found."
        cask.Response(msg, 404, Seq("Content-Type" -> "text/html; charset=utf-8"))
    }
