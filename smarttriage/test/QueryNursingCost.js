var soap = require('soap');
var fs = require('fs');
var parseString = require('xml2js').parseString;
var co = require('co');


var url = 'http://10.16.0.206:8891/Service.asmx?wsdl';
var args = {
  bqh000: '2268',
  date: '20130719',
  bednum: '09'
  // ,
  // Date: '2018-02-13'
};
var soaptest = function() {
  console.info(new Date());
  soap.createClient(url, function(err, client) {
    client.QueryNursingCost(args, function(err, result) {
      co(function*(){
        const data = yield new Promise(function(resolve,reject){
          // console.info(result)
          // fs.writeFile('D://abc.js',result.QueryNursingCostResult)
          parseString(result.QueryNursingCostResult, function (err, parseData) {
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
