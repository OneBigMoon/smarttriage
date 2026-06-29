'use strict';

const fs = require('fs');
const gulp = require('gulp');

const buildConfigPath = './build';

fs.readdirSync(buildConfigPath).forEach((file) => {
  if (/^gulp\..*\.js$/.test(file)) {
    require(`${buildConfigPath}/${file}`);
  }
});

// 可用任务列表
gulp.task('default', ['serve']);
gulp.task('build', ['building']);
