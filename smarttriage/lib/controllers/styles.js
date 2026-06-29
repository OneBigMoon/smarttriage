'use strict';

const Utils = require('../utils');
const logger = Utils.logger(__filename);
const Model = require('../models');
const Style = Model('Style');

module.exports = {
  async query(ctx) {
    try {
      const styles = await Style.find({});
      ctx.body = {
        errcode: 0,
        result: styles
      };
    } catch (e) {
      logger.error('error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  }
};
