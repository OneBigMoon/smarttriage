<template>
  <div id="app">
    <!-- <page-header
      :offices="offices"
      :office.sync="office"
      v-show="showHeader">
    </page-header> -->
    <navbar
      :title="title"
      :sidenav="sidenav"
      :offices="offices"
      :office.sync="office"
      :username="username">
    </navbar>
    <router-view
      class="container-fluid content-div">
    </router-view>
    <alert
      id="alert"
      :show.sync="alert.show"
      :duration="alert.duration"
      :type="alert.type"
      width="280px"
      placement="top-right"
      dismissable>
      <div class="content" @click="alertCallback">
        <p>{{alert.msg}}</p>
      </div>
    </alert>
    <loading :show="loading"></loading>
  </div>
</template>

<script>
const Auth = require('./services/auth');
const Utils = require('./utils');

const Alert = require('./components/v-alert');
const Navbar = require('./components/navbar');
const PageHeader = require('./views/user/header');
const Loading = require('./components/loading');
const routes = require('./route');

const {
  each,
  noop,
  cookie,
  handleError
} = Utils;

const noauthPaths = ['/login', '/noaccess', '/setCookie2'];


exports.components = {
  PageHeader,
  Alert,
  Loading,
  Navbar
};

exports.data = () => ({
  alert: {
    show: false,
    msg: '',
    type: '',
    duration: 0,
    icon: {
      success: 'check',
      info: 'info',
      warning: 'exclamation',
      danger: 'times'
    },
    callback: noop
  },
  loading: 0,
  offices: [],
  office: '',
  showHeader: false,
  page: 'index',
  sidenav: []
});

exports.methods = {
  alertCallback() {
    this.alert.show = false;
    this.alert.callback();
  },
  select(param) {
    this.page = param;
  },
  refresh(isLogin) {
    const _this = this;
    _this.$broadcast('app.refresh');

    const sideObj = {
      box: { icon: 'television', name: '设备管理', children: [] },
      data: { icon: 'database', name: '接口管理', children: [] },
      org: { icon: 'cubes', name: '分组管理', children: [] },
      upgrade: { icon: 'upload', name: '升级管理', children: [] },
      sys: { icon: 'cog', name: '系统管理', children: [] }
    };
    each(routes, (route, link) => {
      if (sideObj[route.type]) {
        sideObj[route.type].children.push({
          link,
          name: route.name,
          icon: route.nav
        });
      }
    });
    _this.sidenav = [];
    each(sideObj, (value, key) => {
      value.key = key;
      _this.sidenav.push(value);
    });

    _this.offices = [];
    // _this.office = 0;

    if (!isLogin) {
      let found = false;
      const toPath = _this.$route.path;
      each(noauthPaths, (noauthPath) => {
        if (toPath.indexOf(noauthPath) >= 0) {
          found = true;
        }
      });
      _this.showHeader = !found;
      if (toPath.indexOf('/noaccess') >= 0) {
        _this.showHeader = true;
      }
      if (found) {
        return;
      }
    }
    _this.showHeader = true;

    Auth.offices((officeArray) => {
      const newOffices = [];
      Utils.genSelectStructure(officeArray, newOffices);
      if (newOffices.length) {
        each(newOffices, (office) => {
          _this.offices.push(office);
        });
        // _this.office = newOffices[0].children[0].children[0].children[0].value;
      }
    }, handleError);
  }
};

exports.events = {
  login() {
    this.refresh(true);
  }
};

exports.watch = {
  office(value) {
    cookie.set('offices.office', value);
    this.$broadcast('office', value);
  }
};

exports.ready = function() {
  const _this = this;
  _this.refresh();
  Utils.$global = _this;
};
</script>

<style lang="stylus">
@import "styles/index"
$space-padding = 10px
#app
  position: absolute
  width: 100%
  top: 0px
  bottom: 30px
  overflow-y: auto
  .content-div
    padding: 69px 20px 0 180px
    height: 100%
/*#container
  padding: $space-padding
  &.v
    &-transition
      transition: all .3s
      transform-origin: 50% 100%
      z-index: 1
    &-enter
    &-leave
      absolute: top 0 left 0
      width: 100%
      transform: scale3d(.9, .9, .9)
      opacity: 0

.glyphicon
  @extend .fa
  &-ok
    &:before
      content: $fa-var-check
      color: $brand-primary*/

// 修复safari浏览器中使用iframe嵌入服务界面显示异常：不显示滚动条、不支持fixed定位
html,body
  height: 100%

/*.container-fluid
  position: absolute
  top: 90px
  bottom: 0
  height:
  left: 0
  width: 100%
  overflow-y: auto
  -webkit-overflow-scrolling: touch
  padding-top: 5px !important*/

.modal-dialog
  position: absolute
  left: 0
  right: 0
  top: 50%
  transform: translate(0, -50%) !important
.bottom-slider
  position: fixed
  height: 40px
  width: 100%
  display: flex
  bottom: 0
  background: #FFFFFF
  border-top: 1px solid #F4F2F0
  .item
    flex: 1
    margin: 10px 0px
    text-align: center
    font-size: medium
    color: #939393
    border-right: 1px solid #939393
    &:last-child
      border-right: 0px
    &.active
      color: #20BBA6
    .icon
      margin-right: 4px
.bottom-div
  width: 100%
  height: 40px
</style>
