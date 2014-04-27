


##movie.buddy.node.01

- pas d'optimisation
- express 3.5.1

##movie.buddy.node.02

###1ère optimisation

    /* 1ère optimisation */
    app.get("/movies", function(req, res) {
      //res.send(movies);
      res.sendfile("./db/movies.json", "utf8");
    });

Référence I/O
[http://hypedrivendev.wordpress.com/2011/06/28/getting-started-with-node-js-part-1/](http://hypedrivendev.wordpress.com/2011/06/28/getting-started-with-node-js-part-1/)




##tests

git clone --q --depth 0 git@github.com:excilys/gatling-maven-plugin-demo.git tests.shots

