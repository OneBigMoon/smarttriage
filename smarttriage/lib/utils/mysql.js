'use strict';

const mysql = require('mysql');
const crypto = require('crypto');
const _ = require('lodash');
const logger = require('./logger')(__filename);
const redis = require('./redis');

let pools = {};
const MYSQL_REQUIRED_FIELDS = ['host', 'user', 'database'];

function normalizePoolConfig(cfg) {
  return _.assign({
    connectionLimit: 20,
    waitForConnections: true,
    rowsAsArray: true,
    charset: 'utf8mb4'
  }, cfg);
}

function cfgHash(cfg) {
  return crypto.createHash('md5').update(JSON.stringify(_.pick(cfg, ['host', 'port', 'user', 'database', 'password']))).digest('hex');
}

function ensurePool(name, cfg) {
  const hash = cfgHash(cfg);
  const current = pools[name];
  if (current && current.hash === hash) {
    return current.pool;
  }
  if (current && current.pool && current.pool.end) {
    current.pool.end();
  }
  const pool = mysql.createPool(cfg);
  pools[name] = {
    hash,
    pool
  };
  return pool;
}

function buildDefaultPoolConfig(system) {
  return normalizePoolConfig({
    host: system.mysqlhost || '127.0.0.1',
    port: Number(system.mysqlport || 3306),
    database: system.mysqldatabase || '',
    user: system.mysqlusername || '',
    password: system.mysqlpassword || ''
  });
}

function buildPacsPoolConfig(system) {
  return normalizePoolConfig({
    host: system.pacsmysqlhost || system.mysqlhost || '127.0.0.1',
    port: Number(system.pacsmysqlport || system.mysqlport || 3306),
    database: system.pacsmysqldatabase || system.mysqldatabase || '',
    user: system.pacsmyusername || system.mysqlusername || '',
    password: system.pacsmypassword || system.mysqlpassword || ''
  });
}

function queryInternal(section, sql) {
  return redis.getValueFromRedis('system', 'hash').then((system) => {
    if (!system) {
      throw new Error('system config not found');
    }

    const cfg = section === 'pacs' ? buildPacsPoolConfig(system) : buildDefaultPoolConfig(system);
    if (_.some(MYSQL_REQUIRED_FIELDS, key => _.isEmpty(cfg[key]))) {
      throw new Error('MySQL config missing required fields');
    }

    return new Promise((resolve, reject) => {
      const pool = ensurePool(section, cfg);
      pool.getConnection((err, conn) => {
        if (err) {
          logger.error('MySQL getConnection error', err);
          reject(err);
          return;
        }
        conn.query({ sql, rowsAsArray: true }, (err2, rows) => {
          conn.release();
          if (err2) {
            reject(err2);
            return;
          }
          resolve(rows || []);
        });
      });
    });
  });
}

function pingInternal(section) {
  return queryInternal(section, 'select 1');
}

module.exports = {
  query(sql) {
    return queryInternal('default', sql);
  },
  queryPacs(sql) {
    return queryInternal('pacs', sql);
  },
  ping() {
    return pingInternal('default');
  },
  pingPacs() {
    return pingInternal('pacs');
  }
};
