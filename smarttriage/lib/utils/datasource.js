'use strict';

const redis = require('./redis');
const oracleUtil = require('./oracle');
const mysqlUtil = require('./mysql');

const DEFAULT_DB_TYPE = 'oracle';

function getDbType(system, section) {
  if (section === 'pacs') {
    return system && system.pacsdbtype ? system.pacsdbtype : DEFAULT_DB_TYPE;
  }
  return system && system.dbtype ? system.dbtype : DEFAULT_DB_TYPE;
}

function pickByType(type, sql, section) {
  if (type && type.toLowerCase() === 'mysql') {
    return section === 'pacs' ? mysqlUtil.queryPacs(sql) : mysqlUtil.query(sql);
  }
  return section === 'pacs' ? oracleUtil.queryPacs(sql) : oracleUtil.query(sql);
}

function pickPing(type, section) {
  if (type && type.toLowerCase() === 'mysql') {
    return section === 'pacs' ? mysqlUtil.pingPacs() : mysqlUtil.ping();
  }
  return section === 'pacs' ? oracleUtil.queryPacs('select 1 from dual') : oracleUtil.query('select 1 from dual');
}

module.exports = {
  async query(sql) {
    const system = await redis.getValueFromRedis('system', 'hash');
    if (!system) {
      throw new Error('system config not found');
    }
    return pickByType(getDbType(system, 'default'), sql, 'default');
  },
  async queryPacs(sql) {
    const system = await redis.getValueFromRedis('system', 'hash');
    if (!system) {
      throw new Error('system config not found');
    }
    return pickByType(getDbType(system, 'pacs'), sql, 'pacs');
  },
  async ping() {
    const system = await redis.getValueFromRedis('system', 'hash');
    if (!system) {
      throw new Error('system config not found');
    }
    return pickPing(getDbType(system, 'default'), 'default');
  },
  async pingPacs() {
    const system = await redis.getValueFromRedis('system', 'hash');
    if (!system) {
      throw new Error('system config not found');
    }
    return pickPing(getDbType(system, 'pacs'), 'pacs');
  }
};
