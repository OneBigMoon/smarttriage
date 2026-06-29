<script>
const {
  extend
} = require('../utils');

const Datepicker = require('../../node_modules/vue-strap/src/Datepicker');
extend(exports, Datepicker);

exports.props.format.default = 'yyyy-MM-dd';

exports.watch.value = function(value) {
  const _this = this;
  _this.currDate = _this.parse(value) || _this.parse(new Date());
};

exports.ready = function() {
  const _this = this;
  Datepicker.ready.call(_this);
  _this.weekRange = '日一二三四五六'.split('');
  _this.monthNames = '一,二,三,四,五,六,七,八,九,十,十一,十二'.split(',').map((month) => month + '月');
};
</script>
