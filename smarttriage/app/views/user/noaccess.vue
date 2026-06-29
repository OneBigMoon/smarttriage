<template>
  <div class="text-center">
    <br>
    <p class="text-warning"><i class="fa fa-warning"></i> 对不起，您没有权限访问该资源！</p>
    <br>
    <!--<button class="btn" @click="back">返回首页</button>-->
  </div>
</template>

<script>
const Auth = require('../../services/auth');

const {
  go
} = require('../../utils');

exports.methods = {
  back() {
    const path = Auth.getFirstPath();
    if (path == '/noaccess') {
      Auth.logout();
      return true;
    }

    go(path);
  }
};
</script>
