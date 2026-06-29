'use strict';

module.exports = (Schema) => {
  /**
   * Orgnization（分组）
   */
  const OrgnizationSchema = new Schema({
    _id: Number,
    name: String, // 名称
    parentid: Number, // 父节点id
    idpath: String, // idpath
    user: {
      type: String,
      ref: 'User'
    }, // 用户
    ct: Date, // 创建时间
    ut: Date // 更新时间
  });

  return OrgnizationSchema;
};
