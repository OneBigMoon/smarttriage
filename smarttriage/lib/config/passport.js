'use strict';

const Utils = require('../utils');
const passport = Utils.passport;
const PassportError = passport.PassportError;

passport.serializeUser((user) => Promise.resolve(JSON.stringify(user)));

passport.deserializeUser((sess) => Promise.resolve(Utils.parseJson(sess)));

// add local strategies for more authentication flexibility
passport.use('local', async (params) => {
  const User = params.userModel;
  const username = params.username;
  const password = params.password;
  const user = await User.findOne({
    username
  });
  if (!user || !user.authenticate(password)) {
    throw new PassportError('密码错误或用户不存在');
  }
  user.lastlogintime = new Date();
  await user.save();
  return user;
});

module.exports = passport;
