/**
 * Auth Service
 */
'use strict';

const Config = require('../config');
const Utils = require('../utils');
const ApiV1 = require('./api-v1');
const crypto = require('crypto');

exports.storage = {
  managers: {}
};

const exportsUser = exports.user = () => {
  let user;
  try {
    user = JSON.parse(Utils.cookie.get(Config.cookie.user));
  } catch (e) { e; }
  return user || {};
};

const authority = exports.authority = (auths, userroles) => {
  const user = exportsUser();
  if (!user || !user.username) {
    return false;
  }

  if (user.auths && user.auths.indexOf('AUTH_ROOT') >= 0) {
    return true;
  }

  let found = false;
  if (!auths || !auths.length) {
    return true; // found;
  }

  userroles = userroles || user.userroles;
  if (!userroles || !userroles.length) {
    return found;
  }

  const authMap = {};
  Utils.each(auths, (auth) => {
    authMap[auth] = auth;
  });

  Utils.each(userroles, (role) => {
    authMap[role] && (found = true);
  });

  return found;
};

const getFirstPath = exports.getFirstPath = () => {
  if (authority()) {
    return '/';
  }

  return '/noaccess';
};

exports.isLoggedIn = () => {
  const user = exportsUser();
  return !!(user && user.name);
};

exports.login = (user, onSuccess, onFail) => {
  let encrypted = '';
  const cipher = crypto.createCipher('aes-256-cbc', Config.app.pwdkey);
  encrypted += cipher.update(user.password, 'binary', 'hex');
  encrypted += cipher.final('hex');
  user.password = encrypted;

  ApiV1.login.post(user, () => {
    onSuccess && onSuccess();
    Utils.go({
      path: getFirstPath(),
      replace: true
    });
  }, onFail);
};

exports.logout = () => {
  ApiV1.login.delete(() => Utils.go({
    path: '/login',
    replace: true
  }));
};

exports.fix = ApiV1.fix.get;
exports.offices = ApiV1.offices.get;
exports.baseconf = ApiV1.baseconf.get;

exports.checkSession = () => {
  const cid = Utils.cookie.get('safarisession');
  return !!cid;
};
