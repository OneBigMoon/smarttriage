<template>
  <div id="upgrade">
    <div class="panel panel-default" style="height: 100%;margin-bottom: 0;overflow-y: auto;">
      <div class="panel-body" style="height:100%;">
        <div class="row" style="height:100%;">
          <div class="col-md-12" style="height:100%;">
            <div class="row" style="padding-bottom:20px;">
              <div class="col-md-3">名称
                <input type="text" v-model="search.name" style="width: 70%;"/>
              </div>
              <div class="col-md-3">
                <button type="button" class="btn btn-primary" @click="query">查询</button>
              </div>
            </div>
            <div class="row" style="padding-bottom:20px;">
              <div class="col-md-12">
                <div class="btn-group" role="group">
                  <button type="button" class="btn btn-primary" @click="edit('new')">新增</button>
                  <button type="button" class="btn btn-primary" @click="confirmShow=true"
                   :disabled="!selectUpgrade._id">删除</button>
                 </div>
              </div>
            </div>
            <div class="row" style="height: calc(100% - 110px);overflow-y: auto;">
              <div class="col-md-12">
                <table class="table table-hover table-bordered">
                  <thead>
                    <tr>
                      <th style="width:50px;">#</th>
                      <th style="width:20%;">文件名</th>
                      <th style="width:20%;">model</th>
                      <th style="width:20%;">appVersion</th>
                      <th>时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="upgrade in upgrades"
                     :class="['upgrade', selectUpgrade._id == upgrade._id ? 'selected' : '' ]"
                     @click="select(upgrade)">
                      <th scope="row">{{$index + 1}}</th>
                      <td>{{upgrade.originname}}</td>
                      <td>{{upgrade.model}}</td>
                      <td>{{upgrade.appVersion}}</td>
                      <td>{{upgrade.ut}}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <dialog title="" :show.sync="confirmShow" :confirm="confirmRemove">
      <div class="text-center" slot="dialog-body">
        <i class="fa text-warning"></i>&emsp;确认删除？
      </div>
    </dialog>
    <dialog title="" :show.sync="editShow" :confirm="editConfirm" :width="700">
      <div style="height:450px;" slot="dialog-body">
        <form class="form-horizontal" v-el:form action="/api/v1/upload?type=upgrade" method="post"
         enctype="multipart/form-data" target="nm_iframe">
          <div class="form-group">
            <label class="col-sm-2 control-label">文件</label>
            <div class="col-sm-8">
              <input type="file" v-model="model.file" name="file">
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">model</label>
            <div class="col-sm-8">
              <input type="text" class="form-control" v-model="model.model" name="model">
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">appVersion</label>
            <div class="col-sm-8">
              <input type="text" class="form-control" v-model="model.appVersion" name="appVersion">
            </div>
          </div>
        </form>
      </div>
    </dialog>
    <iframe name="nm_iframe" style="display:none;"></iframe>
  </div>
</template>

<script>
const Empty = require('../components/empty');
const Tree = require('../components/tree');
const VSelect = require('../components/v-select');
const Pagination = require('../components/pagination');
const Dialog = require('../components/dialog');
const TimePicker = require('../components/timepicker');
const ApiV1 = require('../services/api-v1');
const {
  handleError, handleSuccess
} = require('../utils');

exports.components = {
  Empty,
  Pagination,
  VSelect,
  Tree,
  Dialog,
  TimePicker
};

exports.data = () => {
  return {
    search: {
      name: ''
    },
    upgrades: [],
    selectUpgrade: { _id: null },
    confirmShow: false,
    editShow: false,
    model: {
      id: null,
      name: null,
      originname: null,
      file: null,
      model: null,
      appVersion: null,
      path: null,
      ut: null
    }
  };
};

exports.methods = {
  query() {
    ApiV1.upgrades.post(this.search, (data) => {
      this.upgrades = data.result;
      this.selectUpgrade = { _id: null };
    }, () => {
      handleError({ message: '查询失败' });
    });
  },
  select(upgrade) {
    this.selectUpgrade = upgrade;
  },
  confirmRemove() {
    ApiV1.upgradeRemove.post({ id: this.selectUpgrade._id }, () => {
      handleSuccess({ message: '删除成功' });
      this.confirmShow = false;
      this.query();
    }, () => {
      handleError({ message: '删除失败' });
      this.confirmShow = false;
    });
  },
  edit() {
    this.$els.form.reset();
    this.editShow = true;
  },
  editConfirm() {
    if (!this.model.file) {
      handleError({ message: '请选择升级包' });
      return;
    }
    if (!this.model.file.match(/\.apk$/)) {
      handleError({ message: '请选择安卓类型文件' });
      return;
    }
    if (!this.model.model) {
      handleError({ message: '请填写model' });
      return;
    }
    if (!this.model.appVersion) {
      handleError({ message: '请填写appVersion' });
      return;
    }
    if (!this.model.appVersion.match(/^\d+\.\d+\.\d+$/)) {
      handleError({ message: 'appVersion格式需参照1.2.3' });
      return;
    }
    this.$els.form.submit();
    handleSuccess({ message: '保存成功' });
    const _this = this;
    setTimeout(function() {
      _this.query();
    }, 1000);
    setTimeout(function() {
      _this.query();
    }, 5000);
    setTimeout(function() {
      _this.query();
    }, 10000);
    this.editShow = false;
  }
};

exports.ready = function() {
  this.query();
};
</script>

<style lang="stylus">
#upgrade
  .select
    flex: 1
    .dropdown-toggle
      width: 100%
    .dropdown-menu
      width: 100%
      text-align: center

  .dropdown-toggle
    padding: 2px
    border-color: rgb(181, 180, 180)
    border-width: 1px

  .btn
    border-radius: 4px
    width: 60px
    padding-bottom: 2px
    padding-top: 2px

  .upgrade.selected
    background-color: #8bcab796

  .dropdown-menu
    max-height: 200px
    overflow-y: auto

</style>
