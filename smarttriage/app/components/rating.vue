<template>
  <div class="rating">
    <i class="fa fa-diamond star" v-for="s in stars | orderBy 'i' -1"
       :class="{active: s.active}" @click="rate(s)"></i>
  </div>
</template>

<script>
const {
  each
} = require('../utils');

exports.props = [
  'min',
  'max',
  'value',
  'enable'
];

exports.data = () => {
  return {
    loaded: false,
    stars: []
  };
};

exports.methods = {
  rate(s) {
    this.loaded = true;
    if (this.enable && s.enable) {
      this.value = s.i;
    }
  },
  update() {
    const _this = this;
    each(_this.stars, function(star, i) {
      star.active = _this.value && _this.value > i;
    });
  },
  init() {
    const _this = this;
    const {
      enable,
      value
    } = _this;
    const stars = _this.stars = [];
    const min = _this.min || 1;
    const max = enable ? (_this.max || 6) : value;

    let i = 1;
    for (; i <= max; i++) {
      stars.push({
        i,
        enable: i >= min,
        active: value && value >= i
      });
    }
  }
};

exports.watch = {
  value() {
    const _this = this;
    if (_this.enable && _this.loaded) {
      _this.update();
      return;
    }

    _this.init();
  }
};

exports.ready = function() {
  this.init();
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.rating
  unicode-bidi: bidi-override
  direction: rtl
  font-size: inherit
  white-space: nowrap
  text-align: left
  display: inline-block

rating-star()
  &:before
    color: #aaa;

rating-star-active()
  &:before,
  ~ .star:before
    color: $brand-warning

.star,.static-star
  font-family: FontAwesome
  font-weight: normal
  font-style: normal
  display: inline-block
  rating-star()

  &:before
    padding-right: 5px

  &:hover
    cursor: pointer

  &.active
    rating-star-active()

  &:hover
    .star
      &,
      &.active
        rating-star()
      &:hover,
      &.active:hover
        rating-star-active()

</style>
