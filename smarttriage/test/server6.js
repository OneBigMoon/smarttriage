const os  = require('os');
const moment = require('moment');
const path = require('path');

console.info(moment().startOf('day').toDate())
console.info(os.tmpDir())
console.info(path.join('aaa','bbbb'))
