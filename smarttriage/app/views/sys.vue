<template>
  <div id="system">
    <div class="row" style="height: 100%;">
      <div class="col-md-12" style="height: 100%;">
        <div class="panel panel-default" style="height: 100%;margin-bottom: 0;">
          <div class="panel-heading">医疗数据库配置</div>
          <div class="panel-body">
            <form class="form-horizontal" v-on:submit.prevent>
              <div class="form-group">
                <label class="col-sm-2 control-label">URL</label>
                <div class="col-sm-5">
                  <input type="text" class="form-control" v-model="model.url">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">分诊账户</label>
                <div class="col-sm-5">
                  <input type="text" class="form-control" v-model="model.username">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">分诊密码</label>
                <div class="col-sm-5">
                  <input type="password" class="form-control" v-model="model.password">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">一级分诊表/视图名</label>
                <div class="col-sm-5">
                  <input type="text" class="form-control" v-model="model.primarytablename">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">二级分诊表/视图名</label>
                <div class="col-sm-5">
                  <input type="text" class="form-control" v-model="model.secondarytablename">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">二级分诊人数表/视图名</label>
                <div class="col-sm-5">
                  <input type="text" class="form-control" v-model="model.secondarycounttablename">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">检验表/视图名</label>
                <div class="col-sm-5">
                  <input type="text" class="form-control" v-model="model.drawbloodtablename">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">药房表/视图名</label>
                <div class="col-sm-5">
                  <input type="text" class="form-control" v-model="model.pharmacytablename">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">超声账户</label>
                <div class="col-sm-5">
                  <input type="text" class="form-control" v-model="model.pacsusername">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">超声密码</label>
                <div class="col-sm-5">
                  <input type="password" class="form-control" v-model="model.pacspassword">
                </div>
              </div>
              <div class="form-group">
                <label class="col-sm-2 control-label">二级超声表/视图名</label>
                <div class="col-sm-5">
                  <input type="text" class="form-control" v-model="model.pacssecondarytablename">
                </div>
              </div>
              <div class="form-group">
                <div class="col-sm-offset-2 col-sm-5">
                  <button class="btn btn-primary" @click="save">保存</button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const Empty = require('../components/empty');
const Pagination = require('../components/pagination');
const ApiV1 = require('../services/api-v1');
const {
  handleError,
  handleSuccess
} = require('../utils');

exports.components = {
  Empty,
  Pagination
};

exports.data = () => {
  return {
    model: {
      url: '',
      primarytablename: '',
      secondarytablename: '',
      secondarycounttablename: '',
      drawbloodtablename: '',
      pharmacytablename: '',
      username: '',
      password: '',
      pacssecondarytablename: '',
      pacsusername: '',
      pacspassword: ''
    }
  };
};

exports.methods = {
  query() {
    ApiV1.system.post({}, (data) => {
      this.model = data.result;
    }, () => {
      handleError({ message: '查询失败' });
    });
  },
  save() {
    if (!this.model.url) {
      handleError({ message: '请填写URL' });
      return;
    }
    ApiV1.systemSave.post(this.model, () => {
      handleSuccess({ message: '保存成功' });
      this.query();
    }, () => {
      handleError({ message: '保存失败' });
    });
  }
};

exports.ready = function() {
  this.query();
};
</script>

<style lang="stylus">
#system
  .btn
    border-radius: 4px
    width: 60px
    padding-bottom: 2px
    padding-top: 2px

</style>
