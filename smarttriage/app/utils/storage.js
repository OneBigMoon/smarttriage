'use strict';

const win = window;
const Utils = require('./');

const storage = {
  setLocalCookie(k, v, t) {
    typeof window.localStorage !== 'undefined' ? localStorage.setItem(k, v) :
      (() => {
        t = t || 365 * 12 * 69 * 60;
        Utils.cookie.set(k, v, t);
      })();
  },
  getLocalCookie(k) {
    k = k || 'localDataTemp';
    return typeof window.localStorage !== 'undefined' ? localStorage.getItem(k) :
      (() => {
        Utils.cookie.get(k);
      })();
  },
  clearLocalData(k) {
    k = k || 'localDataTemp';
    return typeof window.localStorage !== 'undefined' ? localStorage.removeItem(k) :
      (() => {
        Utils.cookie.set(k, '', 0);
      })();
  },
  init() {
    this.bindEvent();
  },
  bindEvent() {
    win.addEventListener('message', (evt) => {
      if (win.parent != evt.source) {
        return;
      }

      const options = JSON.parse(evt.data);
      if (options.type == 'GET') {
        const data = storage.getLocalCookie(options.key);
        win.parent.postMessage(data, '*');
      }

      options.type == 'SET' && storage.setLocalCookie(options.key, options.value);
      options.type == 'REM' && storage.clearLocalData(options.key);
    }, false);
  },
  send(data) {
    win.parent.postMessage(data, '*');
  }
};

module.exports = storage;
