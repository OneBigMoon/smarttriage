<template>
  <div>
    <nav v-if="total || start">
      <ul class="pagination hidden-xs">
        <li :class="{disabled: page < 2}">
          <div class="item" @click="prev">&lsaquo; 上一页</div>
        </li>
        <li :class="{disabled: total < limit}">
          <div class="item" @click="next">下一页 &rsaquo;</div>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script>
const {
  each
} = require('../utils');

const props = exports.props = {
  total: {
    type: Number,
    default: 0
  },
  limit: {
    type: Number,
    default: 10
  },
  start: {
    type: Number,
    default: 0
  }
};

exports.data = () => {
  return {
    page: 1
  };
};

exports.methods = {
  prev() {
    const _this = this;
    if (_this.page > 1) {
      _this.page--;
      _this.start = (_this.page - 1) * _this.limit;
    }
  },
  next() {
    const _this = this;
    if (_this.total >= _this.limit) {
      _this.page++;
      _this.start = (_this.page - 1) * _this.limit;
    }
  },
  update() {
    const _this = this;

    _this.page = parseInt(_this.start / _this.limit, 10) + 1;
  }
};

exports.ready = function() {
  this.update();
};

const watch = exports.watch = {};
each(props, (val, prop) => watch[prop] = 'update');
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.pagination
  .plain
    > a
    > .item
      &
      &:hover
        background: $pagination-bg
        color: $pagination-disabled-color
  > li
    > a
    > .item
    > span
      padding: 6px 15px
</style>
