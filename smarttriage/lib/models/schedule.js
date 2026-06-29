'use strict';

module.exports = (Schema) => {
  /**
   * Schedule（定时任务）
   *
   * `_id`: 任务名称
   * `exec_date`: 最后执行时间
   */
  const ScheduleSchema = new Schema({
    _id: { type: String },
    exec_date: { type: Number, default: 0 }
  });

  return ScheduleSchema;
};
