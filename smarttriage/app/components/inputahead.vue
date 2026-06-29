<template>
  <div>
    <dropdown class="input-ahead" v-el:drop>
      <input type="text" :class="inputClass" :placeholder="placeholder"
             v-model="label" @keyup="onKeyup | debounce 300" @keyup.enter="onEnter" @blur="onBlur | debounce 300">
      <button type="button" class="btn btn-default btn-caret" data-toggle="dropdown" @click="toggle">
        <i class="fa fa-caret-down" ></i>
      </button>
      <input type="text" v-model="labelTemp" class="hide">
      <div name="dropdown-menu" class="dropdown-menu">
        <div class="list">
          <div class="list-item" :class="{ 'active': model.value == row.value }" @click="setValue(row)"
               v-for="row in list | filterBy filterLabel">
            <div class="pull-right">{{row.label}}</div>
            {{row.value}}
          </div>
        </div>
        <pagination class="pull-right" :total="total" :start.sync="start" :limit="limit"></pagination>
      </div>
    </dropdown>
  </div>
</template>

<script>
const Utils = require('../utils');
const Vue = require('vue');
const Dropdown = require('vue-strap').dropdown;
const Pagination = require('./pagination');

const extend = Utils.extend;
const pick = Utils.pick;
const clone = Utils.clone;

exports.components = {
  Dropdown,
  Pagination
};

exports.props = {
  inputClass: {
    type: String
  },
  placeholder: {
    type: String
  },
  action: {
    type: Function,
    default() {
      return Utils.noop;
    }
  },
  model: {
    type: Object,
    twoWay: true
  },
  key: {
    type: String,
    default: '_id'
  },
  filterKey: {
    type: Array,
    default() {
      return [];
    }
  },
  filterValue: {
    type: Array,
    default() {
      return [];
    }
  },
  real: {
    type: Boolean,
    default: true
  },
  limit: {
    type: Number,
    default: 10
  }
};

exports.data = () => {
  return {
    label: '',
    filterLabel: '',
    list: [],
    total: 0,
    start: 0
  };
};

exports.computed = {
  labelTemp() {
    this.label = this.model.label;
    return this.model.label;
  }
};

exports.methods = {
  query(val) {
    const _this = this;

    const copyParams = {
      filterKey: clone(_this.filterKey),
      filterValue: clone(_this.filterValue)
    };

    const params = extend(copyParams, pick(_this, [
      'start',
      'limit'
    ]));

    if (_this.real && val) {
      const keys = _this.key || '_id';
      Utils.each(keys.split(','), (key) => {
        params.filterKey.push(key);
        params.filterValue.push(val);
      });
    }

    _this.action(params, (data) => {
      _this.list = data.list;
      _this.total = data.total;
    }, Utils.handleError);
  },
  toggle() {
    const _this = this;

    _this.filterLabel = '';
    if (_this.start === 0) {
      if (_this.real || !_this.list.length) {
        _this.query();
      }
    } else {
      _this.start = 0;
    }
  },
  setValue(item) {
    this.dropHide();

    this.label = item.label;
    this.model = item;
  },
  dropShow() {
    const classList = this.$els.drop.classList;
    if (!classList.contains('open')) {
      classList.add('open');
    }
  },
  dropHide() {
    const classList = this.$els.drop.classList;
    classList.remove('open');
  },
  onEnter() {
    const _this = this;

    _this.setDefaultValue();
  },
  onKeyup(e) {
    const _this = this;

    if (e.which != 13) {
      _this.model.value = '';
      _this.dropShow();
      if (_this.real || !_this.list.length) {
        _this.query(_this.label);
      }
    }
  },
  onBlur() {
    const _this = this;

    if (!_this.model.value) {
      _this.setDefaultValue();
    }
  },
  setDefaultValue() {
    const _this = this;

    let selectValue = {
      value: '',
      label: ''
    };

    if (_this.label && _this.list.length) {
      if (!this.real) {
        const filterList = Vue.filter('filterBy')(_this.list, _this.filterLabel);
        if (filterList && filterList.length) {
          selectValue = filterList[0];
        }
      } else {
        selectValue = _this.list[0];
      }
    }

    _this.setValue(selectValue);
  }
};

exports.watch = {
  start() {
    this.query();
  },
  filterValue() {
//    this.query(); 过滤条件改变时不及时查询数据
    this.list = [];
  },
  label() {
    if (!this.real) {
      this.filterLabel = this.label;
    }
  }
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

// Menu position and menu carets
.input-ahead
  position: relative
  width: 100%
  > input
    padding-right: 40px
  > .btn-caret
    absolute: top 0 right 0
    border-radius: 0 !important
    z-index: 3

  .dropdown-menu
    width: 100%
    .list
      width: 100%
      max-height: 200px
      overflow: auto
      font-size: $font-size-small
      &-item
        padding: 8px 10px
        border-bottom: 1px solid $border-color
        cursor: pointer
        &.active
          background: $brand-info
          color: $white
    nav
      padding: 5px
      padding-bottom: 0
      .pagination
        margin-bottom: 0
        margin-top: 5px
</style>
