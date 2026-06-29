'use strict';

const path = require('path');
const Promise = require('bluebird');
const _ = require('lodash');
const log4js = require('log4js');
const moment = require('moment');
const targz = require('targz');
const compress = Promise.promisify(targz.compress);

const Utils = require('../utils');
const fs = Utils.fs;

const ALL_LEVELS = ['trace', 'debug', 'info', 'warn', 'error', 'fatal'];

function ctxParser(event) {
  let result = '';
  let ctx;
  let params;

  if (event.request) {
    ctx = event.request;
  } else if (_.isArray(event.data)) {
    event.data.forEach((arg, i) => {
      if (arg && _.isObject(arg) && _.isString(arg.originalUrl)) {
        ctx = event.req = arg;
        event.data.splice(i, 1);
      }
    });
  }

  if (ctx) {
    params = _.pick(ctx, [
      'params',
      'query',
      'body',
      'headers'
    ]);
    params.body = params.body || ctx.request.body;
    if (ctx.user) {
      params.user = ctx.user.name;
    }

    result = `[${ctx.method} ${ctx.originalUrl} ${JSON.stringify(params)}]`;
  }

  return result;
}

function configure(config) {
  configure.logDir = config.dir.log;
  configure.appName = config.app.name;

  const appenders = [
    {
      type: 'console',
      layout: {
        type: 'pattern',
        pattern: '%[[%d] [%p] [%c] %x{ctx}%] %m',
        tokens: {
          ctx: ctxParser
        }
      }
    }
  ];

  ALL_LEVELS.slice(ALL_LEVELS.indexOf(config.log.level.toLowerCase()))
    .forEach((level) => {
      appenders.push({
        type: 'logLevelFilter',
        level: level.toUpperCase(),
        maxLevel: level.toUpperCase(),
        appender: {
          type: 'dateFile',
          filename: path.join(configure.logDir, `${configure.appName}-${level}`),
          pattern: '-yyyy-MM-dd.log',
          alwaysIncludePattern: true,
          layout: {
            type: 'pattern',
            pattern: '[%d] [%p] [%c] %x{ctx} %m',
            tokens: {
              ctx: ctxParser
            }
          }
        }
      });
    });

  log4js.configure({
    levels: {
      '[all]': config.log.level
    },
    appenders
  });
}

configure.cleanLogs = () => {
  const logger = Utils.logger(__filename);

  function deleteLogs() {
    // 日志保留一个月
    const keepDate = moment().add(-1, 'month').format('YYYY-MM-DD');

    return fs.readdirAsync(configure.logDir)
      .then((files) => {
        const re = /.*-(20\d\d-\d\d-\d\d)\.log.*/;

        return Promise.all(_.map(files, (file) => {
          const matches = re.exec(file);
          if (matches != null && matches[1] < keepDate) {
            logger.info(`Logs delete: ${file}`);

            return fs.unlinkAsync(path.join(configure.logDir, file));
          }
        }));
      })
      .then(() => logger.info(`Logs deleted before ${keepDate}`))
      .catch((err) => logger.error(`Logs delete before ${keepDate} error`, err));
  }

  function zipLogs() {
    // 压缩今天之前的日志
    const today = moment().format('YYYY-MM-DD');

    return fs.readdirAsync(configure.logDir)
      .then((files) => {
        const re = /.*-(20\d\d-\d\d-\d\d)\.log$/;

        files = _.groupBy(files, (file) => {
          const matches = re.exec(file);
          return matches ? matches[1] : '';
        });

        return Promise.all(_.map(files, (file, day) => {
          if (day && day < today) {
            logger.info(`Logs compress: ${file.join(' ')}`);

            return compress({
              src: configure.logDir,
              dest: path.join(configure.logDir, `${configure.appName}-${day}.log.tar.gz`),
              tar: {
                entries: file.slice(0)
              },
              gz: {
                level: 9,
                memLevel: 9
              }
            })
              .then(() => file)
              .map((f) => fs.unlinkAsync(path.join(configure.logDir, f)));
          }
        }));
      })
      .then(() => logger.info(`Logs compressed before ${today}`))
      .catch((err) => logger.error(`Logs compress before ${today} error`, err));
  }

  return deleteLogs().then(zipLogs);
};

module.exports = configure;
