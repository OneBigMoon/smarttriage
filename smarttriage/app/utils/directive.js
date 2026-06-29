'use strict';

const {
  addClass,
  removeClass,
  closest
} = require('./');

exports.install = (Vue) => {
  Vue.directive('form-valid', {
    params: [
      'name'
    ],
    bind() {
      const _this = this;
      const scope = _this._scope || _this.vm;
      const errCls = 'has-warning';
      const form = closest(_this.el, 'form[name]')._vueForm.state;

      _this.group = closest(_this.el, '.form-group');
      _this.watcher = scope.$watch(form.$name, () => {
        if (form.$submitted && form[_this.params.name].$invalid) {
          addClass(_this.group, errCls);
        } else {
          removeClass(_this.group, errCls);
        }
      }, { deep: true });
    },
    unbind() {
      const _this = this;
      _this.watcher();
      delete _this.group;
    }
  });
};
