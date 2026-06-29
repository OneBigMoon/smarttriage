<template>
  <toolbar class="text-center text-muted">
    <h1><i class="fa fa-dropbox"></i></h1>
    <p>没有符合条件的<slot></slot>记录</p>
  </toolbar>
</template>

<script>
const Toolbar = require('./toolbar');

exports.components = {
  Toolbar
};
</script>
