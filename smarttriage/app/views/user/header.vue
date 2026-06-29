<template>
  <div class="page-header">
    <div class="page-header-title">
      <div class="title">{{$route.name}}</div>
      <select name="province" v-model="province" class="form-control" v-show="provinces.length > 1">
        <option :value="p" v-for="p in provinces">{{p.label}}</option>
      </select>
      <select name="city" v-model="city" class="form-control" v-show="provinces.length > 1 || cities.length > 1">
        <option :value="c" v-for="c in cities">{{c.label}}</option>
      </select>
      <select name="office" v-model="office" class="form-control" v-show="subOffices.length">
        <option value="{{o.value}}" v-for="o in subOffices">{{o.label}}</option>
      </select>
    </div>
    <div class="page-header-time text-right">
      <div class="time time-left">
        <div class="time-xs text-left">{{time.week | fWeek}}</div>
        <div><span>{{time.day | fillZero}}</span><span class="time-xs">{{time.month | fMonth}}</span></div>
      </div><div class="time time-right">
        <div class="time-xs text-right">{{time.am ? 'AM' : 'PM'}}</div>
        <div><span>{{time.hour | fillZero}}:{{time.minute | fillZero}}</span></div>
      </div>
    </div>
  </div>
</template>

<script>
const Auth = require('../../services/auth');
const Utils = require('../../utils');

const {
  each,
  cookie
} = Utils;

let timer;

exports.components = {
};

exports.props = [
  'offices',
  'office'
];

exports.data = () => {
  return {
    provinces: [],
    cities: [],
    subOffices: [],
    province: {
      value: '',
      label: '',
      children: []
    },
    city: {
      value: '',
      label: '',
      children: []
    },
    time: {
      week: '',
      month: '',
      day: '',
      hour: '',
      minute: '',
      am: true
    }
  };
};

exports.methods = {
  getTime() {
    const {
      time
      } = this;

    const now = new Date();
    if (now) {
      time.month = now.getMonth();
      time.day = now.getDate();
      time.week = now.getDay();
      const hour = now.getHours();
      time.am = hour <= 12;
      time.hour = time.am ? hour : (hour - 12);
      time.minute = now.getMinutes();
    }
  },
  setTimer() {
    const {
      getTime,
      clearTimer
    } = this;
    getTime();
    clearTimer();
    timer = setInterval(getTime, 1000 * 1);
  },
  clearTimer() {
    timer && clearInterval(timer);
  },
  setProvince(province) {
    this.cities = province.children;
    const cookieCity = cookie.get('offices.city');
    let findCityItem = null;
    if (cookieCity) {
      each(this.cities, (cItem) => {
        if (cItem.value == cookieCity) {
          findCityItem = cItem;
          return false;
        }
      });
    }
    this.city = findCityItem ? findCityItem : this.cities[0];
    this.setCity(this.city);
  },
  setCity(city) {
    this.subOffices = city.children;
    const cookieOffice = cookie.get('offices.office');
    let findOfficeItem = null;
    if (cookieOffice) {
      each(this.subOffices, (oItem) => {
        if (oItem.value == cookieOffice) {
          findOfficeItem = oItem;
          return false;
        }
      });
    }
    this.office = findOfficeItem ? findOfficeItem.value : this.subOffices[0].value;
    // 获取超时预警设置
    Auth.baseconf({
      officeid: city.value
    }, (data) => {
      Utils.queuewarning = (data && data.queuewarningsuper) || '';
    });
  },
  goCharts() {
    Utils.go({
      path: '/charts',
      replace: true
    });
  }
};

function foundMult(array, type, childObj) {
  if (array && array.length) {
    if (type) {
      childObj[type] = array;
    }
    each(array, (item) => {
      foundMult(item.children, item.type, childObj);
    });
  }
}

exports.watch = {
  'offices'(value) {
    const childrenObj = {};
    if (value && value.length) {
      foundMult(value, '', childrenObj);
    }

    this.provinces = childrenObj.country || [];
    if (this.provinces.length) {
      const cookiePro = cookie.get('offices.province');
      let findProItem = null;
      if (cookiePro) {
        each(this.provinces, (proItem) => {
          if (proItem.value == cookiePro) {
            findProItem = proItem;
            return false;
          }
        });
      }
      this.province = findProItem ? findProItem : this.provinces[0];
      this.setProvince(this.province);
    }
  },
  'province.value'(value) {
    if (value) {
      cookie.set('offices.province', value);
      this.setProvince(this.province);
    }
  },
  'city.value'(value) {
    if (value) {
      cookie.set('offices.city', value);
      this.setCity(this.city);
    }
  }
};

exports.ready = function() {
  this.setTimer();
};

exports.beforeDestroy = function() {
  this.clearTimer();
};
</script>

<style lang="stylus">
@import "../../styles/variables"

$page-border-width = 4px

.page-header
  margin: 0 10px
  padding-bottom: $page-border-width
  border-bottom-width: $page-border-width

  display: flex
  align-items: center

  &-title
    padding: 15px 0 1px 0
    .title
      font-size: 20px
      color: $gray-darker
      padding: 0 5px
    .form-control
      display: inline-block
      background: transparent
      border-color: transparent
      box-shadow: none
      width: auto
      height: $input-height-base
      padding-left: 0
      padding-right: 0
      color: $gray-dark

  &-time
    font-size: 24px
    line-height: 1
    flex: 1
    padding: 0 5px
    .time
      display: inline-flex
      flex-direction: column
      padding: 4px
      &-left
        background: $brand-info
        color: $white
      &-right
        background: $white
        color: $brand-info
      &-xs
        font-size: 10px
</style>
