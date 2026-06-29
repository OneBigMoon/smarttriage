'use strict';

const oracledb = require('oracledb');
const logger = require('./logger')(__filename);
const redis = require('./redis');

oracledb.fetchAsBuffer = [oracledb.BLOB];
oracledb.fetchAsString = [oracledb.CLOB];

let username = '';
let password = '';
let url = '';
let usernameUltrasonic = '';
let passwordUltrasonic = '';

const doRelease = function(connection) {
  connection.release(
    function(err) {
      if (err) {
        logger.error(err);
      }
    });
};

const getPool = async function() {
  const system = await redis.getValueFromRedis('system', 'hash');
  if (!system) {
    throw new Error('system config not found');
  }
  return new Promise((resolve, reject) => {
    if (system.username != username || system.password != password || system.url != url) {
      username = system.username;
      password = system.password;
      url = system.url;
      logger.info('初始化连接池');
      oracledb.createPool({
        user: username,
        password,
        connectString: url,
        poolMax: 30,
        poolMin: 15,
        poolAlias: 'oraclePool'
      }, function(err) {
        if (err) {
          logger.error(err);
          reject(err);
        } else {
          logger.info('初始化连接池成功');
          resolve(oracledb.getPool('oraclePool'));
        }
      });
    } else {
      try {
        const oraclePool = oracledb.getPool('oraclePool');
        resolve(oraclePool);
      } catch (err) {
        logger.warn('等待连接池初始化');
        reject(err);
      }
    }
  });
};

const getPoolUltrasonic = async function() {
  const system = await redis.getValueFromRedis('system', 'hash');
  if (!system) {
    throw new Error('system config not found');
  }
  return new Promise((resolve, reject) => {
    if (system.pacsusername != usernameUltrasonic || system.pacspassword != passwordUltrasonic || system.url != url) {
      usernameUltrasonic = system.pacsusername;
      passwordUltrasonic = system.pacspassword;
      url = system.url;
      logger.info('初始化PACS连接池');
      oracledb.createPool({
        user: usernameUltrasonic,
        password: passwordUltrasonic,
        connectString: url,
        poolMax: 20,
        poolMin: 5,
        poolAlias: 'oraclePoolUltrasonic'
      }, function(err) {
        if (err) {
          logger.error(err);
          reject(err);
        } else {
          logger.info('初始化PACS连接池成功');
          resolve(oracledb.getPool('oraclePoolUltrasonic'));
        }
      });
    } else {
      try {
        const oraclePoolUltrasonic = oracledb.getPool('oraclePoolUltrasonic');
        resolve(oraclePoolUltrasonic);
      } catch (err) {
        logger.warn('等待PACS连接池初始化');
        reject(err);
      }
    }
  });
};

module.exports = {
  async query(sql) {
    return new Promise((resolve, reject) => {
      getPool().then(function(oraclePool) {
        oraclePool.getConnection(function(err, connection) {
          if (err) {
            reject(err);
            return;
          }
          const startDate = new Date().getTime();
          connection.execute(sql, function(err2, result) {
            if (err2) {
              doRelease(connection);
              reject(err2);
              return;
            }
            doRelease(connection);
            const endDate = new Date().getTime();
            resolve(result.rows);
            logger.info(`${sql}, exec sql span time ${(endDate - startDate) / 1000} seconds`);
          });
        });
      }).catch(function(err) {
        if (err) {
          logger.error(err);
        }
        reject(err);
      });
    });
  },
  async queryPacs(sql) {
    return new Promise((resolve, reject) => {
      getPoolUltrasonic().then(function(oraclePoolUltrasonic) {
        oraclePoolUltrasonic.getConnection(function(err, connection) {
          if (err) {
            reject(err);
            return;
          }
          connection.execute(sql, function(err2, result) {
            if (err2) {
              doRelease(connection);
              reject(err2);
              return;
            }
            doRelease(connection);
            resolve(result.rows);
          });
          });
      }).catch(function(err) {
        if (err) {
          logger.error(err);
        }
        reject(err);
      });
    });
  }
};
