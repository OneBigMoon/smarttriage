<template>
  <div id="org">
    <div class="row" style="height: 100%;">
      <div class="col-md-6" style="height: 100%;">
        <div class="panel panel-default" style="height: 100%;margin-bottom: 0;overflow-y: auto;">
          <div class="panel-body">
            <tree :data="treeData" :name="menuName" :is-edit="true"></tree>
          </div>
        </div>
      </div>
      <div class="col-md-6" style="height: 100%;">
        <div class="panel panel-default" style="height: 100%;margin-bottom: 0;">
          <div class="panel-body">
            <form name="form">
              <div class="form-group">
                <label for="name" class="control-label">
                  名称:
                </label>
                <input type="text" name="name" class="" v-model="model.name" required
                 :disabled="model._id==0||model._id==-1||(!model._id&&!model.parentid&&model.parentid!=0)">
                <button type="button" class="btn btn-primary" style="margin-left:10px;" @click="save"
                 :disabled="model._id==0||model._id==-1||(!model._id&&!model.parentid&&model.parentid!=0)">保存</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
    <dialog title="" :show.sync="confirmShow" :confirm="confirmRemove">
      <div class="text-center" slot="dialog-body">
        <i class="fa text-warning"></i>&emsp;确认删除？
      </div>
    </dialog>
  </div>
</template>

<script>
const Empty = require('../components/empty');
const _ = require('../../node_modules/lodash');
const Tree = require('../components/tree');
const Pagination = require('../components/pagination');
const Dialog = require('../components/dialog');
const ApiV1 = require('../services/api-v1');
const {
  handleError,
  handleSuccess
} = require('../utils');

exports.components = {
  Empty,
  Pagination,
  Tree,
  Dialog
};

exports.data = () => {
  return {
    treeData: [],
    menuName: 'menuName', // 显示菜单名称的属性
    model: {
      name: '',
      parentid: null
    },
    confirmShow: false,
    removeId: null
  };
};

exports.methods = {
  query() {
    ApiV1.orgnizations.post({}, (data) => {
      this.treeData = data.result;
    }, () => {
      handleError({ message: '查询失败' });
    });
  },
  save() {
    if (!this.model.name) {
      handleError({ message: '请填写名称' });
      return;
    }
    ApiV1.orgnizationSave.post(this.model, () => {
      handleSuccess({ message: '保存成功' });
      this.query();
    }, () => {
      handleError({ message: '保存失败' });
    });
  },
  confirmRemove() {
    ApiV1.orgnizationRemove.post({ id: this.removeId }, () => {
      handleSuccess({ message: '删除成功' });
      this.confirmShow = false;
      this.query();
    }, () => {
      handleError({ message: '删除失败' });
      this.confirmShow = false;
    });
  }
};

exports.watch = {
  start: 'query'
};

exports.events = {
  'node-edit'(item) {
    this.model = _.clone(item);
  },
  'node-add'(item) {
    this.model = { name: '', parentid: item._id };
  },
  'node-remove'(item) {
    this.removeId = item._id;
    this.confirmShow = true;
  }
};

exports.ready = function() {
  this.query();
};
</script>

<style lang="stylus">
#org
  .btn
    border-radius: 4px
    width: 60px
    padding-bottom: 2px
    padding-top: 2px

</style>
