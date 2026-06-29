'use strict';

const soap = require('soap');
const parseString = require('xml2js').parseString;

module.exports = {
  async execFun(url, fun, args, pre, suf) {
    const client = await soap.createClientAsync(url);
    const result = await client[fun + 'Async'](args);
    const data = await new Promise(function(resolve, reject) {
      parseString((pre ? pre : '') + result[fun + 'Result'] + (suf ? suf : ''), function(err, parseData) {
        if (err) {
          reject(err);
        } else {
          resolve(parseData);
        }
      });
    });
    return data;
  }
};
  // var url = 'http://10.16.0.206:8891/Service.asmx?wsdl';
  // var args = {
  //   bqh000: '2262'
  //   ,
  //   types: '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15'
  //   // ,
  //   // Date: '2018-02-13'
  // };
  // co(webservice.execFun(url, 'QueryNursingDate', args)).then(function(r){
  //   console.info(JSON.stringify(r));
  // });
