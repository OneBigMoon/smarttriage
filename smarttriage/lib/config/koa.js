'use strict';

const Koa = require('koa');
const staticCache = require('koa-static-cache');
const compress = require('koa-compress');
const convert = require('koa-convert');

// const bodyParser = require('koa-bodyparser');
const koaBody = require('koa-body');

const Utils = require('../utils');
const logger = Utils.logger(__filename);

module.exports = (config, session) => {
  const app = new Koa();

  app.keys = config.cookie.keys;

  app.use(convert(Utils.logger.middleware(logger, {
    format: ':method :status :url :response-time' + 'ms',
    level: 'auto'
  })));

  // koa-error v2 returns koa 1 generator middleware
  const errorMw = require('koa-error');
  app.use(convert(errorMw()));

  app.use(compress({ level: 6 }));

  app.use(koaBody({
    multipart: true,
    formLimit: '100mb',
    formidable: {
      uploadDir: 'uploads',
      keepExtensions: true
    }
  }));

  // app.use(config.session(config));
  if (session && session.constructor && session.constructor.name === 'GeneratorFunction') {
    app.use(convert(session));
  } else {
    app.use(session);
  }

  const passportMw = config.passport.middleware();
  if (passportMw && passportMw.constructor && passportMw.constructor.name === 'GeneratorFunction') {
    app.use(convert(passportMw));
  } else {
    app.use(passportMw);
  }

  if (staticCache.constructor && staticCache.constructor.name === 'GeneratorFunction') {
    app.use(convert(staticCache(config.dir.frontend, config.static)));
  } else {
    app.use(staticCache(config.dir.frontend, config.static));
  }

  return app;
};
