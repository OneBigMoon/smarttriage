'use strict';

const Utils = require('../utils');
const _ = require('lodash');
const send = require('koa-send');
const logger = Utils.logger(__filename);
const Model = require('../models');
const Box = Model('Box');
const Log = Model('Log');
const Orgnization = Model('Orgnization');
const socketios = require('./socketios');
const Config = require('../config');
const uploadPath = Config.dir.upload;

function isBoxOnline(box) {
  return box && box.status == '正常';
}

async function syncBoxDataEnabled(box, previousEnabled) {
  const currentEnabled = Number(box.dataenabled) === 1;
  if (!isBoxOnline(box) || previousEnabled === currentEnabled) {
    return;
  }
  if (currentEnabled) {
    await socketios.sendInitialData(null, box);
  } else {
    await socketios.clearData(box.no);
  }
}

module.exports = {
  async query(ctx) {
    try {
      const params = ctx.request.body;
      const filter = {};
      if (params.no) {
        filter.no = new RegExp(params.no, 'i');
      }
      if (params.name) {
        filter.name = new RegExp(params.name, 'i');
      }
      if (params.org != 'all' && (params.org || params.org == 0)) {
        const org = await Orgnization.findOne({ _id: params.org });
        const idpath = org.idpath.replace(/\./g, '\\.');
        const orgs = await Orgnization.find({ idpath: new RegExp('^' + idpath) });
        const ids = [];
        orgs.forEach((o) => {
          ids.push(o._id);
        });
        filter.org = { $in: ids };
      }
      if (params.status && params.status != 'all') {
        filter.status = params.status;
      }
      const boxes = await Box.find(filter).populate('org').populate('datasource').sort({ no: -1 });
      ctx.body = {
        errcode: 0,
        result: boxes
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
  async toggleDataEnabled(ctx) {
    try {
      const params = ctx.request.body;
      const box = await Box.findOne({ _id: params.id });
      if (!box) {
        ctx.body = {
          errcode: 10001,
          errmsg: '终端不存在'
        };
        return;
      }
      const previousEnabled = Number(box.dataenabled) === 1;
      box.dataenabled = previousEnabled ? 0 : 1;
      box.ut = new Date();
      await box.save();
      await syncBoxDataEnabled(box, previousEnabled);
      ctx.body = {
        errcode: 0,
        result: {
          id: box._id,
          dataenabled: box.dataenabled
        }
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
  async enableAllData(ctx) {
    try {
      const now = new Date();
      await Box.update({}, { dataenabled: 1, ut: now }, { multi: true });
      const boxes = await Box.find({ status: '正常', dataenabled: 1 });
      for (let i = 0; i < boxes.length; i++) {
        await socketios.sendInitialData(null, boxes[i]);
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
  },
  async move(ctx) {
    try {
      const params = ctx.request.body;
      await Box.update({ _id: params.id }, { org: params.orgId, ut: new Date() });
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
  async remove(ctx) {
    try {
      const params = ctx.request.body;
      await Box.remove({ _id: params.id });
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
  async save(ctx) {
    try {
      const params = ctx.request.body;
      if (!params.datasource) {
        params.datasource = null;
      }
      if (!params.style) {
        params.style = null;
      }
      if (!params.rotation) {
        params.rotation = null;
      }
      await Box.update({ _id: params.id }, _.pick(params, ['name', 'style', 'powerontime',
       'powerofftime', 'volume', 'datasource', 'rotation', 'horselamp', 'title', 'winname']));
      const box = await Box.findOne({ _id: params.id });
      await socketios.sendConfig(null, box);
      await socketios.sendInitialData(null, box);
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
  async power(ctx) {
    try {
      const params = ctx.request.body;
      // params.id params.cmd on restart off
      let box;
      switch (params.cmd) {
        case 'on':
          box = await Box.findOne({ _id: params.id, status: '关机' });
          if (box) {
            box.status = '正常';
            await box.save();
            await socketios.emitData(box.no, {
              type: 'COMMAND',
              content: {
                cmd: 'on'
              }
            });
          }
          break;
        case 'restart':
          box = await Box.findOne({ _id: params.id, status: '正常' });
          if (box) {
            box.status = '断开';
            await box.save();
            await socketios.emitData(box.no, {
              type: 'COMMAND',
              content: {
                cmd: 'restart'
              }
            });
          }
          break;
        case 'off':
          box = await Box.findOne({ _id: params.id, status: '正常' });
          if (box) {
            box.status = '关机';
            await box.save();
            await socketios.emitData(box.no, {
              type: 'COMMAND',
              content: {
                cmd: 'off'
              }
            });
          }
          break;
        default:
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
  },
  async groupPower(ctx) {
    try {
      const params = ctx.request.body;
      // params.id params.cmd on restart off
      const org = await Orgnization.findOne({ _id: params.id });
      const idpath = org.idpath.replace(/\./g, '\\.');
      const orgs = await Orgnization.find({ idpath: new RegExp('^' + idpath) });
      const ids = [];
      orgs.forEach((o) => {
        ids.push(o._id);
      });
      const boxes = await Box.find({ org: { $in: ids } });
      let box;
      for (let i = 0; i < boxes.length; i++) {
        box = boxes[i];
        switch (params.cmd) {
          case 'on':
            if (box.status == '关机') {
              box.status = '正常';
              await box.save();
              await socketios.emitData(box.no, {
                type: 'COMMAND',
                content: {
                  cmd: 'on'
                }
              });
            }
            break;
          case 'restart':
            if (box.status == '正常') {
              box.status = '断开';
              await box.save();
              await socketios.emitData(box.no, {
                type: 'COMMAND',
                content: {
                  cmd: 'restart'
                }
              });
            }
            break;
          case 'off':
            if (box.status == '正常') {
              box.status = '关机';
              await box.save();
              await socketios.emitData(box.no, {
                type: 'COMMAND',
                content: {
                  cmd: 'off'
                }
              });
            }
            break;
          default:
        }
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
  },
  async groupSave(ctx) {
    try {
      const params = ctx.request.body;
      const org = await Orgnization.findOne({ _id: params.id });
      const idpath = org.idpath.replace(/\./g, '\\.');
      const orgs = await Orgnization.find({ idpath: new RegExp('^' + idpath) });
      const ids = [];
      orgs.forEach((o) => {
        ids.push(o._id);
      });
      await Box.update({ org: { $in: ids } }, _.pick(params, ['powerontime',
       'powerofftime', 'volume', 'horselamp']), { multi: true });
      const boxes = await Box.find({ org: { $in: ids } });
      for (let i = 0; i < boxes.length; i++) {
        await socketios.sendConfig(null, boxes[i]);
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
  },
  async checkBoxDisconnect(ctx) {
    try {
      await Box.update({ ht: { $lt: new Date(new Date().getTime() - 15 * 1000) }, status: { $ne: '断开' } },
       { ut: new Date(), status: '断开' }, { multi: true });
    } catch (e) {
      logger.error('error', e);
    }
  },
  async uploadLog(ctx) {
    try {
      const params = ctx.request.body;
      const box = await Box.findOne({ _id: params.id });
      await socketios.emitData(box.no, {
        type: 'COMMAND',
        content: {
          cmd: 'uploadlog'
        }
      });
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
  async checkLog(ctx) {
    try {
      const params = ctx.request.body;
      const box = await Box.findOne({ _id: params.id });
      const logs = await Log.find({ no: box.no }).sort({ ut: -1 }).limit(1);
      if (logs.length) {
        ctx.body = {
          errcode: 0
        };
      } else {
        ctx.body = {
          errcode: 10000,
          errmsg: '未查询到该设备的日志'
        };
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
  async downloadLog(ctx) {
    try {
      const params = ctx.request.query;
      const box = await Box.findOne({ _id: params.id });
      const logs = await Log.find({ no: box.no }).sort({ ut: -1 }).limit(1);
      if (logs.length) {
        const log = logs[0];
        ctx.set('Content-disposition', `attachment;filename=${log.originname}`);
        await send(ctx, log.name, { root: uploadPath });
      }
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
