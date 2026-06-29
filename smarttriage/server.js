'use strict';

// Set default node environment to production
const env = process.env.NODE_ENV = process.env.NODE_ENV || 'production';

const dir = require('./config/base').dir;

const Config = require(`${dir.backend}/config`);
const Utils = require(`${dir.backend}/utils`);

// Log4js
Config.log4js(Config);
const logger = Utils.logger(__filename);

Config[env == 'development' ? 'cluster' : 'cluster'](Config);

// 处理未捕获异常
process.on('uncaughtException', (err) => {
  logger.fatal('uncaughtException', err); // may not printed with pm2
  process.nextTick(() => process.exit(1));
});
