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
  val logfilePath  = "server.log"
}
