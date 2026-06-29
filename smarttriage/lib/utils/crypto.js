'use strict';
const fs = require('fs');

const crypto = require('crypto');

exports.base64ToUrlSafe = function(v) {
  return v.replace(/\//g, '_').replace(/\+/g, '-');
};

exports.urlsafeBase64Encode = function(jsonFlags) {
  const encoded = new Buffer(jsonFlags).toString('base64');
  return exports.base64ToUrlSafe(encoded);
};

exports.hmacSha1 = function(encodedFlags, secretKey) {
  const hmac = crypto.createHmac('sha1', secretKey);
  hmac.update(encodedFlags);
  return hmac.digest('base64');
};

exports.md5 = function(text) {
  const md5 = crypto.createHash('md5');
  md5.update(text, 'utf-8');
  return md5.digest('hex');
};

exports.readFileMd5 = async function(url) {
  return new Promise((reslove) => {
    const md5sum = crypto.createHash('md5');
    const stream = fs.createReadStream(url);
    stream.on('data', function(chunk) {
      md5sum.update(chunk);
    });
    stream.on('end', function() {
      const fileMd5 = md5sum.digest('hex');
      reslove(fileMd5);
    });
  });
};
