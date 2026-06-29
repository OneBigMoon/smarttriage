'use strict';

const fs = require('fs');
const _ = require('lodash');

const Utils = require('../utils');

const models = _.filter(fs.readdirSync(__dirname), (file) => file != 'index.js')
  .map((model) => model.substr(0, model.lastIndexOf('.')));

const getModel = module.exports = (modelName) => {
  if (getModel.__models) {
    return getModel.__models[modelName];
  }
  if (_.isObject(modelName)) {
    const mongooseClient = modelName;
    getModel.__models = {};
    models.forEach((model) => {
      const name = Utils.upperCamelCase(model);
      getModel.__models[name] = mongooseClient.model(
        name,
        require(`./${model}`)(mongooseClient.Schema)
      );
    });
  }
};
