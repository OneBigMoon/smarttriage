/**
 * ApiV1 Service
 */
'use strict';

const Utils = require('../utils');
const http = require('../utils/http');

function resource(path, noLoading) {
  return http(`/api/v1${path}`, { noLoading });
}

module.exports = {
  login: resource('/user/login'),
  logout: resource('/user/logout'),
  boxes: resource('/boxes/query'),
  boxMove: resource('/boxes/move'),
  boxRemove: resource('/boxes/remove'),
  boxPower: resource('/boxes/power'),
  boxSave: resource('/boxes/save'),
  boxToggleDataEnabled: resource('/boxes/dataenabled/toggle'),
  boxEnableAllData: resource('/boxes/dataenabled/enable-all'),
  boxGroupPower: resource('/boxes/group/power'),
  boxGroupSave: resource('/boxes/group/save'),
  orgnizations: resource('/orgnizations/query'),
  orgnizationSave: resource('/orgnizations/save'),
  orgnizationRemove: resource('/orgnizations/remove'),
  styles: resource('/styles/query'),
  datasourcetypes: resource('/datasourcetypes/query'),
  datasources: resource('/datasources/query'),
  datasourceRemove: resource('/datasources/remove'),
  datasourceSave: resource('/datasources/save'),
  system: resource('/system'),
  systemSave: resource('/system/save'),

  fix: resource('/fix'),
  offices: resource('/user/offices', true),
  monitor: resource('/monitor/:officeid'),
  baseconf: resource('/baseconf/:officeid', true),
  ticket: resource('/ticket'),
  mapData: resource('/map-database/:level/:type/:date'),
  counteroffices: resource('/offices/:name'),
  attnofficesbase: resource('/user/:name/attnoffices/baseinfo'),
  attnoffices: resource('/user/:name/attnoffices'),

  uploadLog: resource('/boxes/upload-log'),
  checkLog: resource('/boxes/check-log'),
  downloadLog: resource('/boxes/download-log'),

  upgrades: resource('/upgrade/query'),
  upgradeRemove: resource('/upgrade/remove')
};

http.before((requestUrl, data, method, options) => {
  if (!options.noLoading) {
    Utils.loading(true);
  }
});

http.after((text, xhr, options) => {
  if (!options.noLoading) {
    Utils.loading(false);
  }

  if (xhr.status == 401) {
    Utils.go({
      path: '/noaccess', // 无权限界面
      replace: true
    });
    return false;
  }
});
