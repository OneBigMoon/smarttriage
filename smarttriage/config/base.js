'use strict';

const path = require('path');
const rootPath = path.normalize(__dirname + '/..');
const app = require('../package.json');

module.exports = {
  port: process.env.PORT || 7016,
  https_port: 7116,
  lang: 'en',
  dir: {
    root: rootPath,
    frontend: path.join(rootPath, 'app'),
    upload: path.join(rootPath, 'uploads'),
    backend: path.join(rootPath, 'lib'),
    log: path.join(rootPath, 'logs'),
    build: path.join(rootPath, 'build'),
    ssl: path.join(rootPath, 'certificate/ssl'),
    cert: path.join(rootPath, 'certificate/cert'),
    static: path.join(rootPath, 'app/static')
  },
  cookie: {
    keys: ['37b0f2d26423d54e47cf557ae9432e54', '7cea19006502379a0198283e6a17640d'],
    expire: 60000 * 60 * 12,
    prefix: 'sc:'
  },
  pwdkey: 'smarttriage1234',
  app
};
