'use strict';

const _ = require('lodash');
const base = require('../../config/base');
const env = require(`../../config/${process.env.NODE_ENV}`);
const cluster = require('./cluster');
const single = require('./single');
const koa = require('./koa');
const log4js = require('./log4js');
const mongooseClient = require('./mongoose-client');
const redisClient = require('./redis-client');
const redisServerClient = require('./redis-server-client');
const passport = require('./passport');
const schedule = require('./schedule');
const session = require('./session');
const socketIo = require('./socketio');

module.exports = _.merge(base, env, {
  koa,
  cluster,
  single,
  log4js,
  mongooseClient,
  redisClient,
  redisServerClient,
  passport,
  schedule,
  session,
  socketIo
});
