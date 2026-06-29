'use strict';
const fs = require('fs');

module.exports = {
  removeFile(path) {
    if (fs.existsSync(path)) {
      fs.unlink(path);
    }
  }
};
