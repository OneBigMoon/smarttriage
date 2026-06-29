'use strict';

const Utils = require('../utils');
const crypto = require('crypto');
const Config = require('../config');

module.exports = (Schema) => {
  /**
   * User（用户）
   */
  const UserSchema = new Schema({
    username: String, // 用户名/工号
    salt: String, // salt
    hashedPassword: String, // hash密码
    auths: [], // 权限
    fullname: String, // 姓名
    profession: String, // 职称
    praises: Number, // 被点赞数
    lastlogintime: Date, // 最后登录时间
    photoname: String, // 照片名
    photouri: String, // 文件地址
    ct: Date, // 创建时间
    ut: Date // 更新时间
  });

  UserSchema
    .virtual('password')
    .set(function(password) {
      this._password = password;
      this.salt = this.makeSalt();
      this.hashedPassword = this.encryptPassword(password);
    })
    .get(function() {
      return this._password;
    });

  UserSchema
    .virtual('_info')
    .get(function() {
      return Utils.pickNotNull(this, [
        'id',
        'username',
        'auths',
        'fullname',
        'profession',
        'praises',
        'lastlogintime',
        'photoname',
        'photouri'
      ]);
    });

  UserSchema.methods = {
    authenticate(plainText) {
      return this.encryptAesPassword(plainText) === this.hashedPassword;
    },
    makeSalt: () => crypto.randomBytes(15).toString('base64'),
    encryptPassword(password) {
      if (!password || !this.salt) {
        return '';
      }
      const salt = new Buffer(this.salt, 'base64');
      return crypto.pbkdf2Sync(password, salt, 10000, 63).toString('base64');
    },
    encryptAesPassword(password) {
      let salt;
      if (!password || !this.salt) {
        return '';
      }

      let decrypted = '';
      const decipher = crypto.createDecipher('aes-256-cbc', Config.pwdkey);
      decrypted += decipher.update(password, 'hex', 'binary');
      decrypted += decipher.final('binary');
      salt = new Buffer(this.salt, 'base64');
      return crypto.pbkdf2Sync(decrypted, salt, 10000, 63, 'sha1').toString('base64');
      // return crypto.pbkdf2Sync(password, salt, 2, 63, 'sha256').toString('base64');
    }
  };

  return UserSchema;
};
