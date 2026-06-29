'use strict';

const _ = require('lodash');

const strategies = {};

function PassportError(msg) {
  const _this = this;
  Error.call(_this);
  _this.stack = new Error().stack;
  _this.message = msg;
}

_.assign(PassportError.prototype = Object.create(Error.prototype), {
  constructor: Error,
  name: 'PassportError'
});

const passport = module.exports = {
  field: 'user',
  sessionField: 'user',
  async login(type, ctx) {
    const user = await strategies[type](_.assign({}, ctx.request.body, ctx.query, ctx.params), ctx);
    if (!_.keys(user).length) {
      throw new PassportError('Can not resolve user');
    }
    if (typeof passport._serializeUser != 'function') {
      throw new PassportError('"passport.serializeUser" not valid');
    }
    const serializedUser = await passport._serializeUser(user._info);
    ctx.session[passport.sessionField] = serializedUser;
    ctx.state[passport.field] = user._info;
    ctx[passport.field] = user._info;
    return ctx.state[passport.field];
  },
  async logout(ctx) {
    ctx.session[passport.sessionField] = undefined;
    ctx.state[passport.field] = undefined;
    ctx[passport.field] = undefined;
    return Promise.resolve();
  },
  middleware(config) {
    _.assign(passport, _.pick(config, [
      'field',
      'sessionField'
    ]));
    return async (ctx, next) => {
      const userSession = ctx.session && ctx.session[passport.sessionField];
      if (userSession) {
        if (typeof passport._deserializeUser != 'function') {
          throw new PassportError('"passport.deserializeUser" not valid');
        }
        const user = await passport._deserializeUser(userSession);
        _.keys(user).length && (ctx.state[passport.field] = ctx[passport.field] = user);
      }
      await next();
    };
  },
  use(type, strategy) {
    strategies[type] = strategy;
  },
  serializeUser(serializeUserFunction) {
    passport._serializeUser = serializeUserFunction;
  },
  deserializeUser(deserializeUserFunction) {
    passport._deserializeUser = deserializeUserFunction;
  },
  PassportError
};
