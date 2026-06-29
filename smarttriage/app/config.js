'use strict';

module.exports = {
  app: {
    name: process.env.PKG_NAME,
    rb_cross_url: process.env.RAINBOW_CROSS_URL,
    pwdkey: process.env.PWD_KEY,
    title: '分诊导医'
  },
  cookie: {
    user: 'user',
    remember: 'remember'
  }
};
