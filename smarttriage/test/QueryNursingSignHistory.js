var soap = require('soap');
var fs = require('fs');
var parseString = require('xml2js').parseString;
var co = require('co');


var url = 'http://10.16.0.206:8891/Service.asmx?wsdl';
var args = {
  bqh000: '2258',
  date: '20180116',
  bednum: '08'
  // ,
  // Date: '2018-02-13'
};
var soaptest = function() {
  console.info(new Date());
  soap.createClient(url, function(err, client) {
    client.QueryNursingSignHistory(args, function(err, result) {
      co(function*(){
        const data = yield new Promise(function(resolve,reject){
          console.info(result)
          //fs.writeFile('D://abc.js',result.QueryNursingSignHistoryResult)
          parseString(result.QueryNursingSignHistoryResult, function (err, parseData) {
              if(err){
                reject(err);
              }else{
                console.info(JSON.stringify(parseData))
                fs.writeFile('D://abc.js',JSON.stringify(parseData))
                resolve(parseData);
                console.info(new Date());
              }

          });
        });
        //console.info(JSON.stringify(data));
      }).catch(function(e){console.info(e)});



    });
  });
}
soaptest();
