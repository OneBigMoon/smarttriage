'use strict';

const Utils = require('../utils');
const logger = Utils.logger(__filename);
const Model = require('../models');
const Datasourcetype = Model('Datasourcetype');

module.exports = {
  async query(ctx) {
    try {
      const datasourcetypes = await Datasourcetype.find({});
      ctx.body = {
        errcode: 0,
        result: datasourcetypes
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
