/** This script populates the database only with controller ids **/

/*jshint esversion: 6 */

var mongo = require('mongodb');
var MongoClient = require('mongodb').MongoClient;
const url = 'mongodb://localhost:27017/server-db';

MongoClient.connect(url, function(err, db) {
        if(err) {
        console.log("Error connecting do database");
    } else{
            db.dropDatabase(function(err, r) {
                if (err) throw err;

                        var collection = db.collection('agent');
                        collection.insertMany([{
                                    _id: new mongo.ObjectID("58357a84d53b63172ccc48a6"),
                                    username: 'controller1', 
                                    password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
                                    type: 'controller',
                                    name: 'Raspberry controller 1'
                            },
                            {
                                    _id: new mongo.ObjectID("58357a84d53b63172ccc48a7"),
                                    username: 'controller2', 
                                    password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
                                    type: 'controller',
                                    name: 'Raspberry controller 2'

                            },
                            {
                                    _id: new mongo.ObjectID("58357a84d53b63172ccc48a8"),
                                    username: 'controller3', 
                                    password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
                                    type: 'controller',
                                    name: 'Raspberry controller 3'                                  
                            },
                            {
                                    _id: new mongo.ObjectID("58357a84d53b63172ccc48a9"),
                                    username: 'controller4', 
                                    password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
                                    type: 'controller',
                                    name: 'Raspberry controller 1'                                  
                            }], function(err, r) {
                        if (err) throw err;
                        console.log(r);
                        db.close();
                            });
                });
    }
});