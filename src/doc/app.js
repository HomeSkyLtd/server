var express = require('express');
var app = express();
var path = require('path');

var rel_path = '../../target/doc';

app.use(express.static(path.join(__dirname, rel_path)));

app.get('/', function(req, res) {
    res.sendFile(path.join(__dirname, rel_path, 'index.html'));
});

app.listen(8080, function() {
	console.log('Listening on port 8080');
});