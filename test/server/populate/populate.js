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
                                nodeId: 4,
                                commandId: 1,
                                value: 0,
                            },
                            controllerId    : "1",
                            clauses: [[{lhs: '3.1', operator: '==', rhs: 0}]],
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
                                    extra: {name: 'Temperature Sensor', room: 'Kitchen'},
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
                                    nodeId: 3,
                                    controllerId: "1",
                                    nodeClass: 1,
                                    accepted: 1,
                                    alive: 1,
                                    extra: {name: 'Presence', room: 'Kitchen'},
                                    dataType: [{
                                        id: 1,
                                        measureStrategy: 2,
                                        type: 2,
                                        dataCategory: 3,
                                        unit: 'on/off',
                                        range: [0, 1]
                                    }],
                                    commandType: []
                                },
                                {
                                    nodeId: 2,
                                    controllerId: "1",
                                    nodeClass: 2,
                                    accepted: 0,
                                    alive: 1,
                                    extra: {name: 'Light Switch', room: 'John\'s Bedroom'},
                                    dataType: [],
                                    commandType: [{
                                        id: 1,
                                        type: 2,
                                        commandCategory: 4,
                                        unit: 'on/off',
                                        range: [0, 1]
                                    }]
                                }, 
                                {
                                    nodeId: 5,
                                    controllerId: "1",
                                    nodeClass: 3,
                                    accepted: 0,
                                    alive: 1,
                                    extra: {name: 'Air Conditioner', room: 'Living Room'},
                                    dataType: [{
                                        id: 1,
                                        measureStrategy: 2,
                                        type: 1,
                                        dataCategory: 1,
                                        unit: 'ºC',
                                        range: [-20, 50]
                                    }],
                                    commandType: [{
                                        id: 1,
                                        type: 1,
                                        commandCategory: 2,
                                        unit: 'ºC',
                                        range: [-20, 50]
                                    },
                                    {
                                        id: 2,
                                        type: 1,
                                        commandCategory: 3,
                                        unit: 'intensity',
                                        range: [0, 3]
                                    }]
                                },
                                {
                                        nodeId: 4,
                                        controllerId: "1",
                                        nodeClass: 2,
                                        accepted: 0,
                                        alive: 1,
                                        extra: {name: 'Light Switch', room: 'Bathroom'},
                                        dataType: [],
                                        commandType: [{
                                            id: 1,
                                            type: 2,
                                            commandCategory: 4,
                                            unit: 'on/off',
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
                                            1: 30.0
                                        }
                                    },
                                    {
                                        nodeId: 2,
                                        controllerId: "1",
                                        command:{
                                            1: 0
                                        }
                                    },
                                    {
                                        nodeId: 1,
                                        controllerId: "2",
                                        data: {
                                            1: 29.8
                                        },
                                        command:{
                                            1: 20.0,
                                            2: 2
                                        }
                                    }, 
                                    {
                                        nodeId: 3,
                                        controllerId: "1",
                                        data: {
                                            1: 1
                                        }
                                    }, 
                                    {
                                        nodeId: 4,
                                        controllerId: "1",
                                        command: {
                                            1: 1
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
