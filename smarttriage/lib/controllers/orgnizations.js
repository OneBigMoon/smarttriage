'use strict';

const Utils = require('../utils');
const logger = Utils.logger(__filename);
const Model = require('../models');
const Orgnization = Model('Orgnization');
const _ = require('lodash');

module.exports = {
  async query(ctx) {
    try {
      const orgs = await Orgnization.find({}).sort({ _id: 1 });
      const orgMap = {};
      orgs.forEach(function(org) {
        orgMap[org._id] = _.pick(org, ['_id', 'name', 'parentid', 'idpath']);
        if (org._id != 0 && org._id != -1) {
          if (!orgMap[org.parentid].children) {
            orgMap[org.parentid].children = [];
          }
          orgMap[org.parentid].children.push(orgMap[org._id]);
        }
      });
      ctx.body = {
        errcode: 0,
        result: [orgMap[-1], orgMap[0]]
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
      if (params._id) {
        await Orgnization.update({ _id: params._id }, { name: params.name, user: ctx.state.user.id, ut: new Date() });
      } else if (params.parentid || params.parentid == 0) {
        const parent = await Orgnization.findOne({ _id: params.parentid });
        const lastOrgs = await Orgnization.find({}).sort({ _id: -1 }).limit(1);
        const org = new Orgnization({
          _id: lastOrgs[0]._id + 1,
          name: params.name,
          parentid: params.parentid,
          idpath: parent.idpath + (lastOrgs[0]._id + 1) + '.',
          user: ctx.state.user.id,
          ct: new Date(),
          ut: new Date()
        });
        await org.save();
      }
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
  async remove(ctx) {
    try {
      const params = ctx.request.body;
      const org = await Orgnization.findOne({ _id: params.id });
      const idpath = org.idpath.replace(/\./g, '\\.');
      await Orgnization.remove({ idpath: new RegExp('^' + idpath) });
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
