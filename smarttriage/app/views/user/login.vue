<template>
  <div id="login">
    <div id="login-modal">
      <div class="dialog modal zoom in">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-body">
              <div id="login-logo"></div>
              <form class="login-form" name="form" @submit.prevent="login" v-form>
                <div class="form-group">
                  <label for="name" class="control-label">
                    用户名
                    <div class="form-notice">
                      <i class="fa fa-warning"></i>
                      <span v-if="form.name.$error.required">用户名必填</span>
                      <span v-if="form.name.$error.right">用户名或密码错误</span>
                    </div>
                  </label>
                  <div class="login-input">
                    <input type="text" name="name" id="name"
                           class="form-control input-lg"
                           v-model="model.username" v-form-ctrl v-form-valid required>
                    <i class="fa fa-fw fa-user"></i>
                  </div>
                </div>
                <div class="form-group">
                  <label for="password" class="control-label">
                    密&emsp;码
                    <div class="form-notice">
                      <i class="fa fa-warning"></i>
                      <span v-if="form.password.$error.required">密码必填</span>
                      <span v-else>{{error}}</span>
                    </div>
                  </label>
                  <div class="login-input">
                    <input type="password" name="password" id="password"
                           class="form-control input-lg"
                           v-model="model.password" v-form-ctrl v-form-valid required>
                    <i class="fa fa-fw fa-key"></i>
                  </div>
                </div>
                <div class="form-group row">
                  <br>
                  <div class="col-xs-12 col-sm-7">
                    <button type="submit" class="btn btn-block btn-success btn-lg">
                      登录
                    </button>
                  </div>
                  <div class="col-xs-12 col-sm-5">
                    <checkbox :model.sync="remember">记住用户名</checkbox>
                  </div>
                </div>
              </form>
            </div>
          </div>
          <p class="footer text-center">&copy; 福建星网锐捷通讯股份有限公司</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const Config = require('../../config');
const Auth = require('../../services/auth');

const Checkbox = require('../../components/checkbox');

const COOKIE_KEY = Config.cookie.remember;

const {
  cookie,
  handleError
} = require('../../utils');

exports.components = {
  Checkbox
};

exports.data = () => {
  const remember = cookie.get(COOKIE_KEY);
  return {
    title: Config.app.title,
    form: {},
    model: {
      username: remember || '',
      password: ''
    },
    remember: !!remember
  };
};

exports.methods = {
  login() {
    const _this = this;
    const {
      form,
      model,
      remember
    } = _this;

    if (form.$valid) {
      Auth.login(model, () => {
        cookie.set(COOKIE_KEY, remember ? model.username : '', 100000000000);
        _this.$dispatch('login');
      }, (err) => {
        model.password = '';
        handleError(err);
      });
    }
  }
};

</script>

<style lang="stylus">
@import "../../styles/variables"
@import "../../styles/mixins"

#login
  .modal
    &.in
      display: block
      background: $white
      background-size: cover
    &-dialog
      width: 500px
      top: 50% !important
      left: 50% !important
      transform: translate(-50%,-50%) !important

      @media (min-width: $grid-float-breakpoint)
        .login-form
          margin: 10px 50px

    &-content
      box-shadow: 0 8px 10px rgba($black, .2)
      background: $white

  &-logo
    background: $white url('../../assets/logo.png') no-repeat 50%
    height: 45px
    background-size: contain
    margin-bottom: 15px

  .header
  .footer
    color: $white
    margin: 10px 0

  .login
    &-input
      position: relative

      .form-control
        padding-left: 30px

      .fa
        absolute: top $padding-large-vertical left 6px
        color: #a5a5a5
        font-size: $font-size-large
        line-height: $line-height-base

  .control-label
    display: block

  .form-notice
    display: none

  .has-warning
    .form-notice
      display: block
      float: right
      color: $state-warning-text

  .checkbox
    margin: ($padding-large-vertical + ($font-size-large - $font-size-base) / 2) 0
</style>
