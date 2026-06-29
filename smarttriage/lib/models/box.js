'use strict';

module.exports = (Schema) => {
  /**
   * Box（终端）
   */
  const BoxSchema = new Schema({
    no: String, // 编号
    name: String, // 名称
    org: {
      type: Number,
      ref: 'Orgnization'
    }, // 所属分组
    ip: String, // ip地址
    mac: String, // mac
    model: String, // model
    appversion: String, // 版本
    style: String, // 样式
    datasource: {
      type: String,
      ref: 'Datasource'
    }, // 数据源
    status: String, // 状态 正常，关机，断开
    user: {
      type: String,
      ref: 'User'
    }, // 用户
    powerontime: String,
    powerofftime: String,
    volume: Number,
    horselamp: String,
    title: String,
    winname: String, // 窗口名，用于展示窗口号引导病患
    rotation: String, // 自动:"auto",横屏:"0",横屏反向:"180",竖屏:"270",竖屏反向:"90"
    dataenabled: {
      type: Number,
      default: 0
    }, // 业务数据推送开关 0:关 1:开
    ht: Date, // heartbeat time
    ct: Date, // 创建时间
    ut: Date // 更新时间
  });

  BoxSchema.index({ no: -1 });

  return BoxSchema;
};
