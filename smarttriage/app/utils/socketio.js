'use strict';

const nsps = {};
let firstIoInstance;
let nspsLength = 0;
let isReconnect = false;

function connect(name) {
  require(['socket.io-client'], (io) => {
    function createConnection() {
      const location = window.location;
      const port = process.env.NODE_ENV ? location.port : process.env.PORT;
      const host = location.hostname + (port ? ':' + port : '');
      const currentIo = nsps[name];
      const currentInstance = currentIo.instance = io.connect(host + '/' + name);
      nspsLength++;
      currentInstance.shutdown = function(callback) {
        this.close();
        nsps[name] = null;
        nspsLength--;
        if (nspsLength < 1) {
          io.managers[location.protocol + '//' + host] = firstIoInstance = null;
        }
        callback && setTimeout(callback, 99);
      };
      if (nspsLength < 2) {
        firstIoInstance = currentInstance;
      }
      currentInstance.on('disconnect', function() {
        console.log('disconnect');
        isReconnect = false;
      });
      currentInstance.on('reconnect', function() {
        console.log('reconnect success');
        isReconnect = true;
      });
      currentIo.callbacks.forEach((cb) => {
        cb(currentInstance);
      });
    }

    if (firstIoInstance && !firstIoInstance.connected) {
      firstIoInstance.once('connect', createConnection);
    } else {
      createConnection();
    }
  });
}

module.exports = {
  initModule(name, callback) {
    if (name) {
      if (nsps[name]) {
        const currentIo = nsps[name];
        if (currentIo.instance) {
          callback(currentIo.instance);
        } else {
          currentIo.callbacks.push(callback);
        }
      } else {
        nsps[name] = {
          callbacks: [callback]
        };
        connect(name);
      }
    } else {
      callback();
    }
  },
  isReconn() {
    return isReconnect;
  },
  setReconn(val) {
    isReconnect = val;
  }
};
