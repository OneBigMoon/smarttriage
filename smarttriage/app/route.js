'use strict';

module.exports = {
  '/box': {
    name: '终端管理',
    type: 'box',
    auth: ['AUTH_ROOT'],
    component(resolve) {
      require(['./views/box'], resolve);
    }
  },
  '/data': {
    name: '接口管理',
    type: 'data',
    auth: ['AUTH_ROOT'],
    component(resolve) {
      require(['./views/data'], resolve);
    }
  },
  '/org': {
    name: '分组管理',
    type: 'org',
    auth: ['AUTH_ROOT'],
    component(resolve) {
      require(['./views/org'], resolve);
    }
  },
  '/upgrade': {
    name: '升级管理',
    type: 'upgrade',
    auth: ['AUTH_ROOT'],
    component(resolve) {
      require(['./views/upgrade'], resolve);
    }
  },
  '/sys': {
    name: '系统管理',
    type: 'sys',
    auth: ['AUTH_ROOT'],
    component(resolve) {
      require(['./views/sys'], resolve);
    }
  },
  '/login': {
    name: '登录',
    component(resolve) {
      require(['./views/user/login'], resolve);
    }
  },
  '/noaccess': {
    name: '无权限',
    component(resolve) {
      require(['./views/user/noaccess'], resolve);
    }
  }
};
