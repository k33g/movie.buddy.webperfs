import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/*
 User: k33g_org
 Date: 06/04/14
 Time: 15:38
*/
public class Preco {
  public Stream<Double> sharedPreferences(HashMap<Double, HashMap<Double, Double>> reviews, Double user1Id, Double user2Id) {
    return reviews.get(user1Id).keySet().stream().filter(m -> reviews.get(user2Id).get(m) !=null);
  }

  public HashMap<String, Double> distance(HashMap<Double, HashMap<Double, Double>> reviews, Double user1Id, Double user2Id) {
    Stream<Double> shared_preferences = this.sharedPreferences(reviews, user1Id, user2Id);
    //System.out.println("distance");

    Double[] sum_of_squares = { 0.0 };
    shared_preferences.forEach(m -> {
      sum_of_squares[0] += Math.pow(reviews.get(user1Id).get(m) - reviews.get(user2Id).get(m), 2.0);
    });

    //System.out.println(sum_of_squares[0]);

    return new HashMap<String, Double>(){{
      put("distance", 1/(1 + java.lang.Math.sqrt(sum_of_squares[0])));
    }};


    /*
    if (shared_preferences.toArray().length == 0) {
      return new HashMap<String, Double>(){{
        put("distance", 0.0);
      }};
    } else {
      Double[] sum_of_squares = { 0.0 };
      shared_preferences.forEach(m -> {
        sum_of_squares[0] += Math.pow(reviews.get(user1Id).get(m) - reviews.get(user2Id).get(m), 2.0);
      });

      System.out.println(sum_of_squares[0]);

      return new HashMap<String, Double>(){{
        put("distance", 1/(1 + java.lang.Math.sqrt(sum_of_squares[0])));
      }};
    }
    */
  }
}

