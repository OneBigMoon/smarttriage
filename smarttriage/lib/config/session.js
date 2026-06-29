'use strict';

const session = require('koa-generic-session');
const _ = require('lodash');
const redisStore = require('koa-session-ioredis');
const logger = require('../utils').logger(__filename);


module.exports = (config) => {
  const ioRetryStrategy = { // ioredis重连机制
    sentinelRetryStrategy(times1) {
      const delay = Math.min(times1 * 200, 2000);
      return delay;
    }
  };

  const storeInfo = config.redis.sentinels ? redisStore(_.assign({ db: config.cookie.db },
    ioRetryStrategy, config.redis)) : redisStore({
      host: config.redis.host,
      port: config.redis.port,
      password: config.redis.password,
      db: config.cookie.db
    });
  return session({
    key: config.app.name,
    prefix: config.cookie.prefix,
    allowEmpty: false,
    cookie: {
      maxage: config.cookie.expire
    },
    store: storeInfo,
    errorHandler(err) {
      logger.error('Session Error', err);
    }
  });
};
