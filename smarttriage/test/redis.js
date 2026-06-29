var redis = require('redis');
var fs = require('fs');

var client = redis.createClient(6379,'localhost');

client.hget('secondary:120','data',function(e,v) {
  console.log(JSON.parse(v)[0][6]);
        fs.writeFile('D:/a.jpg', new Buffer(JSON.parse(v)[0][6].data), function(err) {
          if (err) {
            console.log(err)
          }
        });
});
