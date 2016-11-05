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
	                        		username: 'controller1', 
	                        		password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
	                        		type: 'controller', 
	                        		houseId: '1'
	                    	},
	                    	{
	                        		username: 'controller2', 
	                        		password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
	                        		type: 'controller', 
	                        		houseId: '2'
	                    	},
	                    	{
	                        		username: 'controller3', 
	                        		password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
	                        		type: 'controller', 
	                        		houseId: '3'
	                    	},
	                    	{
	                        		username: 'controller4', 
	                        		password: 'AYag$/HXl9rALel8=$+CwAzkg5hbEhUHJS5Y3Qy+INyiI=', 
	                        		type: 'controller', 
	                        		houseId: '3'
	                    	}], function(err, r) {
	        			if (err) throw err;
	        			console.log(r);
	        			db.close();
	                    	});
	        	});
	}
});