'use strict';

module.exports = (Schema) => {
  /**
   * Upgrade（升级包）
   */
  const UpgradeSchema = new Schema({
    model: String, // model
    appVersion: String, // 版本号
    sortAppVersion: Number, // 版本号数值
    originname: String, // 原始文件名
    name: String, // 文件名
    path: String, // 路径
    md5: String, // md5
    ct: Date, // 创建时间
    ut: Date // 更新时间
  });

  return UpgradeSchema;
};
