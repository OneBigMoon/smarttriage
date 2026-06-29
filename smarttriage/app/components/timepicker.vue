<template>
  <label class="form-control timepicker clearfix">
    <input type="number" class="timepicker-control" v-model="h">
    <div class="timepicker-spliter">:</div>
    <input type="number" class="timepicker-control" v-model="m">
    <div class="timepicker-spliter">:</div>
    <input type="number" class="timepicker-control" v-model="s">
  </label>
</template>

<script>
exports.props = {
  value: {
    type: String,
    required: true,
    twoWay: true
  }
};

function getValues(_this) {
  return _this.value.split(':');
}

function setValue(_this, place, max, value) {
  const values = getValues(_this);
  if (value < 0) {
    value = '0';
  }
  if (value > max) {
    value = '' + max;
  }
  value = ('00' + value).substr(-2);
  values[place] = value;
  _this.value = values.join(':');
}

exports.computed = {
  h: {
    get() {
      return getValues(this)[0];
    },
    set(h) {
      setValue(this, 0, 23, h);
    }
  },
  m: {
    get() {
      return getValues(this)[1];
    },
    set(m) {
      setValue(this, 1, 59, m);
    }
  },
  s: {
    get() {
      return getValues(this)[2];
    },
    set(s) {
      setValue(this, 2, 59, s);
    }
  }
};

exports.watch = {
  'value'(val) {
    if (!val) {
      this.value = '00:00:00';
    }
  }
};

exports.ready = function() {
  const _this = this;
  if (!_this.value) {
    _this.value = '00:00:00';
  }
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.timepicker
  display: block
  margin: 0
  font-weight: normal
  &-control
    float: left
    width: 30%
    border: 0
    outline: 0
    text-align: center
    -moz-appearance: textfield // 隐藏上下箭头 Firefox
    &::-webkit-outer-spin-button // 隐藏上下箭头 Chrome
    &::-webkit-inner-spin-button
      // display: none Crashes Chrome on hover
      -webkit-appearance: none
      margin: 0 // Apparently some margin are still there even though it's hidden
  &-spliter
    float: left
    width: 5%
    text-align: center

@media (min-width: $grid-float-breakpoint)
  .form-inline
    .timepicker
      width: 90px
</style>
