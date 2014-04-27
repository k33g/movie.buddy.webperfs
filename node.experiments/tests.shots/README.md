gatling-maven-plugin-demo
=========================

Simple showcase of a maven project using the gatling-maven-plugin.

To test it out, simply execute the following command :

    $mvn gatling:execute -Dgatling.simulationClass=basic.BasicExampleSimulation

    $mvn gatling:execute -Dgatling.simulationClass=org.k33g.moviebuddy.tests.AllMoviesLoadingScenario
