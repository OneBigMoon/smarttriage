'use strict';

const _ = require('lodash');
const Utils = require('../utils');
const logger = Utils.logger(__filename);
const Model = require('../models');
const Datasource = Model('Datasource');

module.exports = {
  async query(ctx) {
    try {
      const params = ctx.request.body;
      const filter = {};
      if (params.name) {
        filter.name = new RegExp(params.name, 'i');
      }
      const datasources = await Datasource.find(filter).sort({ type: 1 });
      for (let i = 0; i < datasources.length; i++) {
        datasources[i] = datasources[i]._info;
      }
      ctx.body = {
        errcode: 0,
        result: datasources
      };
    } catch (e) {
      logger.error('error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  },
  async remove(ctx) {
    try {
      const params = ctx.request.body;
      await Datasource.remove({ _id: params.id });
      ctx.body = {
        errcode: 0
      };
    } catch (e) {
      logger.error('error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  },
  async save(ctx) {
    try {
      const params = ctx.request.body;
      console.log(params)
      if (!params.datasource) {
        params.datasource = null;
      }
      if (!params.style) {
        params.style = null;
      }
      let datasource;
      const departmentid = [];
      const screensplitid = [];
      const windowid = [];
      const pharmacywinno = [];
      if (params.departmentid) {
        const deptids = params.departmentid.split(',');
        deptids.forEach(function(deptid) {
          if (!isNaN(deptid)) {
            departmentid.push(Number(deptid));
          }
        });
        datasource = await Datasource.findOne({ type: 'primarytriage', departmentid });
        let datasource1 = await Datasource.findOne({ type: 'leveldepart', departmentid });
        if ((datasource && datasource._id != params.id) || (datasource1 && datasource1._id != params.id)) {
          logger.warn('该一级分诊屏ID相关数据源已存在');
          ctx.status = 500;
          ctx.body = {
            errcode: 10001,
            errmsg: '该一级分诊屏ID相关数据源已存在'
          };
          return;
        }
      } else if (params.screenid) {
        datasource = await Datasource.findOne({ type: 'secondarytriage', screenid: params.screenid });
        if (datasource && datasource._id != params.id) {
          logger.warn('该屏ID相关数据源已存在');
          ctx.status = 500;
          ctx.body = {
            errcode: 10001,
            errmsg: '该屏ID相关数据源已存在'
          };
          return;
        }
      } else if (params.screensplitid) {
        const scrsplids = params.screensplitid.split(',');
        scrsplids.forEach(function(scrid) {
          if (!isNaN(scrid)) {
            screensplitid.push(Number(scrid));
          }
        });
        datasource = await Datasource.findOne({ type: 'secondarytriagesplit', screensplitid });
        if (datasource && datasource._id != params.id) {
          logger.warn('该分屏ID相关数据源已存在');
          ctx.status = 500;
          ctx.body = {
            errcode: 10001,
            errmsg: '该分屏ID相关数据源已存在'
          };
          return;
        }
      } else if (params.queue) {
        datasource = await Datasource.findOne({ type: 'secondarytriageultrasonic', queue: params.queue.split(',') });
        if (datasource && datasource._id != params.id) {
          logger.warn('该队列名相关数据源已存在');
          ctx.status = 500;
          ctx.body = {
            errcode: 10001,
            errmsg: '该队列名相关数据源已存在'
          };
          return;
        }
      } else if (params.windowid) {
        const winids = params.windowid.split(',');
        winids.forEach(function(winid) {
          if (!isNaN(winid)) {
            windowid.push(Number(winid));
          }
        });
        datasource = await Datasource.findOne({ type: 'drawbloodtriage', windowid });
        if (datasource && datasource._id != params.id) {
          logger.warn('该窗口编号相关数据源已存在');
          ctx.status = 500;
          ctx.body = {
            errcode: 10001,
            errmsg: '该窗口编号相关数据源已存在'
          };
          return;
        }
      } else if (params.pharmacydeptno || params.pharmacywinno) {
        const pharwinnos = params.pharmacywinno.split(',');
        pharwinnos.forEach(function(winno) {
          if (!isNaN(winno)) {
            pharmacywinno.push(Number(winno));
          }
        });
        datasource = await Datasource.findOne({ type: params.type,
         pharmacydeptno: params.pharmacydeptno, pharmacywinno });
        if (datasource && datasource._id != params.id) {
          logger.warn('该部门编号下该窗口编号相关数据源已存在');
          ctx.status = 500;
          ctx.body = {
            errcode: 10001,
            errmsg: '该部门编号下该窗口编号相关数据源已存在'
          };
          return;
        }
      }
      if (!params.id) {
        datasource = new Datasource({ name: params.name,
          departmentid,
          screenid: params.screenid,
          screensplitid,
          windowid,
          pharmacydeptno: params.pharmacydeptno,
          pharmacywinno,
          type: params.type,
          morningcleartime: params.morningcleartime,
          afternooncleartime: params.afternooncleartime,
          ct: new Date(),
          ut: new Date() });
        if (params.queue) {
          datasource.queue = params.queue.split(',');
          if(params.consultingroomname){
            datasource.consultingroomname=params.consultingroomname.split(',');
            if(datasource.consultingroomname.length>datasource.queue.length){
              logger.warn('诊室名称数量大于队列数量');
              ctx.status = 500;
              ctx.body = {
                errcode: 10001,
                errmsg: '诊室名称数量大于队列数量'
              };
              return;
            }else if(params.consultingroomname.indexOf('，')>=0){

              logger.warn('诊室名称格式错误');
              ctx.status = 500;
              ctx.body = {
                errcode: 10001,
                errmsg: '诊室名称格式错误'
              };
              return;

            }
          }


        }
      } else {
        datasource = await Datasource.findOne({ _id: params.id });
        _.assign(datasource, _.pick(params, ['name', 'type', 'screenid', 'pharmacydeptno', 'morningcleartime', 'afternooncleartime'
      ]), { departmentid, screensplitid, windowid, pharmacywinno });
        if (params.queue) {
          datasource.queue = params.queue.split(',');
          if(params.consultingroomname){
            datasource.consultingroomname=params.consultingroomname.split(',');
            if(datasource.consultingroomname.length > datasource.queue.length){
              logger.warn('诊室名称数量大于队列数量');
              ctx.status = 500;
              ctx.body = {
                errcode: 10001,
                errmsg: '诊室名称数量大于队列数量'
              };
              return;

            }else if(params.consultingroomname.indexOf('，')>=0){

              logger.warn('诊室名称格式有错误');
              ctx.status = 500;
              ctx.body = {
                errcode: 10001,
                errmsg: '诊室名称格式错误'
              };
              return;

            }
          }else{
            datasource.consultingroomname=null;
          }
        }
      }
      await datasource.save();
      ctx.body = {
        errcode: 0
      };
    } catch (e) {
      logger.error('error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  }
};
