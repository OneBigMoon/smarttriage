'use strict';

module.exports = {
  mongo: {
    uri: 'mongodb://127.0.0.1:27017/smarttriage',
    options: {
      db: {
        safe: true
      },
      server: {
        poolSize: 50
      }
    }
  },
  redis: {
    host: '127.0.0.1',
    port: '6379',
    db: 0
  },
  cookie: {
    db: 0
  },
  log: {
    level: 'TRACE'
  },
  static: {
    maxAge: 0
  }
};
