'use strict';

const mongoose = require('mongoose');

const Utils = require('../utils');
const logger = Utils.logger(__filename);

module.exports = (config) => {
  const client = mongoose.createConnection(config.uri, config.options);

  // 包装原始 Schema 类
  client.Schema = function() {
    client.base.Schema.apply(this, arguments);
  };
  client.Schema.prototype = new client.base.Schema();
  client.Schema.Types = client.base.Schema.Types;

  // 包装原始 model 方法
  client.__model = client.model;
  client.model = function(collectionName, schema) {
    collectionName += 's'; // 兼容scw数据库表名为复数
    if (!schema) {
      return client.__model.call(this, collectionName);
    }

    schema.options = schema.options || {};
    schema.options.collection = collectionName.toLowerCase();

    return client.__model.call(this, collectionName, schema);
  };

  client.promise = new Promise((resolve, reject) => {
    client.once('open', () => {
      logger.info(`MongoDB open on ${config.uri}`);
      resolve(client);
    });

    client.on('error', (err) => {
      logger.fatal('MongoDB error', err);
      reject(err);
    });

    client.on('connected', () => logger.info('MongoDB connected'));

    client.on('reconnected', () => logger.warn('MongoDB reconnected'));

    client.on('disconnected', () => {
      logger.fatal('MongoDB disconnected');

      // 断线后手动重连, 防止mongoose在一段时间重连失败后停止自动重连
      // https://github.com/Automattic/mongoose/issues/3615
      setTimeout(() => {
        client.open(config.uri);
      }, 5000);
    });
  });

  return client;
};
