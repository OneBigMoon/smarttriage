'use strict';

const {
  toArray,
  encodeHTML,
  leftpad
} = require('./');

function fillZero(str, len = 2) {
  return leftpad(str, len, '0');
}

exports.install = (Vue) => {
  // 动态使用给定 filter 进行过滤
  Vue.filter('filter', function(value, filterName) {
    if (filterName) {
      const args = toArray(arguments);
      args.splice(1, 1);
      return Vue.filter(filterName).apply(this, args);
    }
    return value == null ? '' : encodeHTML('' + value);
  });

  // 换行
  Vue.filter('br', (value = '', spliter = ';') => {
    return value.replace(new RegExp(spliter, 'g'), '<br>');
  });

  // 转换时间
  Vue.filter('date', (value) => {
    if (!value) { return ''; }
    const date = new Date(parseInt(value, 10));
    return `${date.getFullYear()}-${fillZero(date.getMonth() + 1)}-${fillZero(date.getDate())} \
${fillZero(date.getHours())}:${fillZero(date.getMinutes())}:${fillZero(date.getSeconds())}`;
  });

  Vue.filter('simpleDate', (value) => {
    if (!value) { return ''; }
    const date = new Date(value);
    return `${date.getFullYear()}-${fillZero(date.getMonth() + 1)}-${fillZero(date.getDate())}`;
  });

  // 限制长度
  Vue.filter('limitTo', (value = '', length = 12) => {
    value = '' + value;
    if (value.length > length) {
      value = value.substr(0, length) + '...';
    }
    return value;
  });

  Vue.filter('fillZero', (value = '', length = 2) => {
    value = '' + value;
    return fillZero(value, length);
  });

  Vue.filter('fWeek', (value = 0) => {
    const weekNames = [
      'Sun', 'Mon', 'Tues', 'Wed', 'Thur', 'Fri', 'Sat'
    ];
    return weekNames[value];
  });

  Vue.filter('fMonth', (value = 0) => {
    const monthNames = [
      'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
    ];
    return monthNames[value];
  });
};
