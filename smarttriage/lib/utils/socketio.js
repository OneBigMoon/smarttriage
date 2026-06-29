'use strict';

const socketio = module.exports = function(name) {
  const namespace = name ? socketio.emitter.of('/' + name) : socketio.emitter;
  return namespace;
};
