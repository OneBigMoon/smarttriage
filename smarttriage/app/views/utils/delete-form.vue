<template>
  <dialog :show.sync="show" :title="title" :confirm="delete" :width="320">
    <h4 class="text-center" slot="dialog-body">
      <i class="fa fa-power-off"></i> 您确认要删除吗？
    </h4>
  </dialog>
</template>

<script>

const Dialog = require('../../components/dialog');

exports.components = {
  Dialog
};

exports.props = [
  'show',
  'title',
  'event'
];

exports.methods = {
  delete() {
    const _this = this;
    _this.show = false;
    _this.event && _this.$dispatch(_this.event);
  }
};
</script>
