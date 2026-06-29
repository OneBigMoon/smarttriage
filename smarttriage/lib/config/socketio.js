'use strict';

const _ = require('lodash');
const socketio = require('socket.io');
const socketioRedis = require('socket.io-redis');
const socketioEmitter = require('socket.io-emitter');
const redis = require('redis').createClient;
const Utils = require('../utils');
const logger = Utils.logger(__filename);

module.exports = (config, server) => {
  const retryStrategy = { // 原生reis重连机制
    retry_strategy(options) {
      if (options.total_retry_time > 1000 * 60 * 60) { // 超时后结束重连，抛出异常让process重启
        logger.info('redis error total_retry_time');
        return new Error('Retry time exhausted');
      }
      // reconnect after
      return Math.min(options.attempt * 200, 2000);
    }
  };

  const io = socketio(server);
  const pubClient = redis(_.assign({
    db: config.cookie.db,
    return_buffers: true
  }, retryStrategy, config.redis));
  const subClient = redis(_.assign({
    db: config.cookie.db,
    return_buffers: true
  }, retryStrategy, config.redis));
  const emitterClient = redis(_.assign({
    db: config.cookie.db,
    return_buffers: true
  }, retryStrategy, config.redis));
  _.each({
    pubClient,
    subClient,
    emitterClient
  }, (client, name) => {
    client.on('connect', () => logger.info(`Socket.io ${name} connected db${config.cookie.db}`));

    client.on('reconnecting', () => logger.warn(`Socket.io ${name} reconnecting`));

    client.on('error', (err) => logger.fatal(`Socket.io ${name} error`, err));
  });
  io.adapter(socketioRedis({
    pubClient,
    subClient
  }));
  Utils.socketio.emitter = socketioEmitter(emitterClient);
  return function(socketRoutes) {
    _.each(socketRoutes, function(routes, path) {
      const namespace = io.of('/' + path);
      namespace.on('connect', function(socket) {
        (async () => {
          if (routes.connect) {
            try {
              await routes.connect(socket, namespace);
            } catch (err) {
              logger.error('Socket.io error', {
                namespace: path,
                event: 'connect'
              }, err, this);
            }
          }
          _.each(routes, function(route, eventType) {
            if (eventType != 'connect') {
              socket.on(eventType, function(data) {
                (async () => {
                  if (data == null) {
                    data = {};
                  }
                  await route(data, socket, namespace);
                })().catch((err) => logger.error('Socket.io error', {
                  namespace: path,
                  event: eventType
                }, err, this));
              });
            }
          });
        })();
      });
    });
  };
};
