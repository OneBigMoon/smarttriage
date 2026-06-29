var oracledb = require('oracledb');
var fs = require('fs');

oracledb.fetchAsBuffer = [oracledb.BLOB];
oracledb.fetchAsString = [oracledb.CLOB];

oracledb.getConnection(
  {
    user          : "system",
    password      : "root1234",
    connectString : "localhost/orcl"
  },
  function(err, connection)
  {
    if (err) {
      console.error(err.message);
      return;
    }
    connection.execute(
      "SELECT * FROM VW_PDJH_HZBR00 where ID0000='17156050'",
      function(err, result)
      {
        if (err) {
          console.error(err.message);
          doRelease(connection);
          return;
        }
        console.log(result.rows[0][16]);
        fs.writeFile('D:/a.jpg', result.rows[0][16], function(err) {
          if (err) {
            console.log(err)
          }
        });
        doRelease(connection);
      });
  });

function doRelease(connection){
  connection.close(
    function(err) {
      if (err)
        console.error(err.message);
    });
}
