#Optimisation de performances avec Node et Express

Intro

Sur quoi je teste : VM Linux Node 0.6

##Mon application

Expliquer

Je vais tester 2 services :

    var movies = (JSON.parse(fs.readFileSync("./db/movies.json", "utf8")));

    app.get("/movies", function(req, res) {
      res.send(movies);
    });

puis

    app.get("/movies/search/genre/:genre/:limit", function(req, res) {
      res.send(movies.filter(function(movie) {
        return movie.Genre.toLowerCase().search(new RegExp(req.params.genre.toLowerCase()),"g") != -1;
      }).slice(0,req.params.limit));
    });

###Test 1er service : Charger tous les films

####Code du test

    class AllMoviesLoadingScenario extends Simulation {
      val title = System.getProperty("title", "localhost")
      val server = System.getProperty("buddyserver", "http://192.168.128.142:3000");
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

####Résultats

|                           | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 01- Tous les films        | 6        |           |                    | 10000      | 1578         |


####Optimisation du service

Avant:

    app.get("/movies", function(req, res) {
      res.send(movies);
    });

Après :

    app.get("/movies", function(req, res) {
      res.sendfile("./db/movies.json", "utf8");
    });

####Résultats après optimisation

|                           | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 02- Tous les films        | 99       | 10000     |                    |            | 100          |

####???

Déjà allez lire l'introduction à node par **Cédric Exbrayat** [http://hypedrivendev.wordpress.com/2011/06/28/getting-started-with-node-js-part-1/](http://hypedrivendev.wordpress.com/2011/06/28/getting-started-with-node-js-part-1/).

Je cite : *"Node.js ne se base pas sur des threads : c’est un serveur asynchrone qui utilise un process monothread et des I/O non bloquants. L’asynchronisme permet au thread d’exécuter des callbacks lorsqu’il est notifié d’une connexion"* ... *"Les I/O regroupent les accès disques, accès base de données, accès réseaux, bref tout ce qui prend du temps sur un ordinateur moderne (latence, débit limité etc…). Ici tous les I/O sont non bloquants, c’est à dire que tout appel est effectué en asynchrone, avec un callback qui sera exécuté une fois l’accès réalisé."*

En fait la 1ère fois, j'allais lire mes données en mémoire, du coup je ne profitais pas des I/O et finalement je perdais les avantages de l'asynchrone.

####Même test avec Node 0.11

J'ai ensuite upgradé la version de node, pas d'amélioration notable

|                           | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 06- Tous les films        | 99       | 10000     |                    |            | 100          |

Pas d'amélioration notable

###Test 2ème service : Charger un certain nombre de films dans une catégorie donnée

On est à nouveau avec du Node 0.6

####Code du test

    class SomeMoviesLoadingScenario extends Simulation {
      val title = System.getProperty("title", "localhost")
      val server = System.getProperty("buddyserver", "http://192.168.128.142:3000");

      val totalUsers = Integer.getInteger("gatling.users", 100).toInt
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

####Résultats

|                          | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------ |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 03- 300 1ères comédies   | 99       | 10000     |                    |            | 100          |

####Optimisation 1 du service

Alors on m'a conseillé plusieurs optimisations, comme "sortir" `new RegExp()` de `filter`

Avant :

    app.get("/movies/search/genre/:genre/:limit", function(req, res) {
      res.send(movies.filter(function(movie) {
        return movie.Genre.toLowerCase().search(new RegExp(req.params.genre.toLowerCase()),"g") != -1;
      }).slice(0,req.params.limit));
    });

Après :

    app.get("/movies/search/genre/:genre/:limit", function(req, res) {
      var regex = new RegExp(req.params.genre.toLowerCase(),"g");
      res.send(movies.filter(function(movie) {
        return movie.Genre.toLowerCase().search(regex) != -1;
      }).slice(0,req.params.limit));
    });

####Résultats

J'ai obtenu les mêmes résultats

|                          | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------ |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 04- 300 1ères comédies   | 99       | 10000     |                    |            | 100          |

Je serais tenté de dire que la condition de `filter` n'est exécuté qu'une seule fois, ce n'est pas un `forEach`

####Optimisation 2 du service

On m'a aussi conseillé de ne pas utiliser `toLowerCase()` mais plutôt la clause `i` (insensitive) des regex.

Avant :

    app.get("/movies/search/genre/:genre/:limit", function(req, res) {
      var regex = new RegExp(req.params.genre.toLowerCase(),"g");
      res.send(movies.filter(function(movie) {
        return movie.Genre.toLowerCase().search(regex) != -1;
      }).slice(0,req.params.limit));
    });

Après :

    app.get("/movies/search/genre/:genre/:limit", function(req, res) {
      var regex = new RegExp(req.params.genre,"i");
      res.send(movies.filter(function(movie) {
        return movie.Genre.search(regex) != -1;
      }).slice(0,req.params.limit));
    });


J'ai là aussi, obtenu les mêmes résultats

|                          | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------ |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 05- 300 1ères comédies   | 99       | 10000     |                    |            | 100          |

Je serais donc tenté de dire que la VM de Node est plutôt bien optimisée, ainsi que l'implémentation de `toLowerCase()`

####Optimisation 3 du service : avec Node 0.11

J'ai ensuite upgradé la version de node à 0.11 : mêmes résultats, pas d'amélioration notable avec la dernière version de Node

|                          | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------ |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 07- 300 1ères comédies   | 99       | 10000     |                    |            | 100          |


####Optimisation 4 du service : avec Node 0.11 et en jouant avec `http.globalAgent.maxSockets`

En standard sur ma VM j'ai `http.globalAgent.maxSockets = Infinity`, j'ai forcé la valeur à 5, 50 puis 150, je n'ai pas eu d'amélioration :

|                              | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ---------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 08- 300 1ères maxSockets=5   | 99       | 10000     |                    |            | 100          |
| 09- 300 1ères maxSockets=50  | 99       | 10000     |                    |            | 100          |
| 10- 300 1ères maxSockets=150 | 99       | 10000     |                    |            | 100          |


Je m'aperçois que Node n'a aucun problème à servir ses 100 requêtes secondes, du coup je me décide à le stresser un peu et modifie mon code de test :

###Test 2ème service "plus dur" : Charger un certain nombre de films dans une catégorie donnée

Pour le même délai d'injection, je vais augmenter le nombre d'utilisateurs :

    val totalUsers = Integer.getInteger("gatling.users", 300).toInt

et le délai (qui ne change pas)

    val delayInjection = Integer.getInteger("gatling.delay", 100).toInt

ce qui nous donnera 30 000 requêtes

####Code du test (toujours en Node 0.11)

    class SomeMoviesLoadingScenarioHarder extends Simulation {
      val title = System.getProperty("title", "localhost")
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

Je repars du code non optimisé du service :

Avant :

    app.get("/movies/search/genre/:genre/:limit", function(req, res) {
      res.send(movies.filter(function(movie) {
        return movie.Genre.toLowerCase().search(new RegExp(req.params.genre.toLowerCase()),"g") != -1;
      }).slice(0,req.params.limit));
    });

Après :

    app.get("/movies/search/genre/:genre/:limit", function(req, res) {
      var regex = new RegExp(req.params.genre.toLowerCase(),"g");
      res.send(movies.filter(function(movie) {
        return movie.Genre.toLowerCase().search(regex) != -1;
      }).slice(0,req.params.limit));
    });

Puis :

    app.get("/movies/search/genre/:genre/:limit", function(req, res) {
      var regex = new RegExp(req.params.genre,"i");
      res.send(movies.filter(function(movie) {
        return movie.Genre.search(regex) != -1;
      }).slice(0,req.params.limit));
    });

Puis : `http.globalAgent.maxSockets = 400`, puis `http.globalAgent.maxSockets = 10`

####Résultats

On voit bien que cette fois-ci, on a "stressé" node :

|                                  | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| -------------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 11- 300 1ères pas d'optimisation | 132      | 9008      | 5926               | 15066      | 227          |
| 12- 300 1ères optimisation 1     | 138      | 9411      | 5936               | 14653      | 217          |
| 13- 300 1ères optimisation 2     | 132      | 9264      | 4434               | 16302      | 227          |
| 14- 300 1ères maxSockets=400     | 130      | 9173      | 6322               | 14505      | 230          |
| 15- 300 1ères maxSockets=10      | 130      | 5957      | 7220               | 16823      | 230          |

Ma première conclusion serait de garder la valeur par défaut de `http.globalAgent.maxSockets` et creuser sur les optimisations concernant les regexs (faire plus de tirs), mais je continue à penser que `toLowerCase()` fait très bien son boulot.

Avant de passer à la suite, je vais aussi modifier mon code de test pour le chargement de tous les films, là aussi avec 300 utilisateurs avec un délai de 100 secondes.

####Code du test (toujours en Node 0.11)

    class AllMoviesLoadingScenarioHarder extends Simulation {
      val title = System.getProperty("title", "localhost")
      val server = System.getProperty("buddyserver", "http://192.168.128.142:3000");
      val totalUsers = toInt(System.getProperty("gatling.users", "300"));
      val delayInjection = toInt(System.getProperty("gatling.delay", "100"));

      val loops = toInt(System.getProperty("gatling.loops", "100"));
      val scn = scenario("Loading all movies (" + totalUsers + " users/" + loops + " loops)").repeat(loops) {
        exec(
          http("Loading all movies")
            .get(server + "/movies")
            .check(status.is(200)))
      }

      setUp(scn
        .inject(ramp(totalUsers users) over (delayInjection seconds)))
    }

####Résultats

|                                                   | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 16- Tous les films (fichier sur disque) 300 users | 141      | 8558      | 6487               | 14955      | 213          |


##Module cluster

Maintenant je vais essayer le module cluster de node avec nos 2 services : "Tous les films" et "les 300 1ères comédies", toujours sur du node 0.11. L'utilisation du module cluster de node, implique quelques modifications. Globalement, votre code applicatif est déplacé dans le `else` ci-dessous :

    var cluster = require('cluster');

    //...

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
      // your application ...
    }

Référence au code : []()

>>Expliquer le module cluster de node

###Résultats

On s'aperçoit que la mise en œuvre du module cluster est particulièrement payante :

|                                                   | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 17- Tous les films (disque) 300 users / cluster   | 181      | 22207     | 3993               | 3800       | 165          |
| 18- 300 1ères comédies 300 users / cluster        | 206      | 26417     | 3149               | 434        | 145          |


si on passe les tests avec seulement 100 utilisateurs :

|                                                   | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 19- Tous les films (disque) 100 users / cluster   | 99       | 10000     |                    |            | 100          |
| 20- 300 1ères comédies 100 users / cluster        | 99       | 10000     |                    |            | 100          |

Pas de changement par rapport à la version sans le module cluster pour le même scénario, donc le module cluster n'est intéressant qu'à partir d'un certain nombre d'utilisateurs.

##Utilisation d'Express 4

J'ai ensuite procédé à la mise à jour d'express et relancé les tests sur les 2 services (optimisés) avec 300 utilisateurs :

###Résultats

|                                                   | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| ------------------------------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 21- Tous les films (disque) 300 users / cluster   | 196      | 25436     | 1777               | 2787       | 152          |
| 22- 300 1ères comédies 300 users / cluster        | 232      | 29789     | 191                |            | 129          |

On note que la mise à jour en version 4.1.x d'Express met un sérieux coup de boost à l'application.

##1ère consolidation

|                                                           | Nb Req/s | t < 800ms | 800ms < t < 1200ms | t > 1200ms | Durée (secs) |
| --------------------------------------------------------- |:--------:|:---------:|:------------------:|:----------:|:------------:|
| 01- Tous les films (node 0.6 fichier en mémoire)          | 6        |           |                    | 10000      | 1578         |
| 02- Tous les films (node 0.6 fichier sur disque)          | 99       | 10000     |                    |            | 100          |
| 03- 300 1ères comédies (node 0.6)                         | 99       | 10000     |                    |            | 100          |
| 04- 300 1ères (node 0.6) optimisation 1                   | 99       | 10000     |                    |            | 100          |
| 05- 300 1ères (node 0.6) optimisation 2                   | 99       | 10000     |                    |            | 100          |
| 06- Tous les films (node 0.11 fichier sur disque)         | 99       | 10000     |                    |            | 100          |
| 07- 300 1ères (node 0.11) optimisation 2                  | 99       | 10000     |                    |            | 100          |
| 08- 300 1ères (node 0.11) maxSockets=5                    | 99       | 10000     |                    |            | 100          |
| 09- 300 1ères (node 0.11) maxSockets=50                   | 99       | 10000     |                    |            | 100          |
| 10- 300 1ères (node 0.11) maxSockets=150                  | 99       | 10000     |                    |            | 100          |
| 11- 300 1ères pas d'optimisation 100->300 users           | 132      | 9008      | 5926               | 15066      | 227          |
| 12- 300 1ères optimisation 1 100->300 users               | 138      | 9411      | 5936               | 14653      | 217          |
| 13- 300 1ères optimisation 2 100->300 users               | 132      | 9264      | 4434               | 16302      | 227          |
| 14- 300 1ères maxSockets=400 100->300 users               | 130      | 9173      | 6322               | 14505      | 230          |
| 15- 300 1ères maxSockets=10  100->300 users               | 130      | 5957      | 7220               | 16823      | 230          |
| 16- Tous les films (fichier sur disque) 300 users         | 141      | 8558      | 6487               | 14955      | 213          |
| 17- Tous les films (disque) 300 users / cluster           | 181      | 22207     | 3993               | 3800       | 165          |
| 18- 300 1ères comédies 300 users / cluster                | 206      | 26417     | 3149               | 434        | 145          |
| 19- Tous les films (disque) 100 users / cluster           | 99       | 10000     |                    |            | 100          |
| 20- 300 1ères comédies 100 users / cluster                | 99       | 10000     |                    |            | 100          |
| 21- Tous les films (disque) 300 users / cluster exp. v4   | 196      | 25436     | 1777               | 2787       | 152          |
| 22- 300 1ères comédies 300 users / cluster exp. v4        | 232      | 29789     | 191                |            | 129          |

##Conclusion n°1

Pour le moment, les seul réels axes d'optimisation concernent les I/O, le module cluster et la mise à jour majeure d'Express. L'exercice suivant (à venir) sera de comparer avec d'autres stacks mais aussi avec un backend (type base de données) pour avoir une idée plus précise. Faites des tests suffisamment "stressants" pour noter les différences (ex module cluster).





