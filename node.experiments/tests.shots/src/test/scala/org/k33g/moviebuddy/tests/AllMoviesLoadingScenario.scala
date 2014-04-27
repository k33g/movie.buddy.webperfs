package org.k33g.moviebuddy.tests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.Headers.Names._
import io.gatling.http.request._
import scala.concurrent.duration._
import bootstrap._
import assertions._


class AllMoviesLoadingScenario extends Simulation {
  val title = System.getProperty("title", "localhost")
  val server = System.getProperty("buddyserver", "http://192.168.128.142:3000/movies");
  val totalUsers = toInt(System.getProperty("gatling.users", "100"));
  val loops = toInt(System.getProperty("gatling.loops", "100"));
  val scn = scenario("Loading all movies (" + totalUsers + " users/" + loops + " loops)").repeat(loops) {
    exec(
      http("Loading all movies")
        .get(server + "/movies")
        .check(status.is(200)))
  }

  setUp(scn
    .inject(ramp(totalUsers users) over (totalUsers seconds)))
}

