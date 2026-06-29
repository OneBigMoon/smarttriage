'use strict';

const Utils = require('../utils');
const logger = Utils.logger(__filename);
const send = require('koa-send');
const Model = require('../models');
const Upgrade = Model('Upgrade');
const Box = Model('Box');
const socketios = require('./socketios');
const Config = require('../config');
const uploadPath = Config.dir.upload;

module.exports = {
  async query(ctx) {
    try {
      const params = ctx.request.body;
      const filter = {};
      if (params.name) {
        filter.name = new RegExp(params.name, 'i');
      }
      const upgrades = await Upgrade.find(filter).sort({ ut: -1 });
      ctx.body = {
        errcode: 0,
        result: upgrades
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
  async remove(ctx) {
    try {
      const params = ctx.request.body;
      await Upgrade.remove({ _id: params.id });
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
  async sendUpgradeCmd(ctx) {
    try {
      const boxes = await Box.find({
        status: '正常'
      });
      for (let i = 0; i < boxes.length; i++) {
        await socketios.emitData(boxes[i].no, {
          type: 'COMMAND',
          content: {
            cmd: 'upgrade'
          }
        });
      }
    } catch (e) {
      logger.error('error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  },
  async upgrade(ctx) {
    try {
      const params = ctx.request.body;
      const model = params.model;
      let appversion = params.appversion;
      if (!model || !appversion) {
        ctx.status = 500;
        ctx.body = {
          errcode: 10000,
          errmsg: '请求参数错误'
        };
        return;
      }
      const versions = appversion.split('.');
      appversion = Number(`${10000 + Number(versions[0])}${10000 + Number(versions[1])}${10000 + Number(versions[2])}`);
      const upgrades = await Upgrade.find({
        model,
        sortAppVersion: {
          $gt: appversion
        }
      }).sort({
        sortAppVersion: -1
      }).limit(1);
      ctx.body = {
        errcode: 0,
        name: upgrades[0] ? upgrades[0].name : null,
        originname: upgrades[0] ? upgrades[0].originname : null,
        md5: upgrades[0] ? upgrades[0].md5 : null,
        appversion: upgrades[0] ? upgrades[0].appVersion : null
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
  async download(ctx) {
    try {
      const params = ctx.request.query;
      const { name } = params;
      const upgrade = await Upgrade.findOne({ name });
      ctx.set('Content-disposition', `attachment;filename=${upgrade.originname}`);
      await send(ctx, upgrade.name, { root: uploadPath });
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
