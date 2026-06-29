var request = require('request');
request('http://220.162.229.98:18083/ylzUP/yimapay/getQRCode?account_type=02&WIDsubject=预交金&system_id=001&amount=100&userid=1001&p_id=001&p_name=张三', function (error, response, body) {
  console.info(response.statusCode)
  if (!error && response.statusCode == 200) {
    console.log(body) // 打印google首页
  }else{
    console.error(error)
  }
})
