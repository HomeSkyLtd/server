/*jshint esversion: 6 */

var mongo = require('mongodb');
var MongoClient = require('mongodb').MongoClient;
const url = 'mongodb://localhost:27017/server-db';

MongoClient.connect(url, function(err, db) {
    if(err) console.log("Error connecting do database");
    else{
        db.dropDatabase(function(err, r) {
            if (err) throw err;

            var collection = db.collection('agent');
            collection.insertMany(
                [
                    {
                        username: 'admin1', 
                        password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
                        type: 'admin', 
                        houseId: '1'
                    },
                    {
                        username: 'controller1', 
                        password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
                        type: 'controller', 
                        houseId: '1'
                    }
                ], function(err, r) {
                    if (err) throw err;

                    collection = db.collection('rules_1');
                    collection.insertMany([
                        {
                            command:{
                                nodeId: 1,
                                commandId: 1,
                                value: 20
                            },
                            controllerId: "1",
                            clauses: [[{lhs: '1.1', operator: '>', rhs: 10}]],
                            accepted: 1
                        }, 
                        {
                            command:{
                                nodeId: 2,
                                commandId: 1,
                                value: 0,
                            },
                            controllerId: "1",
                            clauses: [[{lhs: '2.1', operator: '==', rhs: 0}]],
                            accepted: 0
                        }], 
                        function(err, r) {
                            if (err) throw err;

                            collection = db.collection('node_1');
                            collection.insertMany([
                                {
                                    nodeId: 1,
                                    controllerId: "1",
                                    nodeClass: 1,
                                    accepted: 1,
                                    alive: 1,
                                    extra: {name: 'Room sensor', color: 'red'},
                                    dataType: [{
                                        id: 1,
                                        measureStrategy: 2,
                                        type: 1,
                                        dataCategory: 1,
                                        unit: 'ºC',
                                        range: [-20, 50]
                                    }],
                                    commandType: []
                                }, 
                                {
                                    nodeId: 2,
                                    controllerId: "1",
                                    nodeClass: 2,
                                    accepted: 0,
                                    alive: 1,
                                    extra: {name: 'Room actuator', color: 'blue'},
                                    dataType: [],
                                    commandType: [{
                                        id: 1,
                                        type: 2,
                                        commandCategory: 4,
                                        unit: '',
                                        range: [0, 1]
                                    }]
                                }
                            ], function(err, r) {
                                if (err) throw err;
                                var collection = db.collection('last_states_1');
                                collection.insertMany([
                                    {
                                        nodeId: 1,
                                        controllerId: "1",
                                        data:{
                                            1: 1.5
                                        }
                                    },
                                    {
                                        nodeId: 2,
                                        controllerId: "1",
                                        command:{
                                            1: 3
                                        }
                                    }
                                ], (err, r)=>{
                                    if(err) throw err;
                                    db.close();
                                });
                            });
                        });
                });
        });
    }
});
