'use strict';

const _ = require('lodash');
const Model = require('../models');
const Utils = require('../utils');
const logger = Utils.logger(__filename);
const redis = Utils.redis;
const socketioEmitter = Utils.socketio;
const Box = Model('Box');
const Datasource = Model('Datasource');

function isDataEnabled(box) {
  return box && Number(box.dataenabled) === 1;
}

const socketios = module.exports = {
  async connect(socket) {
    try {
      const params = socket.request._query;
      logger.info('connect, params: ' + JSON.stringify(params));
      const date = new Date();
      if (!Utils.validateParams(params, ['ip'])) {
        logger.error('connect invalid params');
        socket.emit('apiv1_error', {
          type: 'ERROR',
          content: {
            msg: 'connect invalid params'
          }
        });
        return;
      }
      if (params.no) { // 尝试重连终端
        let box = await Box.findOne({
          no: params.no
        });
        if (box && box.status != '断开') { // 该终端号被使用
          socket.emit('apiv1_message', {
            type: 'ERROR',
            content: {
              code: '11001',
              msg: 'boxno is repeated'
            }
          });
          socket.disconnect(true); // 断开该socket
          return;
        } else if (box) {
          await Box.update({ no: params.no }, { ip: params.ip, mac: params.mac, model: params.model,
            appversion: params.appversion, ut: date, ht: date, status: '正常' });
          socket.join(params.no);
        } else {
          box = new Box({
            no: params.no,
            name: params.no,
            org: -1,
            ip: params.ip,
            mac: params.mac,
            model: params.model,
            appversion: params.appversion,
            status: '正常',
            ht: date,
            ct: date,
            ut: date
          });
          await box.save();
          socket.join(params.no);
        }
      } else { // 新建终端
        const boxes = await Box.find({}).sort({ no: -1 }).limit(1);
        let boxNo;
        if (boxes.length == 0) {
          boxNo = 'BOX001';
        } else {
          let incNo = boxes[0].no.replace('BOX', '');
          incNo = Number(incNo) + 1;
          incNo = String(incNo);
          if (incNo.length < 3) {
            for (let i = incNo.length; i < 3; i++) {
              incNo = '0' + incNo;
            }
          }
          boxNo = 'BOX' + incNo;
        }
        const box = new Box({
          no: boxNo,
          name: boxNo,
          org: -1,
          ip: params.ip,
          mac: params.mac,
          model: params.model,
          appversion: params.appversion,
          status: '正常',
          ht: date,
          ct: date,
          ut: date
        });
        await box.save();
        socket.request._query.no = boxNo;
        socket.join(boxNo);
      }
      socket.on('disconnect', function() {
        Box.update({ no: socket.request._query.no }, { status: '断开' }).exec();
      });
      await socketios.sendConfig(socket);
      await socketios.sendInitialData(socket);
    } catch (e) {
      logger.error('connect error', e);
    }
  },
  async apiv1_heartbeat(data, socket) {
    try {
      const params = socket.request._query;
      const date = new Date();
      const box = await Box.findOne({ no: params.no });
      if (box) {
        if (box.status == '断开') {
          socket.emit('apiv1_message', {
            type: 'ERROR',
            content: {
              code: '11002',
              msg: 'box is disconnected'
            }
          });
          socket.disconnect(true); // 断开该socket
        } else {
          await Box.update({ no: params.no }, { ip: data.content.ip, ut: date, ht: date });
        }
      }
    } catch (e) {
      logger.error('apiv1_heartbeat error', e);
    }
  },
  async apiv1_message(data, socket) {
    try {
      const params = socket.request._query;
      const date = new Date();
      // 为了流程可读性不简化代码
      switch (data.type) {
        case 'CONFIG':
          await Box.update({ no: params.no }, _.assign({ ut: date }, Utils.pickNotNull(data.content,
             ['style', 'datasource', 'volume', 'powerontime', 'powerofftime', 'rotation'])));
          await socketios.sendConfig(socket);
          break;
        case 'COMMAND':
          let status;
          if (data.content.cmd == 'on') {
            status = '正常';
          } else if (data.content.cmd == 'off') {
            status = '关机';
          }
          if (status) {
            await Box.update({ no: params.no }, { ut: date, status });
          }
          break;
        default:
      }
    } catch (e) {
      logger.error('apiv1_message error', e);
    }
  },
  async sendConfig(socket, box) {
    if (!box) {
      const params = socket.request._query;
      box = await Box.findOne({ no: params.no });
    }
    let offtime = '';
    if (box.datasource) {
      const datasource = await Datasource.findOne({ _id: box.datasource });
      if (datasource && datasource.morningcleartime) {
        offtime = datasource.morningcleartime;
      }
      if (datasource && datasource.afternooncleartime) {
        if (offtime) {
          offtime += ',' + datasource.afternooncleartime;
        } else {
          offtime = datasource.afternooncleartime;
        }
      }
    }
    socketioEmitter('socketio').to(box.no).emit('apiv1_message', {
      type: 'CONFIG',
      content: _.assign(Utils.pickNotNull(box, ['no', 'name', 'style', 'datasource', 'volume',
       'powerontime', 'powerofftime', 'rotation', 'horselamp', 'title', 'winname']), { timestamp: new Date().getTime(), offtime })
    });
  },
  async sendInitialData(socket, box) {
    if (!box) {
      const params = socket.request._query;
      box = await Box.findOne({ no: params.no });
    }
    if (box && box.datasource && isDataEnabled(box)) {
      const datasource = await Datasource.findOne({ _id: box.datasource });
      if (datasource.type == 'primarytriage' || datasource.type == 'leveldepart' ) {
        const cache = await redis.getFieldValueFromHashRedis('primary:' + datasource.departmentid.join(','), 'data');
        const content = await Utils.transformPrimaryData(cache);
        await socketios.emitData(box.no, { type: 'DATA', content });
      } else if (datasource.type == 'secondarytriage') {
        const cache = await redis.getFieldValueFromHashRedis('secondary:' + datasource.screenid, 'data');
        const content = await Utils.transformSecondaryData(cache);
        await socketios.emitData(box.no, { type: 'DATA', content });
      } else if (datasource.type == 'secondarytriageultrasonic') {
        let content = [];
        let cacheData;
        for (let i = 0; i < datasource.queue.length; i++) {
          cacheData = await redis.getFieldValueFromHashRedis('pacssecondary:' + datasource.queue[i], 'data');
          content.push(cacheData);
        }
        content = await Utils.transformPacsSecondaryData(content);
        await socketios.emitData(box.no, { type: 'DATA', content });
      } else if (datasource.type == 'drawbloodtriage') {
        const cache = await redis.getFieldValueFromHashRedis('drawblood:' + datasource.windowid.join(','), 'data');
        const content = await Utils.transformDrawbloodData(cache);
        await socketios.emitData(box.no, { type: 'DATA', content });
      } else if (datasource.type == 'primarypharmacytriage') {
        const cache = await redis.getFieldValueFromHashRedis('pharmacyprimary:' +
         datasource.pharmacydeptno + '_' + datasource.pharmacywinno.join(','), 'data');
        const content = await Utils.transformPharmacyData(cache);
        await socketios.emitData(box.no, { type: 'DATA', content });
      } else if (datasource.type == 'secondarypharmacytriage') {
        const cache = await redis.getFieldValueFromHashRedis('pharmacysecondary:' +
         datasource.pharmacydeptno + '_' + datasource.pharmacywinno.join(','), 'data');
        const content = await Utils.transformPharmacySecondaryData(cache);
        await socketios.emitData(box.no, { type: 'DATA', content });
      }
    }
  },
  async clearData(boxNo) {
    await socketios.emitData(boxNo, {
      type: 'COMMAND',
      content: {
        cmd: 'cleardata'
      }
    });
  },
  async emitData(boxNo, data) {
    try {
      if (data && data.type == 'DATA') {
        const box = await Box.findOne({ no: boxNo });
        if (!isDataEnabled(box)) {
          return;
        }
      }
      socketioEmitter('socketio').to(boxNo).emit('apiv1_message', data);
    } catch (e) {
      logger.error('emitData error', e);
    }
  }
};
