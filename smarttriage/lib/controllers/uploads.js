'use strict';
const path = require('path');
const Utils = require('../utils');
const Model = require('../models');
const Log = Model('Log');
const Upgrade = Model('Upgrade');
const logger = Utils.logger(__filename);
const readFileMd5 = Utils.crypto.readFileMd5;
const upgrades = require('./upgrades');

module.exports = {
  async upload(ctx) {
    try {
      const query = ctx.request.query;
      const params = ctx.request.body;
      const type = query.type;
      const filePath = path.resolve(__dirname, `../../${ctx.request.files.file.path}`);
      const name = ctx.request.files.file.path.split('\\')[ctx.request.files.file.path.split('\\').length - 1];
      if (type == 'logfile') {
        const log = new Log({
          no: params.no,
          originname: ctx.request.files.file.name,
          name,
          path: filePath,
          ct: new Date(),
          ut: new Date()
        });
        await log.save();
      } else if (type == 'upgrade') {
        const versions = params.appVersion.split('.');
        const sortAppVersion = Number(`${10000 + Number(versions[0])}${10000 + Number(versions[1])}${10000 + Number(versions[2])}`);
        const md5 = await readFileMd5(filePath);
        const upgrade = new Upgrade({
          model: params.model,
          appVersion: params.appVersion,
          sortAppVersion,
          originname: ctx.request.files.file.name,
          name,
          path: filePath,
          md5,
          ct: new Date(),
          ut: new Date()
        });
        await upgrade.save();
        await upgrades.sendUpgradeCmd(ctx);
      }

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
  }
};
