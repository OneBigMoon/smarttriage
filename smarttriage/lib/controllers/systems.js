'use strict';

const Model = require('../models');
const System = Model('System');
const _ = require('lodash');
const Utils = require('../utils');
const logger = Utils.logger(__filename);
const redis = Utils.redis;

const systems = module.exports = {
  async get(ctx) {
    try {
      const system = await System.findOne();
      ctx.body = {
        errcode: 0,
        result: system
      };
    } catch (e) {
      logger.error('error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  },
  async save(ctx) {
    try {
      const params = ctx.request.body;
      await System.update({}, _.pick(params, ['url', 'primarytablename', 'secondarytablename',
       'secondarycounttablename', 'drawbloodtablename', 'pharmacytablename',
       'username', 'password', 'pacssecondarytablename', 'pacsusername', 'pacspassword']));
      await systems.initCache();
      ctx.body = {
        errcode: 0
      };
    } catch (e) {
      logger.error('error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  },
  async initCache() {
    const system = await System.findOne();
    if (!system) {
      throw new Error('系统配置为空');
    }
    await redis.setValueToRedis('system', system._info, 'hash');
  }
};
