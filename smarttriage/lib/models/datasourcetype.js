'use strict';

module.exports = (Schema) => {
  /**
   * Datasourcetype（数据源类型）
   */
  const DatasourcetypeSchema = new Schema({
    _id: Number,
    name: String, // 名称
    key: String // key
  });

  return DatasourcetypeSchema;
};
