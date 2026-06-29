'use strict';

const send = require('koa-send');
const _ = require('lodash');

const Config = require('../config');
const Utils = require('../utils');
const i18n = Utils.i18n;
const logger = Utils.logger(__filename);
const handleError = Utils.handleError(logger);
const skipPaths = ['/api/v1/login', '/api/v1/usernamemsg', '/api/v1/branch/',
 '/api/v1/dashboard/monitor/', '/api/v1/syslogs/server-info'];


function unAuthorized(path) {
  let unAuth = true;
  skipPaths.forEach((pa) => {
    if (path.indexOf(pa) == 0) {
      unAuth = false;
    }
  });
  return unAuth;
}
module.exports = {
  /**
   * 返回 index.html
   */
  async index(ctx) {
    await send(ctx, '/index.html', { root: Config.dir.frontend });
  },
  async auth(ctx, next) {
    if (unAuthorized(ctx.path) && !ctx.state.user) {
      ctx.status = 401;
      return;
    }

    if (ctx.state.user && _.intersection(ctx.state.user.auths, ['AUTH_ROOT']).length === 0) {
      if (ctx.route.auth != null) {
        if (_.intersection(ctx.state.user.auths, ctx.route.auth).length === 0) {
          ctx.status = 401;
          return;
        }
      }

      if (ctx.route.role != null) {
        if (_.intersection(ctx.state.user.userroles, ctx.route.role).length === 0) {
          ctx.status = 401;
          return;
        }
      }
    }

    await next();
  },

  async parseParams(params, ctx, next) {
    if (params) {
      logger.debug(ctx.path, ctx.params = Utils.parseJson(params));
    }
    await next();
  },

  async handleError(ctx, next) {
    try {
      await next();
    } catch (e) {
      handleError(i18n.__('error.Server'), e, ctx);
    }
  },

  /**
   * 更新session，重新计算过期时间
   */
  async touchSession(ctx, next) {
    await next();
    if (ctx.state.user) {
      ctx.session._garbage = Date.now();
    }
    if (ctx.userSocketio) {
      ctx.session._garbage = Date.now();
    }
  },

  /**
   *  设置cookie中的当前用户和项目
   */
  async setCookie(ctx, next) {
    await next();
    ctx.cookies.set(`${Config.app.name}.lang`, Config.lang, { httpOnly: false }); // 设置语言
    ctx.cookies.set(`${Config.app.name}.user`, ctx.state.user ?
      encodeURIComponent(JSON.stringify(ctx.state.user)) : '',
      {
        httpOnly: false
      });
  },

  /**
   * 禁用响应缓存
   */
  async setNoCacheControl(ctx, next) {
    ctx.set('Cache-Control', 'no-cache');
    ctx.set('Pragma', 'no-cache');
    ctx.set('Expires', 0);
    await next();
  },

  /**
   * 在当前上下文中保存路由对象
   */
  setRouteOfContext(currentRoute) {
    return async (ctx, next) => {
      ctx.route = currentRoute;
      await next();
    };
  }
};
