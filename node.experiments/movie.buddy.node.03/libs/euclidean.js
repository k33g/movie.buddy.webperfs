/* Euclidean version */
var mathjs = require('mathjs')
  , math = mathjs()

var preco = {
  
  sharedPreferences : function (preferences, user1, user2) {
    var shared_preferences = [];
    for (var item in preferences[user1]) {
      if (preferences[user2][item]) { /* si la preference existe chez les 2 */
        shared_preferences.push(+item);
      }
    };
    return shared_preferences;
  },

  /*Euclidean distance*/
  distance : function (preferences, user1, user2) {
    
    var shared_preferences = this.sharedPreferences(preferences, user1, user2)
      , sum_of_squares = 0;

    if (shared_preferences.length == 0) return 0;
    //shared_preferences = Array.prototype.slice.apply(shared_preferences);

    shared_preferences.forEach(function (item) {
      //console.log("--> ", item, " : ",  math.pow(preferences[user1][item] - preferences[user2][item],2))
      sum_of_squares += math.pow(preferences[user1][item] - preferences[user2][item],2);
    });
    //console.log("distance" ,1/(1 + math.sqrt(sum_of_squares)))
    return 1/(1 + math.sqrt(sum_of_squares));
  },

  findTheNearest : function (preferences, user1) {
    var virtual_distance = 1000, distance = null, nearest_user = { user:null, distance:null};
    for (var user in preferences) {
      if (user != user1) {
        distance = this.distance(preferences, user1, user);
        if (distance!=0){
          if (distance < virtual_distance) {
            virtual_distance = distance;
            nearest_user.user = user;
            nearest_user.distance = distance;
          }
        }
      }
    }
    return nearest_user;
  },

  findTheNearestInRange : function (range, preferences, user1) {
    var virtual_distance = 1000, distance = null, nearest_user_in_range = { user:null, distance:null};
    var that = this;
    range.forEach(function(user) {
      distance = that.distance(preferences, user1, user);
      if (distance!=0){
        if (distance < virtual_distance) {
          virtual_distance = distance;
          nearest_user_in_range.user = user;
          nearest_user_in_range.distance = distance;
        }
      }
    });

    return nearest_user_in_range;
  },
  
  findTheFarthest : function (preferences, user1) {
    var virtual_distance = 0, distance = null, farthest_user = { user:null, distance:null};
    for (var user in preferences) {
      if (user != user1) {
        distance = this.distance(preferences, user1, user);
        if (distance > virtual_distance) {
          virtual_distance = distance;
          //farthest_user = user;
          farthest_user.user = user;
          farthest_user.distance = distance;
        }
      }
    }
    return farthest_user;      
  }

}

module.exports = preco;