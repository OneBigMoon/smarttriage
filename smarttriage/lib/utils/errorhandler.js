'use strict';

const _ = require('lodash');
const logger = require('./logger')(__filename);

module.exports = (errorLogger) => {
  errorLogger = errorLogger || logger;

  return function() {
    const msgs = [];
    const errs = [];

    let status;
    let ctx;

    Array.prototype.slice.call(arguments).forEach((arg) => {
      if (_.isString(arg)) {
        msgs.push(arg);
      } else if (_.isNumber(arg)) {
        status = arg;
      } else if (arg != null && _.isObject(arg)) {
        if (arg.originalUrl) {
          ctx = arg;
        }
        errs.push(arg);
      }
    });

    status = status || 500;

    if (errs.length) {
      msgs.push('\n');
    }

    errorLogger.error.apply(errorLogger, msgs.concat(errs));

    if (ctx) {
      ctx.status = status;
      ctx.body = msgs[0] && { message: msgs[0] };
    }
  };
};
