/**
 * socket.io client
 */

var io = require('socket.io-client');
var fs = require('fs');


var socket = io('http://101.132.39.30:7016/socketio', {
//var socket = io('http://127.0.0.1:7016/socketio', {
  query: {
    ip: "192.168.1.100"
  },
  transports: ['websocket']
});

socket.on('connect', function() {
  console.info(111);
  setInterval(() => {
    socket.emit('apiv1_heartbeat', {
      "type": "HEARTBEAT",
      "content": {
        "ip": "192.168.1.101"
      }
    });
  }, 10000);
  // setTimeout(() => {
  //   socket.emit('apiv1_message', {
  //     "type": "CONFIG",
  //     "content": {
  //       "datasource":"5b6ec0c6faaad51c8993d5fc",
  //       "status":"关机"
  //     }
  //   });
  // }, 5000);
});

socket.on('apiv1_message', function(data) {
  console.info(data)
});
