'use strict';

const Utils = require('../utils');
const oracleUtil = Utils.oracleUtil;
const logger = Utils.logger(__filename);
const crypto = Utils.crypto;
const redis = Utils.redis;
const Model = require('../models');
const socketios = require('./socketios');
const Datasource = Model('Datasource');
const Box = Model('Box');
const _ = require('lodash');

module.exports = {
  *initCache() {
    // 凌晨清空redis就诊人数
    const keys = yield redis.getKeysByFilter('secondary_count:*');
    for (let i = 0; i < keys.length; i++) {
      yield redis.deleteKeyToRedis(keys[i]);
    }
  },
  *syncPrimaryData() {
    try {
      const system = yield redis.getValueFromRedis('system', 'hash');
      if (!system.primarytablename) {
        logger.debug('一级分诊表/视图名未配置，结束本次调度。');
        return;
      }
      // const sql = 'select JZKS00, ZSMC00, YSXM00, HZXHMC, BRXM00, GZBRXH, ' +
      //  'JHSJ00, DSXSXX, JHYYXX, HZBRXH from ' + system.primarytablename + ' order by JHSJ00 desc';

       const sql = 'select XSPID0, ZSMC00, YSXM00, HZXHMC, BRXM00, GZBRXH, ' +
       'JHSJ00, DSXSXX, JHYYXX, HZBRXH from ' + system.primarytablename + ' order by JHSJ00 desc';
       logger.info("---------------------一级分诊sql--------", sql);

      const result = yield oracleUtil.query(sql);

     // logger.info("---------------------一级分诊result--------", JSON.stringify(result));

      const departmentMap = {};
      result.forEach(function(row) {
        if (departmentMap[row[0]]) {
          departmentMap[row[0]].push(row);
        } else {
          departmentMap[row[0]] = [row];
        }
      });
      // logger.info("---------------------一级分诊map--------", JSON.stringify(departmentMap));

      // const datasources = yield Datasource.find({ type: 'primarytriage' });
      const datasources = yield Datasource.find({"$or":[
          {type: "primarytriage"},
          {type: "leveldepart"}
        ]});
      logger.info("---------------------一级分诊datasources--------", JSON.stringify(datasources));

      for (let j = 0; j < datasources.length; j++) {
        const datasource = datasources[j];
        const key = datasource.departmentid.join(',');
        let cache = [];
        /* eslint-disable */
        datasource.departmentid.forEach(function(deptid) {
          if (departmentMap[deptid] && datasource.type !== "leveldepart") {
            cache = cache.concat(departmentMap[deptid]);
          }
          if (departmentMap[deptid] && datasource.type === "leveldepart") {
            cache = cache.concat(new Array(departmentMap[deptid][0]));
          }
        });
        cache.sort(function(a, b) {
          return a[6] < b[6] ? 1 : -1;
        });
        /* eslint-enable */
        cache = JSON.stringify(cache);
        console.log("cache" + cache);
        const md5 = crypto.md5(cache);
        const lastMd5 = yield redis.getFieldValueFromHashRedis('primary:' + key, 'md5');
        //let lastMd5=Date.now();

        if (md5 != lastMd5) {
          yield redis.setValueToRedis('primary:' + key, {
            md5,
            data: cache
          }, 'hash');
          const boxes = yield Box.find({ datasource: datasource._id });
          logger.info("---------------------一级分诊boxes--------", JSON.stringify(boxes));

          const content = yield Utils.transformPrimaryData(cache);
          for (let i = 0; i < boxes.length; i++) {
            yield socketios.emitData(boxes[i].no, { type: 'DATA', content });
            // logger.info("---------------------一级分诊socket--------", JSON.stringify(content));

          }
        }
      }
    } catch (err) {
      logger.error('syncPrimaryData error', err);
    }
  },
  *syncSecondaryData() {
    try {
      const system = yield redis.getValueFromRedis('system', 'hash');
      if (!system.secondarytablename || !system.secondarycounttablename) {
        logger.debug('二级分诊表/视图名或二级分诊人数表/视图名未配置，结束本次调度。');
        return;
      }
      let sql = 'select JZYSBH, JZRS00 from ' + system.secondarycounttablename;
      logger.info("---------------------二级分诊sql1--------", sql);

      let result = yield oracleUtil.query(sql);

      //logger.info("---------------------二级分诊result--------", JSON.stringify(result));

      for (let i = 0; i < result.length; i++) {
        yield redis.setValueToRedis('secondary_count:' + result[i][0], result[i][1], 'string');
      }
      sql = 'select ZSMC00, XSPID0, JZYSXM, YSZYZC, ZYTC00, PBSJ00,' +
       'PHOTO0, HZXHMC, BRXM00, BRXM01, HZZT00, JZYSBH, YSJJ00, JZSJ00 from ' + system.secondarytablename;
       logger.info("---------------------二级分诊sql2--------", sql);

      result = yield oracleUtil.query(sql);
      //logger.info("---------------------二级分诊result2--------", JSON.stringify(result));

      const screenMap = {};
      result.forEach(function(row) {
        if (!screenMap[row[1]]) {
          screenMap[row[1]] = [];
        }
        screenMap[row[1]].push(row);
      });
     // logger.info("---------------------二级分诊map--------", JSON.stringify(screenMap));

      for (const key in screenMap) {
        if (Object.prototype.hasOwnProperty.call(screenMap, key)) {
          const cache = JSON.stringify(screenMap[key]);
          const md5 = crypto.md5(cache);
          const lastMd5 = yield redis.getFieldValueFromHashRedis('secondary:' + key, 'md5');
          if (md5 != lastMd5) {
            yield redis.setValueToRedis('secondary:' + key, {
              md5,
              data: cache
            }, 'hash');
            // 二级分诊
            let datasources = yield Datasource.find({ type: 'secondarytriage', screenid: key });
            logger.info("---------------------二级分诊datasources--------", JSON.stringify(datasources));

            for (let j = 0; j < datasources.length; j++) {
              const datasource = datasources[j];
              const boxes = yield Box.find({ datasource: datasource._id });
              logger.info("---------------------二级分诊boxes--------", JSON.stringify(boxes));

              const content = yield Utils.transformSecondaryData(cache);
              for (let i = 0; i < boxes.length; i++) {
                yield socketios.emitData(boxes[i].no, { type: 'DATA', content });
                // logger.info("---------------------二级分诊socket--------", JSON.stringify(content));

              }
            }
            // 二级分诊(分屏)
            datasources = yield Datasource.find({ type: 'secondarytriagesplit', screensplitid: key });
            for (let j = 0; j < datasources.length; j++) {
              const datasource = datasources[j];
              const boxes = yield Box.find({ datasource: datasource._id });
              let content = [];
              let cacheData;
              for (let i = 0; i < datasource.screensplitid.length; i++) {
                cacheData = yield redis.getFieldValueFromHashRedis('secondary:' + datasource.screensplitid[i], 'data');
                content.push(cacheData);
              }
              content = yield Utils.transformSecondarySplitData(content);
              for (let i = 0; i < boxes.length; i++) {
                yield socketios.emitData(boxes[i].no, { type: 'DATA', content });
              }
            }
          }
        }
      }
    } catch (err) {
      logger.error('syncSecondaryData error', err);
    }
  },
  *syncSecondaryUltrasonicData() {
    try {
      const pacsMap = {};
      const system = yield redis.getValueFromRedis('system', 'hash');
      if (!system.pacssecondarytablename) {
        logger.debug('二级超声表/视图名未配置，结束本次调度。');
        return;
      }
      const time = new Date().getHours() < 13 ? '上午' : '下午';
      let sql = 'select 队列名称, 排队号, NAME, 状态, 叫号时间,呼叫诊室 from ' +
        '(select 队列名称, 排队号, NAME, 状态, 叫号时间,呼叫诊室 ,row_number() over(partition by 队列名称 order by 叫号时间 desc) ' +
        'NO from ' + system.pacssecondarytablename + ' where 状态!=\'0\' and 叫号时间 is not null) where NO=1';

      let result = yield oracleUtil.queryPacs(sql);


      for (let i = 0; i < result.length; i++) {
        pacsMap[result[i][0]] = [result[i]];
      }
      sql = 'select 队列名称, 排队号, NAME, 状态 ,呼叫诊室 from ' + system.pacssecondarytablename
        + ' where 状态=\'0\' and 队列时段=\'' + time + '\' order by 队列名称,排队号';

      result = yield oracleUtil.queryPacs(sql);

      for (let i = 0; i < result.length; i++) {
        if (pacsMap[result[i][0]]) {
          pacsMap[result[i][0]].push(result[i]);
        } else {
          pacsMap[result[i][0]] = [result[i]];
        }
      }
      for (const key in pacsMap) {
        if (Object.prototype.hasOwnProperty.call(pacsMap, key)) {
          const cache = JSON.stringify(pacsMap[key]);
          const md5 = crypto.md5(cache);
          let lastMd5 = yield redis.getFieldValueFromHashRedis('pacssecondary:' + key, 'md5');

          lastMd5=Date.now();

          if (md5 != lastMd5) {
            yield redis.setValueToRedis('pacssecondary:' + key, {
              md5,
              data: cache
            }, 'hash');
            const datasources = yield Datasource.find({ type: 'secondarytriageultrasonic', queue: key });
            for (let j = 0; j < datasources.length; j++) {
              const datasource = datasources[j];
              const boxes = yield Box.find({ datasource: datasource._id });
              let content = null;

              var contents={
                  content:[],
                  consultingroomname:[]

              };
              let cacheData;
              for (let i = 0; i < datasource.queue.length; i++) {

                cacheData = yield redis.getFieldValueFromHashRedis('pacssecondary:' + datasource.queue[i], 'data');

                  contents.content.push(cacheData);
                //如果有设置诊室名称
                if(datasource.consultingroomname ){
                     //如果能查到数据，且有对应的诊室名称不为空
                    if(datasource.consultingroomname[i]){
                      contents.consultingroomname.push(datasource.consultingroomname[i]);
                    }else{
                      contents.consultingroomname.push('');

                    }

                }else{
                  contents.consultingroomname.push('');

                }

              }

              content = yield Utils.transformPacsSecondaryData(contents);
              for (let i = 0; i < boxes.length; i++) {
                yield socketios.emitData(boxes[i].no, { type: 'DATA', content });
              }
            }
          }
        }
      }
    } catch (err) {
      logger.error('syncSecondaryUltrasonicData error', err);
    }
  },
  *syncDrawbloodData() {
    try {
      const system = yield redis.getValueFromRedis('system', 'hash');
      if (!system.drawbloodtablename) {
        logger.debug('检验表/视图名未配置，结束本次调度。');
        return;
      }
      const sql = 'select CYCKBH,CKMC00,JHBZ00,DLH000,DJSJ00,BRXM00,DSXSXX,JHYYXX from ' +
       system.drawbloodtablename + ' where JHBZ00=\'1\' or JHBZ00=\'7\' order by CYCKBH,JHBZ00 desc';
      const result = yield oracleUtil.query(sql);
      const windowMap = {};
      result.forEach(function(row) {
        windowMap[row[0]] = row;
      });
      const datasources = yield Datasource.find({ type: 'drawbloodtriage' });
      for (let j = 0; j < datasources.length; j++) {
        const datasource = datasources[j];
        const key = datasource.windowid.join(',');
        let cache = [];
        /* eslint-disable */
        datasource.windowid.forEach(function(winid) {
          if (windowMap[winid]) {
            cache.push(windowMap[winid]);
          }
        });
        cache.sort(function(a, b) {
          if (a[0] < b[0]) {
            return -1;
          } else if (a[0] > b[0]) {
            return 1;
          }
        });
        /* eslint-enable */
        cache = JSON.stringify(cache);
        const md5 = crypto.md5(cache);
        const lastMd5 = yield redis.getFieldValueFromHashRedis('drawblood:' + key, 'md5');
        if (md5 != lastMd5) {
          yield redis.setValueToRedis('drawblood:' + key, {
            md5,
            data: cache
          }, 'hash');
          const boxes = yield Box.find({ datasource: datasource._id });
          const content = yield Utils.transformDrawbloodData(cache);
          for (let i = 0; i < boxes.length; i++) {
            yield socketios.emitData(boxes[i].no, { type: 'DATA', content });
          }
        }
      }
    } catch (err) {
      logger.error('syncDrawbloodData error', err);
    }
  },
  *syncPrimaryPharmacyData() {
    try {
      const system = yield redis.getValueFromRedis('system', 'hash');
      if (!system.pharmacytablename) {
        logger.debug('药房表/视图名未配置，结束本次调度。');
        return;
      }
      const sql = 'select yfbmbh,fyckbh,fydlxh,brxm00,jhcs00,jhsj00,brxm01 from ' +
       system.pharmacytablename + ' order by yfbmbh,fyckbh,jhsj00 desc';
      const result = yield oracleUtil.query(sql);
      const windowMap = {};
      result.forEach(function(row) {
        if (windowMap[`${row[0]}_${row[1]}`]) {
          windowMap[`${row[0]}_${row[1]}`].push(row);
        } else {
          windowMap[`${row[0]}_${row[1]}`] = [row];
        }
      });
      const datasources = yield Datasource.find({ type: 'primarypharmacytriage' });
      for (let j = 0; j < datasources.length; j++) {
        const datasource = datasources[j];
        const key = `${datasource.pharmacydeptno}_${datasource.pharmacywinno.join(',')}`;
        let cache = [];
        /* eslint-disable */
        datasource.pharmacywinno.forEach((winno, i) => {
          const k = `${datasource.pharmacydeptno}_${datasource.pharmacywinno[i]}`;
          if (windowMap[k]) {
            cache = cache.concat(windowMap[k]);
          }
        });
        /* eslint-enable */
        cache.sort(function(a, b) {
          return a[5] < b[5] ? 1 : -1;
        });

        cache = JSON.stringify(cache);
        const md5 = crypto.md5(cache);
        const lastMd5 = yield redis.getFieldValueFromHashRedis('pharmacyprimary:' + key, 'md5');
        if (md5 != lastMd5) {
          yield redis.setValueToRedis('pharmacyprimary:' + key, {
            md5,
            data: cache
          }, 'hash');
          const boxes = yield Box.find({ datasource: datasource._id });
          const content = yield Utils.transformPharmacyData(cache);
          for (let i = 0; i < boxes.length; i++) {
            yield socketios.emitData(boxes[i].no, { type: 'DATA', content });
          }
        }
      }
    } catch (err) {
      logger.error('syncPrimaryPharmacyData error', err);
    }
  },
  *syncSecondaryPharmacyData() {
    try {
      const system = yield redis.getValueFromRedis('system', 'hash');
      if (!system.pharmacytablename) {
        logger.debug('药房表/视图名未配置，结束本次调度。');
        return;
      }
      const sql = 'select yfbmbh,fyckbh,fydlxh,brxm00,brxm01,jhcs00,jhsj00,zt0000,CKMC00 from ' +
       system.pharmacytablename + ' where zt0000 in (\'0\', \'1\', \'2\') order by yfbmbh,fyckbh,zt0000,jhsj00 desc';
      const result = yield oracleUtil.query(sql);
      const windowMap = {};
      result.forEach(function(row) {
        if (windowMap[`${row[0]}_${row[1]}`]) {
          windowMap[`${row[0]}_${row[1]}`].push(row);
        } else {
          windowMap[`${row[0]}_${row[1]}`] = [row];
        }
      });
      const datasources = yield Datasource.find({ type: 'secondarypharmacytriage' });
      for (let j = 0; j < datasources.length; j++) {
        const datasource = datasources[j];
        const key = `${datasource.pharmacydeptno}_${datasource.pharmacywinno.join(',')}`;
        let cache = [];
        /* eslint-disable */
        datasource.pharmacywinno.forEach((winno, i) => {
          const k = `${datasource.pharmacydeptno}_${datasource.pharmacywinno[i]}`;
          if (windowMap[k]) {
            cache = cache.concat(windowMap[k]);
          }
        });
        /* eslint-enable */
        cache = JSON.stringify(cache);
        const md5 = crypto.md5(cache);
        const lastMd5 = yield redis.getFieldValueFromHashRedis('pharmacysecondary:' + key, 'md5');
        if (md5 != lastMd5) {
          yield redis.setValueToRedis('pharmacysecondary:' + key, {
            md5,
            data: cache
          }, 'hash');
          const boxes = yield Box.find({ datasource: datasource._id });
          const content = yield Utils.transformPharmacySecondaryData(cache, datasource.pharmacywinno.length > 1);
          for (let i = 0; i < boxes.length; i++) {
            yield socketios.emitData(boxes[i].no, { type: 'DATA', content });
          }
        }
      }
    } catch (err) {
      logger.error('syncSecondaryPharmacyData error', err);
    }
  },
  *clearDataCache() {
    try {
      const curDate = new Date();
      const datasources = yield Datasource.find().sort({ type: 1 });
      for (let i = 0; i < datasources.length; i++) {
        const datasource = datasources[i];
        let flag = false;
        if (datasource.morningcleartime) {
          const times = datasource.morningcleartime.split(':');
          const date = _.clone(curDate);
          date.setHours(times[0]);
          date.setMinutes(times[1]);
          date.setSeconds(times[2]);
          if (5 * 60 * 1000 > (curDate - date) && (curDate - date) >= 0) {
            flag = true;
          }
        }
        if (datasource.afternooncleartime) {
          const times = datasource.afternooncleartime.split(':');
          const date = _.clone(curDate);
          date.setHours(times[0]);
          date.setMinutes(times[1]);
          date.setSeconds(times[2]);
          if (5 * 60 * 1000 > (curDate - date) && (curDate - date) >= 0) {
            flag = true;
          }
        }
        if (flag) { // 清缓存及发送清屏命令
          logger.debug(`clear data, date: ${curDate}, datasource: ${JSON.stringify(datasource)}`);
          if (datasource.type == 'primarytriage' ||datasource.type == 'leveldepart') {
            const key = datasource.departmentid.join(',');
            yield redis.deleteKeyToRedis('primary:' + key);
          } else if (datasource.type == 'secondarytriage') {
            const key = datasource.screenid;
            yield redis.deleteKeyToRedis('secondary:' + key);
          } else if (datasource.type == 'secondarytriagesplit') {
            for (let j = 0; j < datasource.screensplitid.length; j++) {
              const key = datasource.screensplitid[j];
              yield redis.deleteKeyToRedis('secondary:' + key);
            }
          } else if (datasource.type == 'secondarytriageultrasonic') {
            for (let j = 0; j < datasource.queue.length; j++) {
              const key = datasource.queue[j];
              yield redis.deleteKeyToRedis('pacssecondary:' + key);
            }
          } else if (datasource.type == 'drawbloodtriage') {
            const key = datasource.windowid.join(',');
            yield redis.deleteKeyToRedis('drawblood:' + key);
          } else if (datasource.type == 'primarypharmacytriage') {
            const key = `${datasource.pharmacydeptno}_${datasource.pharmacywinno.join(',')}`;
            yield redis.deleteKeyToRedis('pharmacyprimary:' + key);
          } else if (datasource.type == 'secondarypharmacytriage') {
            const key = `${datasource.pharmacydeptno}_${datasource.pharmacywinno[0]}`;
            yield redis.deleteKeyToRedis('pharmacysecondary:' + key);
          }
          const boxes = yield Box.find({ datasource: datasource._id });
          for (let j = 0; j < boxes.length; j++) {
            yield socketios.emitData(boxes[j].no, { type: 'COMMAND', content: {
              cmd: 'cleardata'
            } });
          }
        }
      }
    } catch (err) {
      logger.error('clearDataCache error', err);
    }
  }
};
