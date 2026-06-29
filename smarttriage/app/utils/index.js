'use strict';

const doc = document;

const Vue = require('vue');
const util = Vue.util;

const Config = require('../config');

const COOKIE_PREFIX = Config.app.name + '.';
const COOKIE_SPITER = '; ';
const COOKIE_DECODE = decodeURIComponent;
const COOKIE_ENCODE = encodeURIComponent;
const CHAR_EQUAL = '=';

const {
  extend,
  toArray,
  isArray
} = util;

const utils = {
  noop() {},
  getRBCrossUrl() {
    return Config.app.rb_cross_url;
  },
  clone(collection) {
    return isArray(collection) ? collection.slice(0) : extend({}, collection);
  },
  each(collection, handler) {
    return isArray(collection) ? collection.forEach(handler) :
        Object.keys(collection).forEach((key) => handler(collection[key], key));
  },
  defaults(object, source) {
    utils.each(object, (defaultValue, prop) => {
      if (source[prop] == null) {
        source[prop] = defaultValue;
      }
    });
  },
  pick(object, attrs) {
    const result = {};
    if (!isArray(attrs)) {
      attrs = toArray(arguments).slice(1);
    }
    utils.each(attrs, (attr) => result[attr] = object[attr]);
    return result;
  },
  drop(array) {
    array = array.slice(0);
    for (const index of toArray(arguments).slice(1)) {
      array.splice(index, 1, null);
    }
    return array.filter((elem) => elem != null);
  },
  isFunction(fn) {
    return fn && typeof fn == 'function';
  },
  // isObjectEmpty(obj) {
  //  if (obj) {
  //    for (const prop in obj) {
  //      return false;
  //    }
  //  }
  //  return true;
  // },
  leftpad(str, len, ch = ' ') {
    let i = -1;

    str = '' + str;
    len = len - str.length;

    while (++i < len) {
      str = ch + str;
    }

    return str;
  },
  encodeHTML(str) {
    const div = doc.createElement('div');
    div.appendChild(doc.createTextNode(str));
    return div.innerHTML;
  },
  closest(elem, selector) {
    const matchesSelector = elem.matches ||
        elem.webkitMatchesSelector ||
        elem.mozMatchesSelector ||
        elem.msMatchesSelector;
    while (elem) {
      if (matchesSelector.call(elem, selector)) {
        return elem;
      }
      elem = elem.parentElement;
    }
    return null;
  },
  fireEvent(node, eventName) {
    // Make sure we use the ownerDocument from the provided node to avoid cross-window problems
    let currentDocument;
    if (node.ownerDocument) {
      currentDocument = node.ownerDocument;
    } else if (node.nodeType == 9) {
      // the node may be the document itself, nodeType 9 = DOCUMENT_NODE
      currentDocument = node;
    } else {
      throw new Error(`Invalid node passed to fireEvent: ${node.id}`);
    }

    if (node.dispatchEvent) {
      // Gecko-style approach (now the standard) takes more work
      let eventClass = '';

      // Different events have different event classes.
      // If this switch statement can't map an eventName to an eventClass,
      // the event firing is going to fail.
      switch (eventName) {
        case 'click':
        case 'mousedown':
        case 'mouseup':
          eventClass = 'MouseEvents';
          break;

        case 'focus':
        case 'change':
        case 'blur':
        case 'select':
        case 'submit':
          eventClass = 'HTMLEvents';
          break;

        default:
          throw new Error(`fireEvent: Couldn't find an event class for event '${eventName}'.`);
      }
      const event = currentDocument.createEvent(eventClass);

      const bubbles = eventName == 'change' ? false : true;
      event.initEvent(eventName, bubbles, true); // All events created as bubbling and cancelable.

      event.synthetic = true; // allow detection of synthetic events
      // The second parameter says go ahead with the default action
      node.dispatchEvent(event, true);
    } else if (node.fireEvent) {
      // IE-old school style
      const event = currentDocument.createEventObject();
      event.synthetic = true; // allow detection of synthetic events
      node.fireEvent('on' + eventName, event);
    }
  },
  cookie: {
    get(key) {
      return utils.cookies[key];
    },
    set(key, value, expires) {
      doc.cookie = COOKIE_ENCODE(COOKIE_PREFIX + key) +
          CHAR_EQUAL +
          (value != null ? COOKIE_ENCODE('' + value) : '') +
          '; path=/' +
          (expires ? '; expires=' + (new Date(Date.now() + (expires ? expires : -1))).toGMTString() : '');
    }
  },
  loading(show) {
    if (utils.$global) {
      let loading = utils.$global.loading;
      loading += show ? 1 : -1;
      if (loading < 0) {
        loading = 0;
      }
      utils.$global.loading = loading;
    }
  },
  alert(msg, type = 'success', duration = 3000, callback = utils.noop) {
    extend(utils.$global.alert, {
      show: true,
      msg,
      type,
      duration,
      callback
    });
  },
  handleError(err) {
    utils.alert(err && err.message || '系统错误', 'danger');
  },
  handleSuccess(success) {
    utils.alert(success && success.message);
  },
  setQueryCookie(key, filterKey = '', filterValue = '', start = 0) {
    utils.cookie.set(key, JSON.stringify({
      filterKey,
      filterValue,
      start
    }));
  },
  getQueryCookie(key, defaultKey = '', defaultValue = '', defaultStart = 0) {
    let cookie;
    try {
      cookie = JSON.parse(utils.cookie.get(key));
    } catch (e) {
      cookie = {};
    }
    const filterKey = cookie.filterKey || defaultKey;
    const filterValue = cookie.filterValue || defaultValue;
    const sort = cookie.sort || {};
    const cStart = parseInt(cookie.start || 0, 10);
    const start = isNaN(cStart) ? defaultStart : cStart;

    utils.setQueryCookie(key);

    return {
      start,
      filterKey,
      filterValue,
      sort
    };
  },
  genSelectStructure(structures, newStructures) {
    if (structures && structures.length) {
      utils.each(structures, (structure) => {
        const newChildren = [];
        utils.genSelectStructure(structure.children, newChildren);
        newStructures.push(utils.extend({
          children: newChildren,
          label: structure.name,
          value: structure._id
        }, utils.pick(structure, ['idpath', 'deep', 'type'])));
      });
    }
  },
  getComputedSize(el, attr) {
    return parseFloat(getComputedStyle(el)[attr]);
  },
  getScreenWidth() {
    return window.innerWidth || doc.documentElement.clientWidth || doc.body.clientWidth;
  },
  /**
   * 获取间隔分钟数
   */
  getMinuteDiff(fromMs, toMs) {
    return Math.floor(((toMs ? toMs : new Date().getTime()) - fromMs) / 60000);
  },
  /**
   * 根据身份证获得周岁
   */
  getAgeFromID(idCardNo) {
    if (!idCardNo) {
      return null;
    }
    const m = idCardNo.match(/^(?:(?:\d{6}(\d{4})(\d{2})(\d{2})\d{2}(\d)(?:X|x|\d))|\d{6}(\d{2})(\d{2})(\d{2})\d{2}(\d))$/);
    if (!m || m.length != 9) {
      return null;
    }
    let year;
    let month;
    let day;
    if (m[1]) {
      year = Number(m[1]);
      month = Number(m[2]);
      day = Number(m[3]);
    } else {
      year = '19' + m[5];
      month = m[6];
      day = m[7];
    }
    const date = new Date();
    return date.getFullYear() - year - (month * 100 + day < (date.getMonth() + 1) * 100 + date.getDate() ? 0 : 1);
  },
  downloadFile(url) {
    const downloadFileWindow = document.createElement('iframe');
    downloadFileWindow.style.display = 'none';
    downloadFileWindow.src = url;
    document.body.appendChild(downloadFileWindow);
  }
};

Object.defineProperty(utils, 'cookies', {
  get() {
    const _cookies = {};
    const documentCookie = doc.cookie;
    if (documentCookie) {
      documentCookie.split(COOKIE_SPITER).map(function(cookie) {
        cookie = cookie.split(CHAR_EQUAL);
        _cookies[COOKIE_DECODE(cookie[0]).substr(COOKIE_PREFIX.length)] = COOKIE_DECODE(cookie[1]);
      });
    }
    return _cookies;
  }
});

module.exports = extend(utils, util);
