/**
 * HTTP Utils
 */
'use strict';

const Utils = require('./');
const {
  noop,
  isObject
} = Utils;

const methods = ['get', 'post', 'put', 'delete'];

const befores = [];
const afters = [];

function parseJson(json) {
  try {
    return JSON.parse(json);
  } catch (e) {
    return json;
  }
}

function ajax(method, url, data, options, onSuccess, onError) {
  const xhr = new (window.XMLHttpRequest || window.ActiveXObject)('Microsoft.XMLHTTP');
  xhr.onreadystatechange = () => {
    if (xhr.readyState == 4) {
      const last = xhr.status >= 200 && xhr.status < 400 ? onSuccess : onError;
      afters.concat([last, noop]).reduce((current, next) => {
        return current && current(parseJson(xhr.responseText), xhr, options) !== false ? next : false;
      });
    }
  };
  xhr.open(method, url, true);
  if (data) {
    if (/POST|PUT/.test(method)) {
      xhr.setRequestHeader('Content-type', 'application/json');
      data = JSON.stringify(data);
    } else {
      data = undefined;
    }
  }
  xhr.send(data);
}

function transformUrl(url, data) {
  const pattern = /\/:(\w+)/g;
  const encode = encodeURIComponent;
  if (isObject(data)) {
    return url.replace(pattern, ($0, $1) => data[$1] == null ? '' : '/' + encode(data[$1]));
  }
  return url.replace(pattern, () => data == null ? '' : '/' + encode(data));
}

const http = module.exports = (url, options = {}) => {
  const fns = {};
  methods.forEach((method) => {
    fns[method] = (data, onSuccess, onFailed) => {
      if (typeof data == 'function') {
        onFailed = onSuccess;
        onSuccess = data;
      }
      method = method.toUpperCase();
      const requestUrl = transformUrl(url, data);
      const last = () => ajax(method, requestUrl, data, options, onSuccess, onFailed);
      befores.concat([last, noop]).reduce((current, next) => {
        return current && current(requestUrl, data, method, options, onSuccess, onFailed) !== false ? next : false;
      });
    };
  });
  return fns;
};

http.before = (before) => {
  befores.push(before);
};

http.after = (after) => {
  afters.push(after);
};
