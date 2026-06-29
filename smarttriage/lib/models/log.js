'use strict';

module.exports = (Schema) => {
  /**
   * Log（日志）
   */
  const LogSchema = new Schema({
    no: String, // 终端号
    originname: String, // 原始文件名
    name: String, // 文件名
    path: String, // 路径
    ct: Date, // 创建时间
    ut: Date // 更新时间
  });

  return LogSchema;
};
