<template>
  <div id="data">
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
                  <button type="button" class="btn btn-primary" @click="edit"
                   :disabled="!selectDatasource._id">编辑</button>
                  <button type="button" class="btn btn-primary" @click="confirmShow=true"
                   :disabled="!selectDatasource._id">删除</button>
                 </div>
              </div>
            </div>
            <div class="row" style="height: calc(100% - 110px);overflow-y: auto;">
              <div class="col-md-12">
                <table class="table table-hover table-bordered">
                  <thead>
                    <tr>
                      <th style="width:50px;">#</th>
                      <th style="width:10%;">名称</th>
                      <th style="width:10%;">类型</th>
                      <!-- <th style="width:10%;">科室ID</th> -->
                      <th style="width:10%;">屏ID</th>
                      <th style="width:10%;">分屏ID</th>
                      <th style="width:10%;">队列名</th>
                      <th style="width:10%;">诊室名称</th>
                      <th style="width:10%;">检验窗口</th>
                      <th>药房</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="datasource in datasources"
                     :class="['datasource', selectDatasource._id == datasource._id ? 'selected' : '' ]"
                     @click="select(datasource)">
                      <th scope="row">{{$index + 1}}</th>
                      <td>{{datasource.name}}</td>
                      <td>{{datasourcetypeMap[datasource.type]}}</td>
                      <td v-if="(datasource.type=='primarytriage' || datasource.type=='leveldepart')">{{datasource.departmentid}}</td>
                      <td v-else>{{datasource.screenid}}</td>
                      <td>{{datasource.screensplitid}}</td>
                      <td>{{datasource.queue}}</td>
                      <td>{{datasource.consultingroomname}}</td>
                      <td>{{datasource.windowid}}</td>
                      <td>{{datasource.pharmacy}}</td>
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
    <div style="height:450px;width:650px;overflow-y:auto;" slot="dialog-body">
      <form class="form-horizontal" style="padding-right:10px;">
        <div class="form-group">
          <label class="col-sm-2 control-label">类型</label>
          <div class="col-sm-8">
            <v-select class="select" :value.sync="model.type"
             :options="datasourcetypes" placeholder="请选择类型" clear-button
             style="width:70%"></v-select>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">名称</label>
          <div class="col-sm-8">
            <input type="text" class="form-control" v-model="model.name">
          </div>
        </div>

          <!-- 一级分诊屏id -->
        <div  v-if="model.type=='primarytriage' || model.type=='leveldepart'" class="form-group">
          <label class="col-sm-2 control-label">屏ID</label>
          <div class="col-sm-8">
            <input type="text" class="form-control" v-model="model.departmentid" :disabled="model.type!='primarytriage' && model.type!='leveldepart'">
            <span>多个ID以半角逗号分隔</span>
          </div>
        </div>


        <div v-if="model.type!='primarytriage' && model.type!='leveldepart'" class="form-group">
          <label class="col-sm-2 control-label">屏ID</label>
          <div class="col-sm-8">
            <input type="text" class="form-control" v-model="model.screenid" :disabled="model.type!='secondarytriage'">
          </div>
        </div>

        <div class="form-group">
          <label class="col-sm-2 control-label">分屏ID</label>
          <div class="col-sm-8">
            <input type="text" class="form-control" v-model="model.screensplitid" :disabled="model.type!='secondarytriagesplit'">
            <span>多个ID以半角逗号分隔</span>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">队列名</label>
          <div class="col-sm-8">
            <input type="text" class="form-control" v-model="model.queue" :disabled="model.type!='secondarytriageultrasonic'">
            <span>多个队列以半角逗号分隔</span>
          </div>
        </div>
         <div class="form-group">
          <label class="col-sm-2 control-label">诊室名称</label>
          <div class="col-sm-8">
            <input type="text" class="form-control" v-model="model.consultingroomname" :disabled="model.type!='secondarytriageultrasonic'">
            <span>多个诊室名称以半角逗号分隔,并且顺序与队列名保持一致</span>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">检验窗口编号</label>
          <div class="col-sm-8">
            <input type="text" class="form-control" v-model="model.windowid" :disabled="model.type!='drawbloodtriage'">
            <span>多个编号以半角逗号分隔</span>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">药房部门编号</label>
          <div class="col-sm-8">
            <input type="text" class="form-control" v-model="model.pharmacydeptno"
             :disabled="model.type!='primarypharmacytriage'&&model.type!='secondarypharmacytriage'">
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">药房窗口编号</label>
          <div class="col-sm-8">
            <input type="text" class="form-control" v-model="model.pharmacywinno"
             :disabled="model.type!='primarypharmacytriage'&&model.type!='secondarypharmacytriage'">
            <span>多个编号以半角逗号分隔(药房一级)</span>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">上午下班时间</label>
          <div class="col-sm-8">
            <time-picker type="text" class="form-control" :value.sync="model.morningcleartime"><time-picker>
          </div>
        </div>
        <div class="form-group">
          <label class="col-sm-2 control-label">下午下班时间</label>
          <div class="col-sm-8">
            <time-picker type="text" class="form-control" :value.sync="model.afternooncleartime"><time-picker>
          </div>
        </div>
      </form>
    </div>
  </dialog>
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
    datasources: [],
    selectDatasource: { _id: null },
    confirmShow: false,
    editShow: false,
    model: {
      id: null,
      departmentid: null,
      screenid: null,
      screensplitid: null,
      queue: null,
      consultingroomname:null,
      windowid: null,
      pharmacydeptno: null,
      pharmacywinno: null,
      type: null,
      name: null,
      morningcleartime: null,
      afternooncleartime: null
    },
    datasourcetypes: [],
    datasourcetypeMap: {}
  };
};

exports.methods = {
  query() {
    ApiV1.datasources.post(this.search, (data) => {
      this.datasources = data.result;
      this.selectDatasource = { _id: null };
    }, () => {
      handleError({ message: '查询失败' });
    });
  },
  select(datasource) {
    this.selectDatasource = datasource;
  },
  queryDatasourceType() {
    ApiV1.datasourcetypes.post({}, (data) => {
      data.result.forEach((datasourcetype, index) => {
        this.datasourcetypes.$set(index, { value: datasourcetype.key, label: datasourcetype.name });
        this.datasourcetypeMap[datasourcetype.key] = datasourcetype.name;
      });
      this.query();
    }, () => {
      handleError({ message: '查询失败' });
    });
  },
  confirmRemove() {
    ApiV1.datasourceRemove.post({ id: this.selectDatasource._id }, () => {
      handleSuccess({ message: '删除成功' });
      this.confirmShow = false;
      this.query();
    }, () => {
      handleError({ message: '删除失败' });
      this.confirmShow = false;
    });
  },
  edit(type) {
    if (type == 'new') {
      this.model.id = null;
      this.model.name = null;
      this.model.departmentid = null;
      this.model.screenid = null;
      this.model.screensplitid = null;
      this.model.queue = null;
      this.model.consultingroomname=null;
      this.model.windowid = null;
      this.model.pharmacydeptno = null;
      this.model.pharmacywinno = null;
      this.model.type = '';
      this.model.morningcleartime = null;
      this.model.afternooncleartime = null;
    } else {
      this.model.id = this.selectDatasource._id;
      this.model.name = this.selectDatasource.name;
      this.model.departmentid = this.selectDatasource.departmentid;
      this.model.screenid = this.selectDatasource.screenid;
      this.model.screensplitid = this.selectDatasource.screensplitid;
      this.model.queue = this.selectDatasource.queue;
      this.model.consultingroomname=this.selectDatasource.consultingroomname;
      this.model.windowid = this.selectDatasource.windowid;
      this.model.pharmacydeptno = this.selectDatasource.pharmacydeptno;
      this.model.pharmacywinno = this.selectDatasource.pharmacywinno;
      this.model.type = this.selectDatasource.type || '';
      this.model.morningcleartime = this.selectDatasource.morningcleartime || null;
      this.model.afternooncleartime = this.selectDatasource.afternooncleartime || null;
    }
    this.editShow = true;
  },
  editConfirm() {
    if (!this.model.name) {
      handleError({ message: '请填写名称' });
      return;
    }
    if (!this.model.type) {
      handleError({ message: '请选择类型' });
      return;
    }
    if ((this.model.type == 'primarytriage' || this.model.type == 'leveldepart') && !this.model.departmentid) {
      handleError({ message: '请填写一级分诊屏ID' });
      return;
    }
    if (this.model.type == 'secondarytriage' && !this.model.screenid) {
      handleError({ message: '请填写屏ID' });
      return;
    }
    if (this.model.type == 'secondarytriagesplit' && !this.model.screensplitid) {
      handleError({ message: '请填写分屏ID' });
      return;
    }
    if (this.model.type == 'secondarytriageultrasonic' && !this.model.queue) {
      handleError({ message: '队列名' });
      return;
    }


   if (this.model.type == 'secondarytriageultrasonic' && this.model.queue) {

     var reg =/^([\u0391-\uFFE5\d\w,])*([\u0391-\uFFE5\d\w]+)$/;

      if(!reg.exec(this.model.queue)){
       handleError({ message: '队列名格式错误' });
      return;
      }

    }


    if (this.model.type == 'secondarytriageultrasonic' && this.model.consultingroomname) {

     var reg =/^([\u0391-\uFFE5\d\w,])*([\u0391-\uFFE5\d\w]+)$/;

      if(!reg.exec(this.model.consultingroomname)){
       handleError({ message: '诊室名称格式错误' });
      return;
      }

    }



 if ((this.model.type == 'primarytriage' || this.model.type == 'leveldepart') && this.model.departmentid) {

     var reg =/^\d+(,\d+)*$/;
      if(!reg.exec(this.model.departmentid)){
       handleError({ message: '屏ID格式错误' });
        return;
      }

    }


    if (this.model.type == 'drawbloodtriage' && !this.model.windowid) {
      handleError({ message: '请填写检验窗口编号' });
      return;
    }
    if ((this.model.type == 'primarypharmacytriage' || this.model.type == 'secondarypharmacytriage') && !this.model.pharmacydeptno) {
      handleError({ message: '请填写药房部门编号' });
      return;
    }
    if ((this.model.type == 'primarypharmacytriage' || this.model.type == 'secondarypharmacytriage') && !this.model.pharmacywinno) {
      handleError({ message: '请填写药房窗口编号' });
      return;
    }
    ApiV1.datasourceSave.post(this.model, () => {
      handleSuccess({ message: '保存成功' });
      this.editShow = false;
      this.query();
    }, (err) => {
      if (err.errcode != 10000) {
        handleError({ message: err.errmsg });
      } else {
        handleError({ message: '保存失败' });
      }
    });
  }
};

exports.watch = {
  'model.type'(val) {
    const keys = ['departmentid', 'screenid', 'screensplitid', 'queue', 'windowid',
     'pharmacydeptno', 'pharmacywinno'];
    let key;
    if (val == 'primarytriage' || val == 'leveldepart') {
      key = 'departmentid';
    } else if (val == 'secondarytriage') {
      key = 'screenid';
    } else if (val == 'secondarytriagesplit') {
      key = 'screensplitid';
    } else if (val == 'secondarytriageultrasonic') {
      key = 'queue';
    } else if (val == 'drawbloodtriage') {
      key = 'windowid';
    } else if (val == 'primarypharmacytriage' || val == 'secondarypharmacytriage') {
      key = ['pharmacydeptno', 'pharmacywinno'];
    }
    keys.forEach((k) => {
      if (Array.isArray(key)) {
        if (key.indexOf(k) < 0) {
          this.model[k] = null;
        }
      } else if (k != key) {
        this.model[k] = null;
      }
    });
  }
};

exports.ready = function() {
  this.queryDatasourceType();
};
</script>

<style lang="stylus">
#data
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

  .datasource.selected
    background-color: #8bcab796

  .dropdown-menu
    max-height: 200px
    overflow-y: auto

</style>
