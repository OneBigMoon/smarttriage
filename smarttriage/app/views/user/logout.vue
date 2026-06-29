<template>
  <dialog :show.sync="show" title="退出" :confirm="logout">
    <h4 class="text-center" slot="dialog-body">
      <i class="fa fa-power-off"></i> 您要退出平台吗？
    </h4>
  </dialog>
</template>

<script>
const Dialog = require('../../components/dialog');
const Auth = require('../../services/auth');

exports.components = {
  Dialog
};

exports.props = {
  show: {
    type: Boolean,
    twoWay: true
  }
};

exports.methods = {
  logout() {
    Auth.logout();
    return true;
  }
};
</script>
