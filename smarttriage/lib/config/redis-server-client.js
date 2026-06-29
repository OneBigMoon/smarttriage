'use strict';

const coRedis = require('co-redis');
const IoRedis = require('ioredis');

const Utils = require('../utils');
const logger = Utils.logger(__filename);

module.exports = (config) => {
  const client = coRedis(new IoRedis(config));

  client.promise = new Promise((resolve, reject) => {
    config.auth && client.auth(config.auth, (err) => {
      err && reject(err);
    });

    client.on('ready', () => {
      logger.info(`Redis ready on ${config.host}:${config.port}`);
      resolve(client);
    });

    client.on('connect', () => {
      const db = config.db || 0;
      logger.info(`Redis connected db${db}`);
      if (db) {
        client.send_anyways = true;
        client.select(db);
        client.send_anyways = false;
      }
    });

    client.on('reconnecting', () => logger.warn('Redis reconnecting'));

    client.on('error', (err) => logger.fatal('Redis error', err));
  });

  return client;
};
