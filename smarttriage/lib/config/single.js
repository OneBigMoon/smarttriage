'use strict';

const http = require('http');
const fs = require('fs');
const Utils = require('../utils');
const logger = Utils.logger(__filename);

module.exports = (config) => {
  // Session
  const session = config.session(config);

  // Server
  const app = module.exports = config.koa(config, session);

  // Mongoose
  const mongooseClient = config.mongooseClient(config.mongo);
  // Redis
  const redisClient = config.redisClient(config.redis);

  !fs.existsSync(config.dir.upload) && fs.mkdirSync(config.dir.upload);

  Promise.all([
    new Promise((resolve, reject) => {
      const server = http.createServer(app.callback());
      server.listen(config.port, '0.0.0.0', (err) => {
        if (err) {
          return reject(err);
        }
        logger.info(`Koa listening on port ${config.port}`);
        resolve(server);
      });
    }),
    mongooseClient.promise,
    redisClient.promise
  ])
    .then((modules) => {
      const server = modules[0];

      // i18n
      require('../locales')(config);

      // Models
      require('../models')(mongooseClient);

      // redis
      Utils.redis.initRedis(redisClient);

      // Routes
      require('../route')(app);

      // Socket.io
      config.socketIo(config, server, session)(require('../socketio'));

      // Schdules
      config.schedule(config);

      logger.info(`${Utils.upperCamelCase(config.app.name)} start success`);
    })
    .catch((err) => logger.fatal(`${Utils.upperCamelCase(config.app.name)} start failed`, err));
};
