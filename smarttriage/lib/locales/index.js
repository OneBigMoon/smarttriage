'use strict';

const i18n = require('i18n');

module.exports = (config) => {
  i18n.configure({
    defaultLocale: config.lang,
    directory: __dirname
  });

  // i18n.setLocale('en');
};
