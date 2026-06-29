'use strict';

const Promise = require('bluebird');
const _ = require('lodash');
const fs = require('fs');
const request = require('request');

// 同时使用 co 与 bluebird 会导致 bluebird 警告
// https://github.com/tj/co/pull/256#issuecomment-168475913
Promise.config({
  warnings: false
});

module.exports = _.transform({
  fs,
  request
}, (result, value, key) => result[key] = Promise.promisifyAll(value));
