/**
 * socket.io client
 */

var io = require('socket.io-client');
var fs = require('fs');


// var socket = io('http://localhost:3000/sccs', {
var socket = io('http://zft.zft518.com', {
  transports: ['websocket']
});

socket.on('connect', function() {
  console.info(111);
});

socket.on('apiv1_logscreen_message', function(data) {
  console.info(JSON.stringify('apiv1_logscreen_message'))
  console.info(JSON.stringify(data))
});

socket.on('apiv1_nursinghost_message', function(data) {
  console.info(JSON.stringify('apiv1_nursinghost_message'))
  console.info(JSON.stringify(data))
});

socket.on('apiv1_bedsidecard_message', function(data) {
  console.info(JSON.stringify('apiv1_bedsidecard_message'))
  console.info(JSON.stringify(data))
});

socket.on('res_ticket_scqtoscc', function(data) {
});

socket.on('lala', function(data) {
  console.info(data)
});
