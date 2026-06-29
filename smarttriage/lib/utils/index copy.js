'use strict';

const i18n = require('i18n');
const util = require('util');
const _ = require('lodash');

const promisified = require('./promisified');

const crypto = require('./crypto');
const handleError = require('./errorhandler');
const logger = require('./logger');
const passport = require('./passport');
const socketio = require('./socketio');
const webservice = require('./webservice');
const redis = require('./redis');
const fileUtil = require('./fileutil');
const oracleUtil = require('./oracle');

module.exports = _.assign({}, util, {
  parseJson(json) {
    let obj;
    try {
      obj = JSON.parse(json);
    } catch (e) {
      logger(__filename).error('JSON parse error', e);
    }
    return obj || {};
  },
  pickNotNull() {
    return _.transform(_.pick.apply(_, arguments), (result, value, key) => {
      value != null && (result[key] = value);
    });
  },
  upperCamelCase(string) {
    const firstLetter = string.substr(0, 1).toUpperCase();
    return firstLetter + _.camelCase(string.substr(1));
  },
  toRegExp(str, flags) {
    return new RegExp(_.trim(str).replace(/([\\\/\*\.\+\&\^\[\]\(\)\{\}\|\?\=\!])/g, '\\$1'), flags);
  },
  validateParams(object, fields) {
    if (_.isEmpty(object)) {
      return false;
    }

    // 值为数字需要独立判断
    function isEmpty(val) {
      return _.isNumber(val) ? false : _.isEmpty(val) ||
        _.isNull(object) || _.isUndefined(object) || _.isNaN(object) || val === 'undefined';
    }

    if (_.isString(fields)) {
      return _.has(object, fields) && !isEmpty(object[fields]);
    }

    // 遍历校验所有属性值
    return _.every(fields, function(field) {
      return _.has(object, field) &&
        _.isBoolean(object[field]) || _.isArray(object[field]) ? true : !isEmpty(object[field]);
    });
  },
  compareObjProps(obj1, obj2, props) {
    if (obj1 == obj2) {
      return true;
    }
    return _.every(props, function(p) {
      return obj1[p] == obj2[p];
    });
  },
  getSocketioRoom(params) {
    switch (params.type) {
      case 'BEDSIDECARD':
        return params.type + '_' + params.wardno + '_' + params.bedno;
      default:
        return params.type + '_' + params.wardno;
    }
  },
  getRedisKey(type, wardno, bedno) {
    let key = '';
    if (wardno) {
      key += 'ward_' + wardno + ':';
    }
    if (bedno) {
      key += 'bed_' + bedno + ':';
    }
    key += type;
    return key;
  },
  getSocketioEvent(terminalType) {
    switch (terminalType) {
      case 'LOGSCREEN_BEDS':
      case 'LOGSCREEN_LOGS':
        return 'apiv1_logscreen_message';
      case 'NURSINGHOST':
        return 'apiv1_nursinghost_message';
      case 'BEDSIDECARD':
        return 'apiv1_bedsidecard_message';
      default:
    }
  },
  *transformPrimaryData(cache) {
    try {
      cache = JSON.parse(cache);
      const result = { queues: [] };
      for (let i = 0; i < cache.length; i++) {
        const row = cache[i];
        result.queues.push({
          departmentid: row[0],
          officename: row[1],
          doctorname: row[2],
          callno: row[3],
          patientname: row[4],
          passno: row[5],
          calltime: row[6],
          callmessage: row[7],
          callvoice: row[8],
          waitno: row[9] || ''
        });
      }
      return result;
    } catch (err) {
      logger(__filename).error('transformPrimary error', err);
    }
  },
  *transformSecondaryData(cache) {
    try {
      cache = JSON.parse(cache);
      const result = { patients: [] };
      for (let i = 0; i < cache.length; i++) {
        const row = cache[i];
        if (!result.clinicname && row[0]) {
          result.clinicname = row[0];
        }
        if (!result.screenid && row[1]) {
          result.screenid = row[1];
        }
        if (!result.doctorname && row[2]) {
          result.doctorname = row[2];
        }
        if (!result.doctortitle && row[3]) {
          result.doctortitle = row[3];
        }
        if (!result.doctorintro && row[4]) {
          result.doctorintro = row[4];
        }
        if (!result.doctorschedule && row[5]) {
          result.doctorschedule = row[5];
        }
        if (!result.doctorphoto) {
          result.doctorphoto = row[6] ? row[6] : null;
        }
        if (!result.doctorno && row[11]) {
          result.doctorno = row[11];
        }
        if (!result.doctorfeature && row[12]) {
          result.doctorfeature = row[12];
        }
        result.patients.push({
          ticket: row[7],
          brxm: row[8],
          brxmfull: row[9],
          status: row[10],
          calltime: row[13],
          callmessage: `请 ${row[7]}号 ${row[8]} 到 ${row[0]} 就诊`,
          callvoice: `请${row[7]}号${row[9]}到${row[0]}就诊`
        });
      }
      result.patientcount = yield redis.getValueFromRedis('secondary_count:' + result.doctorno, 'string');
      return result;
    } catch (err) {
      logger(__filename).error('transformSecondary error', err);
    }
  },
  *transformSecondarySplitData(caches) {
    try {
      const result = { queues: [] };
      for (let j = 0; j < caches.length; j++) {
        let cache = caches[j];
        if (cache) {
          cache = JSON.parse(cache);
          const queue = { patients: [] };
          for (let i = 0; i < cache.length; i++) {
            const row = cache[i];
            if (!queue.clinicname && row[0]) {
              queue.clinicname = row[0];
            }
            if (!queue.screenid && row[1]) {
              queue.screenid = row[1];
            }
            if (!queue.doctorname && row[2]) {
              queue.doctorname = row[2];
            }
            if (!queue.doctortitle && row[3]) {
              queue.doctortitle = row[3];
            }
            if (!queue.doctorintro && row[4]) {
              queue.doctorintro = row[4];
            }
            if (!queue.doctorschedule && row[5]) {
              queue.doctorschedule = row[5];
            }
            if (!queue.doctorphoto) {
              queue.doctorphoto = row[6] ? row[6] : null;
            }
            if (!queue.doctorno && row[11]) {
              queue.doctorno = row[11];
            }
            if (!queue.doctorfeature && row[12]) {
              queue.doctorfeature = row[12];
            }
            if (!queue.offtime && row[13]) { // 市皮才有offtime
              queue.offtime = row[13];
            }
            queue.patients.push({
              ticket: row[7],
              brxm: row[8],
              brxmfull: row[9],
              status: row[10]
            });
          }
          queue.patientcount = yield redis.getValueFromRedis('secondary_count:' + queue.doctorno, 'string');
          result.queues.push(queue);
        }
      }
      return result;
    } catch (err) {
      logger(__filename).error('transformSecondarySplitData error', err);
    }
  },
  *transformPacsSecondaryData(caches) {
    try {
      const result = { queues: [] };
      caches.forEach((cache) => {
        if (cache) {
          cache = JSON.parse(cache);
          const queue = { patients: [] };
          for (let i = 0; i < cache.length; i++) {
            const row = cache[i];
            if (!queue.queuename && row[0]) {
              queue.queuename = row[0];
            }
            queue.patients.push({
              ticket: row[1],
              brxmfull: row[2],
              status: row[3]
            });
          }
          result.queues.push(queue);
        }
      });
      return result;
    } catch (err) {
      logger(__filename).error('transformSecondary error', err);
    }
  },
  *transformDrawbloodData(cache) {
    try {
      cache = JSON.parse(cache);
      const result = {
        patiens: []
      };
      for (let i = 0; i < cache.length; i++) {
        const row = cache[i];
        result.patiens.push({
          no: row[3],
          name: row[5],
          winno: row[0],
          winname: row[1],
          callvoice: row[7]
        });
      }
      return result;
    } catch (err) {
      logger(__filename).error('transformDrawblood error', err);
    }
  },
  *transformPharmacyData(cache) {
    try {
      cache = JSON.parse(cache);
      const result = {
        patiens: []
      };
      for (let i = 0; i < cache.length; i++) {
        const row = cache[i];
        if (!result.deptno) {
          result.deptno = row[0];
        }
        if (!result.winno) {
          result.winno = row[1];
        }
        result.patiens.push({
          no: row[2],
          name: row[6],
          namefull: row[3],
          calltimes: row[4],
          winno: row[1],
          calltime: row[5]
        });
      }
      return result;
    } catch (err) {
      logger(__filename).error('transformPharmacy error', err);
    }
  }
}, {
  crypto,
  handleError,
  logger,
  passport,
  socketio,
  i18n,
  webservice,
  redis,
  fileUtil,
  oracleUtil
}, promisified);
