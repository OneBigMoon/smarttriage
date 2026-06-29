<template>
  <div class="table-sort" @click="change">
    <i class="fa fa-sort{{icons[state]}}"></i>
  </div>
</template>

<script>
const {
  addClass,
  removeClass
} = require('../utils/');

const SORTS = [
  undefined,
  'asc',
  'desc'
];

const WRAP_CLS = 'table-sort-wrap';

exports.props = {
  attr: {
    required: true
  },
  sort: {
    required: true,
    twoWay: true
  }
};

exports.data = () => {
  return {
    state: 0,
    icons: [
      '',
      '-asc',
      '-desc'
    ]
  };
};

exports.methods = {
  change() {
    const _this = this;
    const state = _this.state = (_this.state + 1) % SORTS.length;
    _this.sort = SORTS[state];
  }
};

exports.watch = {
  sort(sort) {
    const _this = this;
    _this.state = SORTS.indexOf(sort);
  }
};

exports.ready = function() {
  addClass(this.$el.parentNode, WRAP_CLS);
};

exports.beforeDestroy = function() {
  removeClass(this.$el.parentNode, WRAP_CLS);
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.table-sort
  absolute: top 0 bottom 0 left 0 right 0
  text-align: right
  padding: $table-cell-padding
  cursor: pointer
  &-wrap
    position: relative
</style>
