'use strict';

const _ = require('lodash');
const Router = require('koa-router');
const Static = require('koa-static');
const convert = require('koa-convert');
const Utils = require('./utils');
const logger = Utils.logger(__filename);

const ctrls = require('./controllers');
const users = require('./controllers/users');
const boxes = require('./controllers/boxes');
const orgnizations = require('./controllers/orgnizations');
const styles = require('./controllers/styles');
const datasourcetypes = require('./controllers/datasourcetypes');
const datasources = require('./controllers/datasources');
const upgrades = require('./controllers/upgrades');
const systems = require('./controllers/systems');
const uploads = require('./controllers/uploads');
const dir = require('../config/base').dir;

const apiV1Params = {
  params: ctrls.parseParams
};

const apiV1Routes = {
  // ======平台====== //
  // ------登陆登出------ //
  '/user/login': {
    post: users.login
  },
  '/user/logout': {
    post: users.logout
  },
  // ------终端------ //
  '/boxes/query': {
    post: boxes.query,
    auth: ['AUTH_ROOT']
  },
  '/boxes/move': {
    post: boxes.move,
    auth: ['AUTH_ROOT']
  },
  '/boxes/remove': {
    post: boxes.remove,
    auth: ['AUTH_ROOT']
  },
  '/boxes/save': {
    post: boxes.save,
    auth: ['AUTH_ROOT']
  },
  '/boxes/dataenabled/toggle': {
    post: boxes.toggleDataEnabled,
    auth: ['AUTH_ROOT']
  },
  '/boxes/dataenabled/enable-all': {
    post: boxes.enableAllData,
    auth: ['AUTH_ROOT']
  },
  '/boxes/power': {
    post: boxes.power,
    auth: ['AUTH_ROOT']
  },
  '/boxes/group/power': {
    post: boxes.groupPower,
    auth: ['AUTH_ROOT']
  },
  '/boxes/group/save': {
    post: boxes.groupSave,
    auth: ['AUTH_ROOT']
  },
  '/boxes/upload-log': {
    post: boxes.uploadLog,
    auth: ['AUTH_ROOT']
  },
  '/boxes/check-log': {
    post: boxes.checkLog,
    auth: ['AUTH_ROOT']
  },
  '/boxes/download-log': {
    get: boxes.downloadLog
  },
  // ------机构------ //
  '/orgnizations/query': {
    post: orgnizations.query,
    auth: ['AUTH_ROOT']
  },
  '/orgnizations/save': {
    post: orgnizations.save,
    auth: ['AUTH_ROOT']
  },
  '/orgnizations/remove': {
    post: orgnizations.remove,
    auth: ['AUTH_ROOT']
  },
  // ------样式------ //
  '/styles/query': {
    post: styles.query,
    auth: ['AUTH_ROOT']
  },
  // ------数据源类型------ //
  '/datasourcetypes/query': {
    post: datasourcetypes.query,
    auth: ['AUTH_ROOT']
  },
  // ------数据源------ //
  '/datasources/query': {
    post: datasources.query,
    auth: ['AUTH_ROOT']
  },
  '/datasources/remove': {
    post: datasources.remove,
    auth: ['AUTH_ROOT']
  },
  '/datasources/save': {
    post: datasources.save,
    auth: ['AUTH_ROOT']
  },
  // 系统
  '/system': {
    post: systems.get,
    auth: ['AUTH_ROOT']
  },
  '/system/save': {
    post: systems.save,
    auth: ['AUTH_ROOT']
  },
  // ------终端------ //
  '/terminal/querystyles': {
    post: styles.query
  },
  '/terminal/querydatasources': {
    post: datasources.query
  },
  // ------上传------ //
  '/upload': {
    post: uploads.upload
  },
  // ------升级------ //
  '/upgrade/query': {
    post: upgrades.query,
    auth: ['AUTH_ROOT']
  },
  '/upgrade/remove': {
    post: upgrades.remove,
    auth: ['AUTH_ROOT']
  },
  '/upgrade': {
    post: upgrades.upgrade
  },
  '/upgrade/download': {
    get: upgrades.download
  }
};

module.exports = (app) => {
  const base = new Router();
  const apiV1 = new Router();

  apiV1
    .use(ctrls.handleError, ctrls.setNoCacheControl);

  _.each(apiV1Params, (middleware, param) => apiV1.param(param, middleware));

  const adaptController = (ctrl) => {
    if (typeof ctrl !== 'function') {
      return ctrl;
    }
    if (ctrl.constructor && ctrl.constructor.name === 'GeneratorFunction') {
      return convert(ctrl);
    }
    return ctrl;
  };

  _.each(apiV1Routes, (routes, path) => {
    const errMsg = '路由配置错误, path: %s, method: %s';
    _.each(routes, (ctrl, method) => {
      let currentRoute;
      let routerParams;

      if (/auth|role|action/.test(method)) {
        return;
      }

      currentRoute = _.isFunction(ctrl) || _.isArray(ctrl) ? { ctrl } : ctrl;

      try {
        currentRoute.method = method;

        if (routes.auth != null && currentRoute.auth == null) { // 权限
          currentRoute.auth = routes.auth;
        }

        if (routes.role != null && currentRoute.role == null) { // 角色
          currentRoute.role = routes.role;
        }

        if (routes.action != null && currentRoute.action == null) { // ActionLog
          currentRoute.action = routes.action;
        }

        routerParams = [path, ctrls.setRouteOfContext(currentRoute)];
        if (currentRoute.auth != null) {
          // 权限
          routerParams.push(ctrls.auth);
        }

        if (_.isArray(currentRoute.ctrl)) {
          currentRoute.ctrl.forEach(function(ctrlFn) {
            if (_.isFunction(ctrlFn)) {
              routerParams.push(ctrlFn);
            } else {
              throw new Error('ctrl is not a function');
            }
          });
        } else {
          if (_.isFunction(currentRoute.ctrl)) {
            routerParams.push(adaptController(currentRoute.ctrl));
          } else {
            throw new Error('ctrl is not a function');
          }
        }
        apiV1[method].apply(apiV1, routerParams);
      } catch (e) {
        logger.error(Utils.format(errMsg, path, method), e);
      }
    });
  });
  base
    .use(ctrls.touchSession, ctrls.setCookie)
    .use('/api/v1', apiV1.routes(), apiV1.allowedMethods())
    .use(Static(dir.static))
    .all('/*', ctrls.index);

  app
    .use(base.routes())
    .use(base.allowedMethods());
};
