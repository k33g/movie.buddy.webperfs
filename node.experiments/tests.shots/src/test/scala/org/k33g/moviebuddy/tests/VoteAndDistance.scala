package org.k33g.moviebuddy.tests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import bootstrap._

class VoteAndDistance extends Simulation {
  val title = System.getProperty("title", "localhost")
  val server = System.getProperty("buddyserver", "http://localhost:3000")


  //val title = System.getProperty("title", "Node-Cluster-EU")
  //val server = System.getProperty("buddyserver", "http://movie-buddy-node-cluster-2.devoxxfr-web-perfs.eu.cloudbees.net")

  //val title = System.getProperty("title", "Node-EU")
  //val server = System.getProperty("buddyserver", "http://movie-buddy-node-2.devoxxfr-web-perfs.eu.cloudbees.net")

  //val title = System.getProperty("title", "Play-Scala-EU")
  //val server = System.getProperty("buddyserver", "http://movie-buddy-play-scala-2.devoxxfr-web-perfs.eu.cloudbees.net")

  val totalUsers = Integer.getInteger("gatling.users", 100).toInt
  val delayInjection = Integer.getInteger("gatling.delay", 100).toInt
  val loops = Integer.getInteger("gatling.loops", 100).toInt
  /*
  val kindOfSearch = System.getProperty("kinsofsearch", "genre")
  val searchValue = System.getProperty("searchvalue", "comedy")
  val limit = System.getProperty("limit", "300")
  */

  val scn = scenario(s"$title : some votes and distance ($totalUsers users/$loops loops)")
    .repeat(loops) {
    exec(
      http("rates-4806-23-3")
        .post(server+"/rates")
        .body(io.gatling.http.request.StringBody("{\"userId\":4806,\"movieId\":23,\"rate\":3}"))
        .asJSON
        .check(status.is(200))
    )
      .exec(
        http("rates-4806-83-5")
          .post(server+"/rates")
          .body(io.gatling.http.request.StringBody("{\"userId\":4806,\"movieId\":83,\"rate\":5}"))
          .asJSON
          .check(status.is(200))
      )
      .exec(
        http("rates-4806-416-1")
          .post(server+"/rates")
          .body(io.gatling.http.request.StringBody("{\"userId\":4806,\"movieId\":416,\"rate\":1}"))
          .asJSON
          .check(status.is(200))
      )
      .exec(
        http("rates-8310-83-2")
          .post(server+"/rates")
          .body(io.gatling.http.request.StringBody("{\"userId\":8310,\"movieId\":83,\"rate\":2}"))
          .asJSON
          .check(status.is(200))
      )
      .exec(
        http("rates-8310-416-5")
          .post(server+"/rates")
          .body(io.gatling.http.request.StringBody("{\"userId\":8310,\"movieId\":416,\"rate\":5}"))
          .asJSON
          .check(status.is(200))
      )
      .exec(
        http("distance-4806-8310")
          .get(server+"/users/distance/4806/8310")
          .check(status.is(200))
      )
  }

  setUp(scn
    .inject(ramp(totalUsers) over (delayInjection seconds))
  )
}

/*
    mvn gatling:execute -Dgatling.simulationClass=org.k33g.moviebuddy.tests.VoteAndDistance
 */