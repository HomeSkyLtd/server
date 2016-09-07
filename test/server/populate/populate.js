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
            collection.insertOne({
                username: 'admin1', 
                password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
                type: 'admin', 
                houseId: '1'}, function(err, r) {
                    if (err) throw err;

                    collection = db.collection('rules_1');
                    collection.insertMany([
                        {
                            nodeId: 1,
                            controllerId: "1",
                            commandId: 1,
                            value: 20,
                            clauses: [[{lhs: '1.1', operator: '>', rhs: 10}]],
                            accepted: true
                        }, 
                        {
                            nodeId: 2,
                            controllerId: "1",
                            commandId: 1,
                            value: 0,
                            clauses: [[{lhs: '2.1', operator: '==', rhs: 0}]],
                            accepted: false
                        }], 
                        function(err, r) {
                            if (err) throw err;

                            collection = db.collection('node_1');
                            collection.insertMany([
                                {
                                    nodeId: 1,
                                    controllerId: "1",
                                    nodeClass: 'sensor',
                                    accepted: 1,
                                    alive: 1,
                                    extra: {name: 'Room sensor', color: 'red'},
                                    dataType: [{
                                        id: 1,
                                        measureStrategy: 'periodic',
                                        type: 'int',
                                        dataCategory: 'temperature',
                                        unit: 'ºC',
                                        range: [-20, 50]
                                    }],
                                    commandType: []
                                }, 
                                {
                                    nodeId: 2,
                                    controllerId: "1",
                                    nodeClass: 'actuator',
                                    accepted: 0,
                                    alive: 1,
                                    extra: {name: 'Room actuator', color: 'blue'},
                                    dataType: [],
                                    commandType: [{
                                        id: 1,
                                        type: 'bool',
                                        commandCategory: 'lightSwitch',
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
                                        controllerId: "2",
                                        data:{
                                            1: 1.5,
                                            2: 3
                                        }
                                    },
                                    {
                                        nodeId: 2,
                                        controllerId: "2",
                                        command:{
                                            1: 3,
                                            2: 10
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
