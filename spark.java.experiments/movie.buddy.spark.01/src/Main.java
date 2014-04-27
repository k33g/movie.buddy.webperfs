import static spark.Spark.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;


public class Main {
  public static void main(String[] args) throws IOException {

    //String path = new File("").getAbsolutePath();
    String path = System.getProperty("app.dir", new File("").getAbsolutePath());

    if(!path.equals(new File("").getAbsolutePath())) { path+="/app";}

    System.out.println("PATH : " + path);

    //externalStaticFileLocation(path+"/public");
    staticFileLocation("/public");
    //setPort(3000);
    setPort(Integer.valueOf(System.getProperty("app.port", "3000")));

    //final int port = Integer.valueOf(System.getProperty("app.port", "3000"));

    HashMap<Double, HashMap<Double, Double>> ratings = new HashMap<>();

    final ObjectMapper mapper = new ObjectMapper();

    List<HashMap> moviesList =  mapper.readValue(new File(path + "/json/movies.json"),List.class);
    final List<HashMap> usersList =  mapper.readValue(new File(path + "/json/users.json"),List.class);

    String jsonMoviesList = mapper.writeValueAsString(moviesList);
    String jsonUsersList = mapper.writeValueAsString(usersList);



    post(new Route("/rates") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        response.status(201);

        try {

          TreeMap rate = mapper.treeToValue(mapper.readValue(request.body(), JsonNode.class), java.util.TreeMap.class);

          if (ratings.get(Double.parseDouble(rate.get("userId").toString())) == null) {
            ratings.put(Double.parseDouble(rate.get("userId").toString()), new HashMap<Double, Double>() {
              {
                put(Double.parseDouble(rate.get("movieId").toString()), Double.parseDouble(rate.get("rate").toString()));
              }
            });
          } else {
            ratings.get(Double.parseDouble(rate.get("userId").toString())).put(Double.parseDouble(rate.get("movieId").toString()), Double.parseDouble(rate.get("rate").toString()));
          }

          //response.header("location","/rates/"+rate.get("userId").toString());
          //response.status(301);

          response.redirect("/rates/"+rate.get("userId").toString(),301);

          //return mapper.writeValueAsString(rate);

        } catch (JsonProcessingException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
        return null;
      }
    });

    get(new Route("/rates/:userid") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        Double userId = Double.parseDouble(request.params(":userid").toString());

        try {
          return mapper.writeValueAsString(
                  ratings.get(userId)
          );
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        return null;
      }
    });


    get(new Route("/users/share/:userid1/:userid2") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        Double user1Id = Double.parseDouble(request.params(":userid1").toString());
        Double user2Id = Double.parseDouble(request.params(":userid2").toString());

        try {
          return mapper.writeValueAsString(
            new Preco().sharedPreferences(ratings, user1Id, user2Id).toArray()
          );
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        return null;
      }
    });

    get(new Route("/users/distance/:userid1/:userid2") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        Double user1Id = Double.parseDouble(request.params(":userid1").toString());
        Double user2Id = Double.parseDouble(request.params(":userid2").toString());

        try {
          return mapper.writeValueAsString(
            new Preco().distance(ratings, user1Id, user2Id)
          );
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        return null;
      }
    });

    get(new Route("/users") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        return jsonUsersList;
      }
    });

    get(new Route("/users/:id") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        try {
          return mapper.writeValueAsString(
            usersList.stream()
            .filter(
                    u -> u.get("_id").toString().equals(request.params(":id").toString())
            ).toArray()
          );
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        return null;
      }
    });

    get(new Route("/users/search/:name/:limit") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        String name = request.params(":name").toString();
        Integer limit = java.lang.Integer.parseInt(request.params(":limit"));
        try {
          return mapper.writeValueAsString(
            usersList.stream()
              .filter(
                      u -> u.get("name").toString().toLowerCase().contains(name)
              ).limit(limit).toArray()
          );
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        return null;
      }
    });


    get(new Route("/movies") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        return jsonMoviesList;
      }
    });

    get(new Route("/movies/:id") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        try {
          return mapper.writeValueAsString(
            moviesList.stream()
              .filter(
                m -> m.get("_id").toString().equals(request.params(":id").toString())
              ).toArray()
          );
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        return null;
      }
    });

    get(new Route("/movies/search/title/:title/:limit") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        String title = request.params(":title").toString();
        Integer limit = java.lang.Integer.parseInt(request.params(":limit"));
        try {
          return mapper.writeValueAsString(
            moviesList.stream()
              .filter(
                m -> m.get("Title").toString().toLowerCase().contains(title)
              ).limit(limit).toArray()
          );
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        return null;
      }
    });

    get(new Route("/movies/search/genre/:genre/:limit") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        String genre = request.params(":genre").toString();
        Integer limit = java.lang.Integer.parseInt(request.params(":limit"));
        try {
          return mapper.writeValueAsString(
            moviesList.stream()
              .filter(
                m -> m.get("Genre").toString().toLowerCase().contains(genre)
              ).limit(limit).toArray()
          );
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        return null;
      }
    });

    get(new Route("/movies/search/actors/:actors/:limit") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        String actors = request.params(":actors").toString();
        Integer limit = java.lang.Integer.parseInt(request.params(":limit"));
        try {
          return mapper.writeValueAsString(
            moviesList.stream()
              .filter(
                m -> m.get("Actors").toString().toLowerCase().contains(actors)
              ).limit(limit).toArray()
          );
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        return null;
      }
    });


  }
}
