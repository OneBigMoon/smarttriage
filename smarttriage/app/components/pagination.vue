<template>
  <div>
    <nav v-if="total">
      <ul class="pagination hidden-xs">
        <li class="plain">
          <div class="item">共 {{total}} 条</div>
        </li>
      </ul>
      <ul class="pagination hidden-xs">
        <li :class="{disabled: page < 2}">
          <div class="item" @click="go(previous)">&lsaquo;</div>
        </li>
        <li :class="{active: page == 1}">
          <div class="item" @click="go(1)">1</div>
        </li>
        <li class="plain" v-if="pages[0] > 2">
          <div class="item">&hellip;</div>
        </li>
        <li v-for="p in pages" track-by="$index" :class="{active: p == page}">
          <div class="item" @click="go(p)">{{p}}</div>
        </li>
        <li class="plain" v-if="last - 1 > pages[pages.length - 1]">
          <div class="item">&hellip;</div>
        </li>
        <li :class="{active: page == last}" v-if="last">
          <div class="item" @click="go(last)">{{last}}</div>
        </li>
        <li :class="{disabled: !next}">
          <div class="item" @click="go(next)">&rsaquo;</div>
        </li>
      </ul>
      <ul class="pagination visible-xs-block">
        <li :class="{disabled: page < 2}">
          <div class="item" @click="go(1)">&laquo;</div>
        </li>
        <li :class="{disabled: page < 2}">
          <div class="item" @click="go(previous)">&lsaquo;</div>
        </li>
        <li class="active">
          <div class="item">{{page}}</div>
        </li>
        <li :class="{disabled: !next}">
          <div class="item" @click="go(next)">&rsaquo;</div>
        </li>
        <li :class="{disabled: !last}">
          <div class="item" @click="go(last)">&raquo;</div>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script>
const {
  extend,
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
  },
  size: {
    type: Number,
    default: 9
  }
};

function calculate(total, start, limit, size) {
  const pages = [];
  const restSize = size - 1;
  const halfSize = ~~(restSize / 2);
  let page;
  let last;
  let fix;
  let begin;
  let end;
  let previous;
  let next;

  limit = limit > 0 ? limit : 1;
  page = start / limit + 1;
  page = page > 0 ? page : 1;
  total = total > 0 ? total : 1;

  if (total > limit) {
    limit = limit > 0 ? limit : 1;
    page = page > 0 ? page : 1;
    last = Math.ceil(total / limit);
    fix = last - page > halfSize ? halfSize : restSize - last + page;
    begin = page - fix < 1 ? 1 : page - fix;
    end = begin + restSize > last ? last : begin + restSize;
    for (let i = begin; i <= end; i++) {
      pages.push(i);
    }

    if (page > 1) {
      previous = page - 1;
    }
    if (page < last) {
      next = page + 1;
    }
  } else {
    pages.push(1);
  }

  pages.shift();
  pages.pop();

  return {
    page,
    pages,
    previous,
    next,
    last
  };
}

exports.data = () => {
  return {
    page: 1,
    pages: [],
    previous: 0,
    next: 0,
    last: 0
  };
};

exports.methods = {
  go(page) {
    const _this = this;
    if (page) {
      _this.page = page;
      _this.start = (page - 1) * _this.limit;
    }
  },
  update() {
    const _this = this;
    extend(_this, calculate(_this.total, _this.start, _this.limit, _this.size));

    if (_this.page < 1 || (_this.page > 1 && _this.total > 0 && _this.total / _this.limit < _this.page - 1)) {
      _this.go(1);
    }
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
