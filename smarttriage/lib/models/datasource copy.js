'use strict';
const Utils = require('../utils');

module.exports = (Schema) => {
  /**
   * Datasource（数据源）
   */
  const DatasourceSchema = new Schema({
    name: String, // 名称
    departmentid: [Number], // 科室id,一级his用,多个id用半角逗号分隔
    screenid: Number, // 屏id,二级his用
    screensplitid: [Number], // 屏id,二级his分屏用
    queue: [String], // 队列名,pacs用,多个队列用半角逗号分隔
    windowid: [Number], // 采样窗口编号,检验窗口用
    pharmacydeptno: Number, // 药房部门编号
    pharmacywinno: [Number], // 发药窗口编号
    type: String,
    user: {
      type: String,
      ref: 'User'
    }, // 用户
    morningcleartime: String,
    afternooncleartime: String,
    ct: Date, // 创建时间
    ut: Date // 更新时间
  });

  DatasourceSchema
    .virtual('_info')
    .get(function() {
      const info = Utils.pickNotNull(this, [
        '_id',
        'name',
        'screenid',
        'screensplitid',
        'pharmacydeptno',
        'pharmacywinno',
        'type',
        'morningcleartime',
        'afternooncleartime'
      ]);
      if (this.departmentid) {
        info.departmentid = this.departmentid.join(',');
      }
      if (this.screensplitid) {
        info.screensplitid = this.screensplitid.join(',');
      }
      if (this.queue) {
        info.queue = this.queue.join(',');
      }
      if (this.windowid) {
        info.windowid = this.windowid.join(',');
      }
      if (this.pharmacydeptno || this.pharmacywinno.length) {
        info.pharmacy = this.pharmacydeptno + '_' + this.pharmacywinno.join(',');
      }
      if (this.pharmacywinno) {
        info.pharmacywinno = this.pharmacywinno.join(',');
      }
      return info;
    });

  DatasourceSchema.index({ type: 1, departmentid: -1 });
  DatasourceSchema.index({ type: 1, screenid: -1 });
  DatasourceSchema.index({ type: 1, screensplitid: -1 });
  DatasourceSchema.index({ type: 1, queue: -1 });
  DatasourceSchema.index({ type: 1, windowid: -1 });
  DatasourceSchema.index({ type: 1, pharmacydeptno: -1 });

  return DatasourceSchema;
};
