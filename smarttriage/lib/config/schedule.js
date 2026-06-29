'use strict';

const _ = require('lodash');
const schedule = require('node-schedule');
const moment = require('moment');

const Utils = require('../utils');
const logger = Utils.logger(__filename);
const Model = require('../models');
const log4jsConfig = require('./log4js');

let systems;
let apis;
let boxes;
let appName;

let Schedule;
function getScheduleModel() {
  return Schedule || (Schedule = Model('Schedule'));
}

async function run(name, job) {
  try {
    // 保证每次仅有一个进程在执行同一个任务
    const numberAffected = await getScheduleModel().update({
      _id: name,
      exec_date: { $lte: +moment().add(-2, 's') }
    }, {
      $set: { exec_date: +moment() }
    }, {
      multi: true
    });
    if (numberAffected && numberAffected.n) {
      const startDate = new Date().getTime();
      logger.info(`Schedule ${name} start`);
      await job();
      const endDate = new Date().getTime();
      logger.info(`Schedule ${name} finished, span time ${(endDate - startDate) / 1000} seconds`);
    } else {
      logger.debug(`Schedule ${name} already executed`);
    }
  } catch (err) {
    logger.error(`Schedule ${name} error`, err);
  }
}

async function scheduleJob(cronStr, name, job) {
  Schedule = getScheduleModel();
  name = _.camelCase(appName + ' ' + name);

  try {
    // 如果任务不存在则新建
    let s = await Schedule.findById(name);
    if (!s) {
      s = await new Schedule({ _id: name }).save();
    }

    schedule.scheduleJob(cronStr, () => run(name, job));
    logger.debug(`Schedule ${name} created`);
  } catch (err) {
    logger.error(`Schedule ${name} create error`, err);
  }
}

async function syncCacheConf() {
  await systems.initCache();
  await apis.initCache();
}

async function clearDataCache() {
  await apis.clearDataCache();
}

async function syncPrimaryData() {
  await apis.syncPrimaryData();
}

async function syncSecondaryData() {
  await apis.syncSecondaryData();
}

async function syncSecondaryUltrasonicData() {
  await apis.syncSecondaryUltrasonicData();
}

async function syncDrawbloodData() {
  await apis.syncDrawbloodData();
}

async function syncPrimaryPharmacyData() {
  await apis.syncPrimaryPharmacyData();
}

async function syncSecondaryPharmacyData() {
  await apis.syncSecondaryPharmacyData();
}

async function checkBoxDisconnect() {
  await boxes.checkBoxDisconnect();
}

module.exports = (Config) => {
  systems = require('../controllers/systems');
  apis = require('../controllers/apis');
  boxes = require('../controllers/boxes');

  appName = Config.app.name;
  // 每天 00:30 刷新cacheconf
  scheduleJob({
    hour: 0,
    minute: 30,
    second: 0
  }, 'syncCacheConf', syncCacheConf).then(function() {
    run('smarttriageSyncCacheConf', syncCacheConf); // 启动时初始化redis
  }).then(function() {
    // 每3秒执行一级分诊数据获取
     scheduleJob('*/3 * * * * *', 'syncPrimaryData', syncPrimaryData);
    // 每3秒执行二级分诊数据获取
    scheduleJob('*/3 * * * * *', 'syncSecondaryData', syncSecondaryData);
    // 每3秒执行二级超声数据获取
     scheduleJob('*/3 * * * * *', 'syncSecondaryUltrasonicData', syncSecondaryUltrasonicData);
    // 每3秒执行检验数据获取
    scheduleJob('*/3 * * * * *', 'syncDrawbloodData', syncDrawbloodData);
    // 每3秒执行一级药房数据获取
    scheduleJob('*/3 * * * * *', 'syncPrimaryPharmacyData', syncPrimaryPharmacyData);
    // 每3秒执行二级药房数据获取
    scheduleJob('*/3 * * * * *', 'syncSecondaryPharmacyData', syncSecondaryPharmacyData);
    // 每15秒执行终端断线检查
    scheduleJob('*/15 * * * * *', 'checkBoxDisconnect', checkBoxDisconnect);
  });
  // 每2分钟执行缓存检查,并清除过期缓存
  scheduleJob('0 */2 * * * *', 'clearDataCache', clearDataCache);
  // 每天 01:00 日志清理
  scheduleJob('0 0 1 * * *', 'cleanLogs', log4jsConfig.cleanLogs);
};
