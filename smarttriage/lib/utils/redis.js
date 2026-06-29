'use strict';

const _ = require('lodash');
let client;

const redisUtils = module.exports = {
  initRedis(cli) {
    client = cli;
  },
  async getValueFromRedis(key, type) {
    if (type == 'string') {
      const value = await client.get(key);
      return value;
    } else if (type == 'hash') {
      const value = await client.hgetall(key);
      if (value && _.keys(value).length > 0) {
        return value;
      }
      return null;
    }
  },
  async getFieldValueFromHashRedis(key, field) {
    const value = await client.hget(key, field);
    if (value) {
      return value;
    }
    return null;
  },
  async getSetLengthFromRedis(key) {
    const value = await client.scard(key);
    if (value) {
      return value;
    }
    return 0;
  },
  async setValueToRedis(key, value, type) {
    if (type == 'string') {
      await client.set(key, value);
    } else if (type == 'hash') {
      await client.hmset(key, value);
    } else if (type == 'set') {
      await client.sadd(key, value);
    }
  },
  async getKeysByFilter(filter) {
    const keys = await client.keys(filter);
    return keys;
  },
  async deleteKeyToRedis(key) {
    await client.del(key);
  },
  async delAllKeysToRedis() {
    const keys = await client.keys('*');
    for (let i = 0; i < keys.length; i++) {
      await redisUtils.deleteKeyToRedis(keys[i]);
    }
  },
  async incrValueToRedis(key) {
    if (key) {
      const r = await client.incr(key);
      return r;
    }
    throw new Error('key is needed');
  },
  async expireatToRedis(key, timestamp) {
    await client.expireat(key, timestamp);
  }
};
