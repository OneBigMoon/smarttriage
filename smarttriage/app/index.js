/**
 * 前端入口
 */
'use strict';

const Vue = require('vue');
const Router = require('vue-router');
const VueForm = require('vue-form');
const App = require('./app');
const Utils = require('./utils');
const Directive = require('./utils/directive');
const Filter = require('./utils/filter');
const Title = require('./utils/title');

Vue.config.debug = true;
Vue.config.silent = !Vue.config.debug;

// 注册路由插件
Vue.use(Router);

// 注册表单插件
Vue.use(VueForm);

// 注册指令
Vue.use(Directive);

// 注册过滤器
Vue.use(Filter);

// 路由
const router = new Router({ history: true });
const routes = require('./route');
const auth = require('./services/auth');

router.map(routes);

const defaultPath = '/box';
router.beforeEach((transition) => {
  window.scrollTo(0, 0);
  if (transition.to.auth && !auth.authority(transition.to.auth)) {
    router.go('/login');
  }
  transition.next();
});

router.afterEach((transition) => {
  Title.setCategory(transition.to.name);
});

router.redirect({
  '*': defaultPath
});

router.start(App, '#app');

Utils.go = (path) => router.go(path);
