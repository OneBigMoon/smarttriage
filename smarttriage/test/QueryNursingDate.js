var soap = require('soap');
var fs = require('fs');
var parseString = require('xml2js').parseString;
var co = require('co');


var url = 'http://10.16.0.206:8891/Service.asmx?wsdl';
var args = {
  bqh000: '2262',
  types: '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15'
  // ,
  // Date: '2018-02-13'
};
var soaptest = function() {
  console.info(new Date());
  soap.createClient(url, function(err, client) {
    client.QueryNursingDate(args, function(err, result) {
      co(function*(){
        const data = yield new Promise(function(resolve,reject){
          fs.writeFile('D://abc.js',result.QueryNursingDateResult)
          // parseString(result.QueryNursingDateResult, function (err, parseData) {
          //     if(err){
          //       reject(err);
          //     }else{
          //       console.info(JSON.stringify(parseData))
          //       resolve(parseData);
          //       console.info(new Date());
          //     }
          //
          // });
        });
        //console.info(JSON.stringify(data));
      }).catch(function(e){console.info(e)});



    });
  });
}
soaptest();
