var oracledb = require('oracledb');

oracledb.fetchAsBuffer = [oracledb.BLOB];
oracledb.fetchAsString = [oracledb.CLOB];

const doRelease = function(connection) {
  connection.release(
    function(err) {
      if (err) {
        logger.error(err);
      }
    });
};

oracledb.createPool({
  user          : "system",
  password      : "root1234",
  connectString : "localhost/orcl",
  poolMax: 100,
  poolMin: 20,
  poolAlias: 'oraclePool'
}, function(err, pool) {
  if (err) {
    console.error(err);
  } else {
    console.info('初始化连接池成功');
    oracledb.getPool('oraclePool').getConnection(function(err, connection) {
      if (err) {
        return;
      }
      connection.execute('select JZKS00, ZSMC00, YSXM00, HZXHMC, BRXM00, GZBRXH, ' +
       'JHSJ00, DSXSXX, JHYYXX, HZBRXH from vw_ys_jhlsxx_fqsyyxy', function(err2, result) {
        if (err2) {
          doRelease(connection);
          return;
        }
        doRelease(connection);
        console.info(result.rows);
      });
    });
  }
});

setTimeout(function(){
  oracledb.getPool('oraclePool').getConnection(function(err, connection) {
    if (err) {
      return;
    }
    connection.execute('select JZKS00, ZSMC00, YSXM00, HZXHMC, BRXM00, GZBRXH, ' +
     'JHSJ00, DSXSXX, JHYYXX, HZBRXH from vw_ys_jhlsxx_fqsyyxy', function(err2, result) {
      if (err2) {
        doRelease(connection);
        return;
      }
      doRelease(connection);
      console.info(result.rows);
      console.info(11)
    });
  });
}, 5000);
