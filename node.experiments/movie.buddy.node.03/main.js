/*=== Main Application ===*/
//var args = process.argv.splice(2);
var cluster = require('cluster');
var express = require('express');
var fs = require("fs");

var http = require('http');

//http.globalAgent.maxSockets = 5
console.log("maxSockets", http.globalAgent.maxSockets)

var movies = (JSON.parse(fs.readFileSync("./db/movies.json", "utf8")));
var users = (JSON.parse(fs.readFileSync("./db/users.json", "utf8")));

var rates = {};

var preco = require("./libs/euclidean");

// Code to run if we're in the master process
if (cluster.isMaster) {

  // Count the machine's CPUs
  var cpuCount = require('os').cpus().length;

  // Create a worker for each CPU
  for (var i = 0; i < cpuCount; i += 1) {
    cluster.fork();
  }

  // Listen for dying workers
  cluster.on('exit', function (worker) {

    // Replace the dead worker, we're not sentimental
    console.log('Worker ' + worker.id + ' died :(');
    cluster.fork();

  });


// Code to run if we're in a worker process
} else {

  var app = express()

  app.use(express.static(__dirname + '/public'));
  app.use(express.json());
  app.use(express.urlencoded());

  /* 1ère optimisation */
  app.get("/movies", function(req, res) {
    //res.send(movies);
    res.sendfile("./db/movies.json", "utf8");
  });

  app.get("/movies/:id", function(req, res) {
    res.send(movies.filter(function(movie) {
      return movie._id == req.params.id;
    }));
  });

  app.get("/movies/search/title/:title/:limit", function(req, res) {
    res.send(movies.filter(function(movie) {
      return movie.Title.toLowerCase().search(new RegExp(req.params.title.toLowerCase()),"g") != -1;
    }).slice(0,req.params.limit));
  });

  app.get("/movies/search/actors/:actors/:limit", function(req, res) {
    res.send(movies.filter(function(movie) {
      return movie.Actors.toLowerCase().search(new RegExp(req.params.actors.toLowerCase()),"g") != -1;
    }).slice(0,req.params.limit));
  });

  app.get("/movies/search/genre/:genre/:limit", function(req, res) {

    /* 2ème optimisation (pas de différence)*/

    var regex = new RegExp(req.params.genre,"i");
    res.send(movies.filter(function(movie) {
      return movie.Genre.search(regex) != -1;
    }).slice(0,req.params.limit));



    /* 1ère optimisation (pas de différence) */
    /*
     var regex = new RegExp(req.params.genre.toLowerCase(),"g");
     res.send(movies.filter(function(movie) {
     return movie.Genre.toLowerCase().search(regex) != -1;
     }).slice(0,req.params.limit));
     */

    /*
     res.send(movies.filter(function(movie) {
     return movie.Genre.toLowerCase().search(new RegExp(req.params.genre.toLowerCase()),"g") != -1;
     }).slice(0,req.params.limit));
     */
  });

  app.get("/users", function(req, res) {
    res.send(users);
  });

  app.get("/users/:id", function(req, res) {
    res.send(users.filter(function(user) {
      return user._id == req.params.id;
    }));
  });

  app.get("/users/search/:name/:limit", function(req, res) {
    res.send(users.filter(function(user) {
      return user.name.toLowerCase().search(new RegExp(req.params.name.toLowerCase()),"g") != -1;
    }).slice(0,req.params.limit));
  });

  app.post("/rates", function(req, res) {

    var userRate = req.body;

    if(!rates[userRate.userId]) rates[userRate.userId] = {};
    rates[userRate.userId][userRate.movieId] = userRate.rate;

    res.statusCode = 301;
    res.header("location", "/rates/"+userRate.userId).end();

  });

//$.getJSON("rates/2164", function(data) { console.log(data); })
  app.get("/rates/:userid1", function(req, res) {
    if (!rates[req.params.userid1]) rates[req.params.userid1] = [];
    res.json(200,rates[req.params.userid1]);
  });


//$.getJSON("users/share/2164/452", function(data) { console.log(data); })
  app.get("/users/share/:userid1/:userid2", function(req, res) {
    res.json(200, preco.sharedPreferences(rates, +req.params.userid1, +req.params.userid2));
  });

//$.getJSON("users/distance/2164/452", function(data) { console.log(data); })
  app.get("/users/distance/:userid1/:userid2", function(req, res) {
    var distance = {distance:preco.distance(rates, req.params.userid1, req.params.userid2)};
    res.json(200, distance);
  });


  app.listen(process.env.PORT || 3000);
  console.log("Listening on " + (process.env.PORT !==undefined ? process.env.PORT : 3000));

  console.log('Worker ' + cluster.worker.id + ' running!');
}

