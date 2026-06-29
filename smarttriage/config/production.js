'use strict';

module.exports = {
  mongo: {
    uri: 'mongodb://root:root1234@127.0.0.1:27017/smarttriage',
    options: {
      db: {
        safe: true
      },
      server: {
        poolSize: 200
      }
    }
  },
  redis: {
    host: '127.0.0.1',
    port: '6379',
    password: 'root1234',
    db: 0
  },
  cookie: {
    db: 0
  },
  log: {
    level: 'INFO'
  },
  static: {
    maxAge: 365 * 24 * 60 * 60
  }
};
