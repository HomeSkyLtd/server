var express = require('express');
var app = express();
var path = require('path');

var rel_path = '../../target/doc';

app.use(express.static(path.join(__dirname, rel_path)));

app.get('/', function(req, res) {
    console.log('GET request');
    res.sendFile(path.join(__dirname, rel_path, 'index.html'));
});

var port = 8080;

app.listen(port, function() {
	console.log(`Listening on port ${port}`);
});
