<script>
const VSelect = module.exports = require('../../node_modules/vue-strap/src/Select');

VSelect.computed.selectedItems = function() {
  const _this = this;
  let label = '';
  _this.options.forEach((o) => {
    if (o.value == _this.value) {
      label = o.label;
    }
  });
  return label;
};

VSelect.methods.select = function(v) {
  const _this = this;
  _this.value = v;
  _this.toggleDropdown();
};

VSelect.ready = function() {};
</script>
