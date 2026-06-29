'use strict';

const http = require('http');
const _ = require('lodash');
const fs = require('fs');
const Utils = require('../utils');
const logger = Utils.logger(__filename);

module.exports = (config) => {
  const cluster = require('cluster');
  const net = require('net');
  const processCount = require('os').cpus().length;

  // node-cluster-socket.io
  // @see https://github.com/elad/node-cluster-socket.io
  if (cluster.isMaster) {
    const workers = [];
    const spawn = (index) => {
      const worker = workers[index] = cluster.fork();

      worker.on('listening', (address) => {
        logger.info(`Worker ${index} start on pid ${worker.process.pid} port ${address.port}`);
      });

      // Restart worker on exit
      worker.on('exit', (code, signal) => {
        logger.error(`Worker ${index} pid ${worker.process.pid} died (${signal || code}). restarting...`);
        spawn(index);
      });
    };
    const workerIndex = (ip) => {
      return Number(ip ? ip.replace(/[^\d]/g, '') : 0) % processCount;
    };

    // Spawn workers.
    _.times(processCount, (i) => spawn(i));

    net.createServer({ pauseOnConnect: true }, (connection) => {
      const worker = workers[workerIndex(connection.remoteAddress)];
      worker.send('sticky-session:connection', connection);
    }).listen(config.port, '0.0.0.0');

    !fs.existsSync(config.dir.upload) && fs.mkdirSync(config.dir.upload);
  } else {
    const session = config.session(config);

    // Server
    const app = module.exports = config.koa(config, session);

    // Mongoose
    const mongooseClient = config.mongooseClient(config.mongo);
    // Redis
    const redisClient = config.redisClient(config.redis);

    Promise.all([
      new Promise((resolve, reject) => {
        const server = http.createServer(app.callback());
        server.listen(0, 'localhost', (err) => {
          if (err) {
            return reject(err);
          }
          logger.info(`Koa listening on port ${config.https_port}`);
          resolve(server);
        });
      }),
      mongooseClient.promise,
      redisClient.promise
    ])
      .then((modules) => {
        const server = modules[0];

        // Listen to messages sent from the master. Ignore everything else
        process.on('message', function(message, connection) {
          if (message != 'sticky-session:connection') {
            return;
          }

          // Emulate a connection event on the server by emitting the
          // event with the connection the master sent us.
          server.emit('connection', connection);

          connection.resume();
        });

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
  }
};
