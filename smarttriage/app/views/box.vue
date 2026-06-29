<template>
  <div id="box">
    <div class="panel panel-default" style="height: 100%;margin-bottom: 0;overflow-y: auto;">
      <div class="panel-body" style="height:100%;">
        <div class="row" style="height:100%;">
          <div class="col-md-3" style="border-right: 1px solid #cec5c5;height:100%;overflow: auto;">
           <div class="btn-group" role="group" style="margin-bottom: 15px;">
            <button type="button" class="btn btn-primary" @click="editGroup"
             :disabled="!selectNodeId&&selectNodeId!=0">设置</button>
           </div>
           <div class="btn-group" role="group" style="margin-bottom: 15px;margin-left:10px;">
             <button type="button" class="btn btn-primary" @click="powerGroup('restart')"
              :disabled="!selectNodeId&&selectNodeId!=0">重启</button>
             <button type="button" class="btn btn-primary" @click="powerGroup('on')"
              :disabled="!selectNodeId&&selectNodeId!=0">开机</button>
             <button type="button" class="btn btn-primary" @click="powerGroup('off')"
              :disabled="!selectNodeId&&selectNodeId!=0">关机</button>
            </div>
            <tree :data="treeData" :name="treeName" :select-able="true" :select-node-id="selectNodeId"></tree>
          </div>
          <div class="col-md-9" style="height:100%;">
            <div class="row" style="padding-bottom:20px;">
              <div class="col-md-3">编号
                <input type="text" v-model="search.no" style="width: 70%;"/>
              </div>
              <div class="col-md-3">名称
                <input type="text" v-model="search.name" style="width: 70%;"/>
              </div>
              <div class="col-md-3">状态
                <v-select class="select" :value.sync="search.status" :options="statuses" clear-button
                 style="width:70%"></v-select>
              </div>
              <div class="col-md-3">
                <button type="button" class="btn btn-primary" @click="query">查询</button>
              </div>
            </div>
            <div class="row" style="padding-bottom:20px;">
              <div class="col-md-12">
                <div class="btn-group" role="group">
                  <button type="button" class="btn btn-primary" @click="move" :disabled="!selectBox._id"
                   style="width:110px;">移动至分组</button>
                  <button type="button" class="btn btn-primary" @click="edit"
                   :disabled="!selectBox._id">设置</button>
                  <button type="button" class="btn btn-primary" @click="confirmShow=true"
                   :disabled="!selectBox._id">删除</button>
                </div>
                <div class="btn-group" role="group" style="margin-left:10px;">
                  <button type="button" class="btn btn-primary" @click="power('restart')"
                   :disabled="!selectBox._id||selectBox.status!='正常'">重启</button>
                  <button type="button" class="btn btn-primary" @click="power('on')"
                   :disabled="!selectBox._id||selectBox.status!='关机'">开机</button>
                  <button type="button" class="btn btn-primary" @click="power('off')"
                   :disabled="!selectBox._id||selectBox.status!='正常'">关机</button>
                </div>
                <div class="btn-group" role="group" style="margin-left:10px;">
                  <button type="button" class="btn btn-primary btn-log" @click="uploadLog()"
                   :disabled="!selectBox._id||selectBox.status!='正常'">上传日志</button>
                  <button type="button" class="btn btn-primary btn-log" @click="downloadLog()"
                   :disabled="!selectBox._id">下载日志</button>
                </div>
                <div style="float: right;">
                  <span style="margin-top: 6px;display: block;" @click="handleTotalMsgClick()">{{totalMsg}}</span>
                </div>
              </div>
            </div>
            <div class="row" style="height: calc(100% - 110px);overflow-y: auto;">
              <div class="col-md-12">
                <table class="table table-hover table-bordered">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>编号</th>
                      <th>名称</th>
                      <th>所属分组</th>
                      <th>IP地址</th>
                      <th>MODEL</th>
                      <th>APP_VERSION</th>
                      <th>样式</th>
                      <th>数据源</th>
                      <th>类型</th>
                      <th>设备状态</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="box in boxes" :class="['box', selectBox._id == box._id ? 'selected' : '' ]"
                     @click="select(box)">
                      <th scope="row">{{$index + 1}}</th>
                      <td>{{box.no}}</td>
                      <td>{{box.name}}</td>
                      <td>{{box.org && box.org.name ? box.org.name : ''}}</td>
                      <td>{{box.ip}}</td>
                      <td>{{box.model}}</td>
                      <td>{{box.appversion}}</td>
                      <td>{{styleMap[box.style]}}</td>
                      <td>{{box.datasource&&box.datasource.name?box.datasource.name:''}}</td>
                      <td>{{rotationMap[box.rotation]}}</td>
                      <td :style="{color:box.status=='断开'?'red':null}">{{box.status}}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <dialog title="" :show.sync="moveShow" :confirm="moveConfirm" :width="700">
      <div style="height:450px;overflow:auto;" slot="dialog-body">
        <tree :data="treeData" :name="treeNameMove" :select-able="true" :select-node-id="selectNodeIdMove"></tree>
      </div>
    </dialog>
    <dialog title="" :show.sync="confirmShow" :confirm="confirmRemove">
      <div class="text-center" slot="dialog-body">
        <i class="fa text-warning"></i>&emsp;确认删除？
      </div>
    </dialog>
    <dialog title="" :show.sync="editShow" :confirm="editConfirm" :width="700">
      <div style="height:450px;" slot="dialog-body">
        <form class="form-horizontal">
          <div class="form-group">
            <label class="col-sm-2 control-label">名称</label>
            <div class="col-sm-8">
              <input type="text" class="form-control" v-model="model.name">
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">开机时间</label>
            <div class="col-sm-8">
              <time-picker type="text" class="form-control" :value.sync="model.powerontime"></time-picker>
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">关机时间</label>
            <div class="col-sm-8">
              <time-picker type="text" class="form-control" :value.sync="model.powerofftime"></time-picker>
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">音量</label>
            <div class="col-sm-8">
              <input type="number" min="0" max="9" class="form-control" v-model="model.volume">
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">屏幕类型</label>
            <div class="col-sm-8">
              <v-select class="select" :value.sync="model.rotation"
               :options="rotations" placeholder="请选择屏幕类型" clear-button
               style="width:70%"></v-select>
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">样式</label>
            <div class="col-sm-8">
              <v-select class="select" :value.sync="model.style" :options="styles" placeholder="请选择样式" clear-button
               style="width:70%;margin-top:4px;"></v-select>
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">数据源</label>
            <div class="col-sm-8">
              <v-select class="select" style="width:70%;margin-top:4px;"
                :value.sync="model.datasource"
                :search="true"
                :options="datasources"
                placeholder="请选择数据源">
              </v-select>
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">走马灯</label>
            <div class="col-sm-8">
              <input type="text" class="form-control" v-model="model.horselamp">
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">标题</label>
            <div class="col-sm-8">
              <input type="text" class="form-control" v-model="model.title">
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">窗口名</label>
            <div class="col-sm-8">
              <input type="text" class="form-control" v-model="model.winname">
            </div>
          </div>
        </form>
      </div>
    </dialog>
    <dialog title="" :show.sync="editGroupShow" :confirm="editGroupConfirm" :width="700">
      <div style="height:450px;" slot="dialog-body">
        <form class="form-horizontal">
          <div class="form-group">
            <label class="col-sm-2 control-label">开机时间</label>
            <div class="col-sm-8">
              <time-picker type="text" class="form-control" :value.sync="modelGroup.powerontime"></time-picker>
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">关机时间</label>
            <div class="col-sm-8">
              <time-picker type="text" class="form-control" :value.sync="modelGroup.powerofftime"></time-picker>
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">音量</label>
            <div class="col-sm-8">
              <input type="number" min="0" max="9" class="form-control" v-model="modelGroup.volume">
            </div>
          </div>
          <div class="form-group">
            <label class="col-sm-2 control-label">走马灯</label>
            <div class="col-sm-8">
              <input type="text" class="form-control" v-model="modelGroup.horselamp">
            </div>
          </div>
        </form>
      </div>
    </dialog>
    <dialog title="" :show.sync="powerShow" :confirm="powerConfirm">
      <div class="text-center" slot="dialog-body">
        <i class="fa text-warning"></i>&emsp;确认{{powerStr[powerCmd]}}？
      </div>
    </dialog>
    <dialog title="" :show.sync="powerGroupShow" :confirm="powerGroupConfirm">
      <div class="text-center" slot="dialog-body">
        <i class="fa text-warning"></i>&emsp;确认{{powerStr[powerCmd]}}？
      </div>
    </dialog>
  </div>
</template>

<script>
const _ = require('../../node_modules/lodash');
const Empty = require('../components/empty');
const Tree = require('../components/tree');
const VSelect = require('../components/v-select');
const Pagination = require('../components/pagination');
const Dialog = require('../components/dialog');
const TimePicker = require('../components/timepicker');
const ApiV1 = require('../services/api-v1');
const {
  handleError, handleSuccess, downloadFile
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
      no: '',
      name: '',
      org: 'all',
      status: 'all'
    },
    boxes: [],
    statuses: [
      { value: 'all', label: '全部' },
      { value: '正常', label: '正常' },
      { value: '关机', label: '关机' },
      { value: '断开', label: '断开' }
    ],
    rotations: [
      { value: 'auto', label: '自动' },
      { value: '0', label: '横屏' },
      { value: '180', label: '横屏反向' },
      { value: '270', label: '竖屏' },
      { value: '90', label: '竖屏反向' }
    ],
    rotationMap: {
      auto: '自动',
      0: '横屏',
      180: '横屏反向',
      270: '竖屏',
      90: '竖屏反向'
    },
    treeData: [],
    selectNodeId: null,
    treeName: 'orgs',
    selectBox: { _id: null },
    moveShow: false,
    selectNodeIdMove: null,
    treeNameMove: 'orgsMove',
    confirmShow: false,
    editShow: false,
    powerShow: false,
    powerCmd: '',
    powerStr: {
      restart: '重启',
      on: '开机',
      off: '关机'
    },
    dataClickWindowMs: 10000,
    totalMsg: '',
    powerGroupShow: false,
    totalClickTracker: {
      count: 0,
      startAt: 0
    },
    rowClickTracker: {
      id: null,
      count: 0,
      startAt: 0
    },
    isEnablingAllData: false,
    isTogglingBoxData: false,
    model: {
      id: null,
      name: null,
      style: null,
      powerontime: null,
      powerofftime: null,
      volume: null,
      datasource: null,
      rotation: null,
      horselamp: null,
      title: null,
      winname: null
    },
    styles: [],
    datasources: [],
    styleMap: {},
    editGroupShow: false,
    modelGroup: {
      id: null,
      powerontime: null,
      powerofftime: null,
      volume: null,
      horselamp: null
    }
  };
};

exports.computed = {
  hostUrl() {
    const { protocol, hostname } = window.location;
    const { port } = window.location;
    if (port != '') {
      return `${protocol}//${hostname}:${port}`;
    }
    return `${protocol}//${hostname}`;
  }
};

exports.methods = {
  query() {
    let total = 0;
    let normal = 0;
    let off = 0;
    let disc = 0;
    ApiV1.boxes.post(this.search, (data) => {
      this.boxes = data.result;
      this.boxes.forEach((box) => {
        total += 1;
        if (box.status == '正常') {
          normal += 1;
        } else if (box.status == '关机') {
          off += 1;
        } else if (box.status == '断开') {
          disc += 1;
        }
      }, this);
      this.selectBox = { _id: null };
      this.resetRowClickTracker();
      this.resetTotalClickTracker();
      this.totalMsg = '设备总数: ' + total + ' ' + '正常数: ' + normal + ' '
       + '关机: ' + off + ' ' + '断开: ' + disc;
    }, () => {
      handleError({ message: '查询失败' });
    });
  },
  resetTotalClickTracker() {
    this.totalClickTracker.count = 0;
    this.totalClickTracker.startAt = 0;
  },
  resetRowClickTracker() {
    this.rowClickTracker.id = null;
    this.rowClickTracker.count = 0;
    this.rowClickTracker.startAt = 0;
  },
  handleTotalMsgClick() {
    if (this.isEnablingAllData) {
      return;
    }
    const now = Date.now();
    if (!this.totalClickTracker.startAt || now - this.totalClickTracker.startAt > this.dataClickWindowMs) {
      this.totalClickTracker.count = 0;
      this.totalClickTracker.startAt = now;
    }
    this.totalClickTracker.count += 1;
    if (this.totalClickTracker.count >= 10) {
      this.resetTotalClickTracker();
      this.enableAllBoxData();
    }
  },
  enableAllBoxData() {
    this.isEnablingAllData = true;
    ApiV1.boxEnableAllData.post({}, () => {
      handleSuccess({ message: '设置成功，已开启全部终端业务数据推送' });
      this.isEnablingAllData = false;
      this.query();
    }, () => {
      this.isEnablingAllData = false;
      handleError({ message: '设置失败' });
    });
  },
  trackRowClick(box) {
    if (this.isTogglingBoxData) {
      return;
    }
    const now = Date.now();
    if (this.rowClickTracker.id !== box._id) {
      this.resetRowClickTracker();
      this.rowClickTracker.id = box._id;
      this.rowClickTracker.startAt = now;
    } else if (!this.rowClickTracker.startAt || now - this.rowClickTracker.startAt > this.dataClickWindowMs) {
      this.rowClickTracker.count = 0;
      this.rowClickTracker.startAt = now;
    }
    this.rowClickTracker.id = box._id;
    this.rowClickTracker.count += 1;
    if (this.rowClickTracker.count >= 10) {
      this.resetRowClickTracker();
      this.toggleBoxDataEnabled(box);
    }
  },
  toggleBoxDataEnabled(box) {
    this.isTogglingBoxData = true;
    ApiV1.boxToggleDataEnabled.post({ id: box._id }, (data) => {
      const enabled = data.result && Number(data.result.dataenabled) === 1;
      handleSuccess({
        message: enabled ? '设置成功，已开启该终端业务数据推送' : '设置成功，已关闭该终端业务数据推送'
      });
      this.isTogglingBoxData = false;
      this.query();
    }, () => {
      this.isTogglingBoxData = false;
      handleError({ message: '设置失败' });
    });
  },
  queryTree() {
    ApiV1.orgnizations.post({}, (data) => {
      this.treeData = data.result;
    }, () => {
      handleError({ message: '查询失败' });
    });
  },
  select(box) {
    this.selectBox = box;
    this.trackRowClick(box);
  },
  move() {
    this.moveShow = true;
  },
  moveConfirm() {
    if (!this.selectNodeIdMove && this.selectNodeIdMove != 0) {
      handleError({ message: '请选择欲移动至的分组' });
      return;
    }
    ApiV1.boxMove.post({ id: this.selectBox._id, orgId: this.selectNodeIdMove }, () => {
      this.query();
      this.moveShow = false;
    }, () => {
      handleError({ message: '移动失败' });
      this.moveShow = false;
    });
  },
  confirmRemove() {
    ApiV1.boxRemove.post({ id: this.selectBox._id }, () => {
      handleSuccess({ message: '删除成功' });
      this.confirmShow = false;
      this.query();
    }, () => {
      handleError({ message: '删除失败' });
      this.confirmShow = false;
    });
  },
  power(cmd) {
    this.powerCmd = cmd;
    this.powerShow = true;
  },
  powerConfirm() {
    ApiV1.boxPower.post({ id: this.selectBox._id, cmd: this.powerCmd }, () => {
      handleSuccess({ message: this.powerStr[this.powerCmd] + '成功' });
      this.powerShow = false;
      this.query();
    }, () => {
      handleError({ message: this.powerStr[this.powerCmd] + '失败' });
      this.powerShow = false;
    });
  },
  powerGroup(cmd) {
    this.powerCmd = cmd;
    this.powerGroupShow = true;
  },
  powerGroupConfirm() {
    ApiV1.boxGroupPower.post({ id: this.selectNodeId, cmd: this.powerCmd }, () => {
      handleSuccess({ message: this.powerStr[this.powerCmd] + '成功' });
      this.powerGroupShow = false;
      this.query();
    }, () => {
      handleError({ message: this.powerStr[this.powerCmd] + '失败' });
      this.powerShow = false;
    });
  },
  queryStyles() {
    ApiV1.styles.post({}, (data) => {
      data.result.forEach((style, index) => {
        this.styles.$set(index, { value: style.key, label: style.name });
        this.styleMap[style.key] = style.name;
      }, this);
      this.query();
    }, () => {
      handleError({ message: '查询失败' });
    });
  },
  queryDatasources() {
    ApiV1.datasources.post({}, (data) => {
      data.result.forEach((datasource, index) => {
        this.datasources.$set(index, { value: datasource._id, label: datasource.name });
      });
    }, () => {
      handleError({ message: '查询失败' });
    });
  },
  edit() {
    this.editShow = true;
    _.assign(this.model, {
      id: null,
      name: null,
      style: null,
      powerontime: null,
      powerofftime: null,
      volume: null,
      datasource: null,
      rotation: null,
      horselamp: null,
      title: null,
      winname: null
    }, _.pick(this.selectBox, ['name', 'style', 'powerontime', 'powerofftime', 'volume', 'horselamp', 'title', 'winname']),
     { datasource: this.selectBox.datasource ? this.selectBox.datasource._id : '', id: this.selectBox._id,
      style: this.selectBox.style ? this.selectBox.style : '',
       rotation: this.selectBox.rotation ? this.selectBox.rotation : '' });
  },
  editConfirm() {
    ApiV1.boxSave.post(this.model, () => {
      handleSuccess({ message: '保存成功' });
      this.editShow = false;
      this.query();
    }, () => {
      handleError({ message: '保存失败' });
    });
  },
  editGroup() {
    _.assign(this.modelGroup, {
      id: null,
      powerontime: null,
      powerofftime: null,
      volume: null,
      horselamp: null
    });
    this.modelGroup.id = this.selectNodeId;
    this.editGroupShow = true;
  },
  editGroupConfirm() {
    ApiV1.boxGroupSave.post(this.modelGroup, () => {
      handleSuccess({ message: '保存成功' });
      this.editGroupShow = false;
      this.query();
    }, () => {
      handleError({ message: '保存失败' });
    });
  },
  uploadLog() {
    ApiV1.uploadLog.post({ id: this.selectBox._id }, () => {
      handleSuccess({ message: '通知成功' });
    }, () => {
      handleError({ message: '通知失败' });
    });
  },
  downloadLog() {
    ApiV1.checkLog.post({ id: this.selectBox._id }, (res) => {
      if (res.errcode != 0) {
        handleError({ message: res.errmsg });
        console.info(`${this.hostUrl}/api/v1/boxes/download-log?id=${this.selectBox._id}`);
      } else {
        downloadFile(`${this.hostUrl}/api/v1/boxes/download-log?id=${this.selectBox._id}`);
      }
    }, () => {
      handleError({ message: '下载失败' });
    });
  }
};

exports.events = {
  'node-select'(id, thiz) {
    if (thiz.name == 'orgs') {
      this.selectNodeId = id;
      this.search.org = id;
      this.query();
    } else if (thiz.name == 'orgsMove') {
      this.selectNodeIdMove = id;
    }
  }
};

exports.watch = {
  'model.volume'(val) {
    if (val > 9) {
      this.model.volume = 9;
    }
  },
  'modelGroup.volume'(val) {
    if (val > 9) {
      this.model.volume = 9;
    }
  }
};

exports.ready = function() {
  this.queryTree();
  this.queryStyles();
  this.queryDatasources();
};
</script>

<style lang="stylus">
#box
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
    &-log
      width: 90px

  .box.selected
    background-color: #8bcab796

  .dropdown-menu
    max-height: 200px
    overflow-y: auto

</style>
