<template>
  <div class="v-radios">
    <span class="header btn-group" v-if="title">{{{title}}}：</span>
    <radio-group :value.sync="value" type="{{type}}">
      <radio value="">不限</radio>
      <radio :value="key" v-for="(key, name) in items">{{name}}</radio>
    </radio-group>
  </div>
</template>

<script>
const RadioGroup = require('../../node_modules/vue-strap/src/radioGroup');
const Radio = require('../../node_modules/vue-strap/src/radioBtn');

exports.components = {
  RadioGroup,
  Radio
};

exports.props = [
  'title',
  'value',
  'type',
  'items'
];
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.v-radios
  font-size: 14px
  padding: 0 15px
  .header
    width: 80px
    text-align: right
  .btn
    margin: 3px 10px
    border: 1px solid $table-border-color
    &.active
      border-color: $brand-success
      color: $brand-success

</style>
