'use strict';

module.exports = (Schema) => {
  /**
   * Style（样式）
   */
  const StyleSchema = new Schema({
    _id: Number,
    name: String, // 名称
    key: String // key
  });

  return StyleSchema;
};
