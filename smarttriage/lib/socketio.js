'use strict';

const socketios = require('./controllers/socketios');

module.exports = {
  socketio: {
    connect: socketios.connect,
    apiv1_heartbeat: socketios.apiv1_heartbeat,
    apiv1_message: socketios.apiv1_message
    // login: Monitor.login,
    // logout: Monitor.logout,
    // transfer: Monitor.transfer
  }
};
