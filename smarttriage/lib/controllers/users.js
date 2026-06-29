'use strict';

const _ = require('lodash');
const Utils = require('../utils');
const logger = Utils.logger(__filename);
const passport = Utils.passport;
const fileUtil = Utils.fileUtil;
const Model = require('../models');
const User = Model('User');
const Config = require('../config');
const path = require('path');

module.exports = {
  async login(ctx) {
    try {
      if (!Utils.validateParams(ctx.request.body, ['username', 'password'])) {
        ctx.status = 401;
        ctx.body = {
          errcode: 10001,
          errmsg: 'invalid params'
        };
        return;
      }
      ctx.params.userModel = User;
      const user = await passport.login('local', ctx);
      ctx.body = {
        errcode: 0,
        user
      };
    } catch (e) {
      if (e instanceof passport.PassportError) {
        ctx.status = 401;
        ctx.body = {
          errcode: 10002,
          errmsg: e.message
        };
      } else {
        logger.error('login error', e);
        ctx.status = 500;
        ctx.body = {
          errcode: 10000,
          errmsg: 'server error'
        };
      }
    }
  },
  async logout(ctx) {
    await passport.logout(ctx);
    ctx.body = {
      errcode: 0,
      errmsg: 'ok'
    };
  },
  async create(ctx) {
    let file;
    try {
      const params = ctx.request.body.fields;
      const date = new Date();
      let photoname;
      let photouri;
      if (ctx.request.body.files && ctx.request.body.files.photo) {
        file = ctx.request.body.files.photo;
        photoname = file.name;
        photouri = '/' + file.path.split('\\')[1];
      }
      if (!Utils.validateParams(params, ['username', 'password', 'fullname', 'wardnos', 'profession'])) {
        ctx.status = 401;
        ctx.body = {
          errcode: 10001,
          errmsg: 'invalid params'
        };
        if (file) {
          fileUtil.removeFile(file.path);
        }
        return;
      }
      const repeatedUser = await User.findOne({
        username: params.username
      });
      if (repeatedUser) {
        ctx.status = 401;
        ctx.body = {
          errcode: 10001,
          errmsg: 'username repeated'
        };
        if (file) {
          fileUtil.removeFile(file.path);
        }
        return;
      }
      await new User({
        username: params.username,
        password: params.password,
        fullname: params.fullname,
        wards: params.wardnos.split(','),
        profession: params.profession,
        auths: ['AUTH_COMMON'],
        photoname,
        photouri,
        ct: date,
        ut: date
      }).save();
      ctx.body = {
        errcode: 0,
        errmsg: 'ok'
      };
    } catch (e) {
      logger.error('create error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
      if (file) {
        fileUtil.removeFile(file.path);
      }
    }
  },
  async delete(ctx) {
    try {
      const params = ctx.request.body;
      if (!Utils.validateParams(params, ['id'])) {
        ctx.status = 401;
        ctx.body = {
          errcode: 10001,
          errmsg: 'invalid params'
        };
        return;
      }
      const user = await User.findOne({
        _id: params.id
      });
      if (!user) {
        ctx.status = 401;
        ctx.body = {
          errcode: 10001,
          errmsg: 'user no exist'
        };
        return;
      }
      await user.remove();
      if (user.photouri) {
        fileUtil.removeFile(path.join(Config.dir.upload, user.photouri));
      }
      ctx.body = {
        errcode: 0,
        errmsg: 'ok'
      };
    } catch (e) {
      logger.error('delete error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  },
  async update(ctx) {
    let file;
    try {
      const params = ctx.request.body.fields;
      const date = new Date();
      let lastPhotouri;
      let photoname;
      let photouri;
      if (ctx.request.body.files && ctx.request.body.files.photo) {
        file = ctx.request.body.files.photo;
        photoname = file.name;
        photouri = '/' + file.path.split('\\')[1];
      }
      if (!Utils.validateParams(params, ['id'])) {
        ctx.status = 401;
        ctx.body = {
          errcode: 10001,
          errmsg: 'invalid params'
        };
        if (file) {
          fileUtil.removeFile(file.path);
        }
        return;
      }
      const user = await User.findOne({
        _id: params.id
      });
      if (!user) {
        ctx.status = 401;
        ctx.body = {
          errcode: 10001,
          errmsg: 'user no exist'
        };
        if (file) {
          fileUtil.removeFile(file.path);
        }
        return;
      }
      if (params.password) {
        user.password = params.password;
      }
      if (params.fullname) {
        user.fullname = params.fullname;
      }
      if (params.wardnos) {
        user.wards = params.wardnos.split(',');
      }
      if (params.profession) {
        user.profession = params.profession;
      }
      if (photoname) {
        user.photoname = photoname;
      }
      if (photouri) {
        lastPhotouri = user.photouri;
        user.photouri = photouri;
      }
      user.ut = date;
      await user.save();
      if (lastPhotouri) {
        fileUtil.removeFile(path.join(Config.dir.upload, lastPhotouri));
      }
      ctx.body = {
        errcode: 0,
        errmsg: 'ok'
      };
    } catch (e) {
      logger.error('update error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
      if (file) {
        fileUtil.removeFile(file.path);
      }
    }
  },
  async query(ctx) {
    try {
      const params = ctx.request.query;
      const filter = {};
      if (params.username) {
        filter.username = new RegExp(params.username, 'i');
      }
      if (params.wardno) {
        filter.wards = params.wardno;
      }
      const users = await User.find({
        $and: [{
          username: {
            $ne: 'root'
          }
        }, filter]
      }).populate('wards', '_id name');
      for (let i = 0; i < users.length; i++) {
        users[i] = users[i]._info;
      }
      ctx.body = {
        errcode: 0,
        result: users
      };
    } catch (e) {
      logger.error('update error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  },
  async queryForBedsidecard(ctx) {
    try {
      const params = ctx.request.query;
      if (!Utils.validateParams(params, ['wardno'])) {
        ctx.status = 400;
        ctx.body = {
          errcode: 10001,
          errmsg: 'invalid params'
        };
        return;
      }
      const users = await User.find({
        wards: params.wardno
      }).populate('wards', '_id name');
      _.forEach(users, function(user, idx) {
        users[idx] = user._info;
      });
      ctx.body = {
        errcode: 0,
        result: users
      };
    } catch (e) {
      logger.error('queryForBedsidecard error', e);
      ctx.status = 500;
      ctx.body = {
        errcode: 10000,
        errmsg: 'server error'
      };
    }
  }
};
