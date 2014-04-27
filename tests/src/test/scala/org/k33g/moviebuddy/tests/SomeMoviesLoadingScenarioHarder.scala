package org.k33g.moviebuddy.tests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import bootstrap._


class SomeMoviesLoadingScenarioHarder extends Simulation {
  val title = System.getProperty("title", "localhost")
  //val server = System.getProperty("buddyserver", "http://localhost:3000")
  val server = System.getProperty("buddyserver", "http://192.168.128.142:3000");

  val totalUsers = Integer.getInteger("gatling.users", 300).toInt
  val delayInjection = Integer.getInteger("gatling.delay", 100).toInt
  val loops = Integer.getInteger("gatling.loops", 100).toInt
  val kindOfSearch = System.getProperty("kinsofsearch", "genre")
  val searchValue = System.getProperty("searchvalue", "comedy")
  val limit = System.getProperty("limit", "300")

  val scn = scenario(s"$title : Loading some (max $limit) movies by $kindOfSearch  = $searchValue ($totalUsers users/$loops loops)")
    .repeat(loops) {
    exec(
      http(s"Loading some (max $limit) movies by $kindOfSearch = $searchValue")
        .get(server + "/movies/search/" + kindOfSearch + "/" + searchValue + "/" +limit)
        .check(status.is(200)))
  }

  setUp(scn
    .inject(ramp(totalUsers) over (delayInjection seconds))
  )
}

/*
    mvn gatling:execute -Dgatling.simulationClass=org.k33g.moviebuddy.tests.SomeMoviesLoadingScenarioHarder
 */