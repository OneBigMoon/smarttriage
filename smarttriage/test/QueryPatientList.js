var soap = require('soap');
var fs = require('fs');
var parseString = require('xml2js').parseString;
var co = require('co');


var url = 'http://10.16.0.206:8891/Service.asmx?wsdl';
var args = {
  bqh000: '2262'
};
var soaptest = function() {
  console.info(new Date());
  soap.createClient(url, function(err, client) {
    client.QueryPatientList(args, function(err, result) {
      co(function*(){
        const data = yield new Promise(function(resolve,reject){
          fs.writeFile('D://abc.js',result.QueryPatientListResult)
          parseString('<result>'+result.QueryPatientListResult+'</result>', function (err, parseData) {
              if(err){
                reject(err);
              }else{
                console.info(JSON.stringify(parseData))
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
