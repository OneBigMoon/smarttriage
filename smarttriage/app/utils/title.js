/**
 * Title Utils
 */
'use strict';

const Config = require('../config');
const Utils = require('./');

const SPLITER = ' - ';

const setAll = exports.setAll = function(...titles) {
  Utils.isArray(titles[0]) && (titles = titles[0]);
  titles.reverse();
  titles.push(Config.app.title);
  document.title = titles.join(' - ');
};

exports.setCategory = (title) => {
  setAll(title);
};

exports.set = (title) => {
  let category = document.title.split(SPLITER);
  category = category[category.length - 2];
  setAll(category, title);
};
