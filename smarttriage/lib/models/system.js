'use strict';

const Utils = require('../utils');

module.exports = (Schema) => {
  /**
   * System（系统配置）
   */
  const SystemSchema = new Schema({
    url: String,
    primarytablename: String,
    secondarytablename: String,
    secondarycounttablename: String,
    drawbloodtablename: String,
    pharmacytablename: String,
    username: String,
    password: String,
    pacssecondarytablename: String,
    pacsusername: String,
    pacspassword: String
  });

  SystemSchema
    .virtual('_info')
    .get(function() {
      return Utils.pickNotNull(this, [
        'url',
        'primarytablename',
        'secondarytablename',
        'secondarycounttablename',
        'drawbloodtablename',
        'pharmacytablename',
        'username',
        'password',
        'pacssecondarytablename',
        'pacsusername',
        'pacspassword'
      ]);
    });

  return SystemSchema;
};
