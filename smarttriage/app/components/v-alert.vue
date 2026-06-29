<script>
const VAlert = module.exports = require('../../node_modules/vue-strap/src/Alert');

const {
  noop
} = require('../utils');

VAlert.props.callback = {
  type: Function,
  default() {
    return noop;
  }
};
</script>

<style lang="stylus">
@import "../styles/variables"

#alert
  z-index: ($zindex-tooltip + 10)
  padding: 0 15px
  .fa
    float: left
    margin-right: $alert-padding
    font-size: $font-size-h1
  .close
  .content
    padding: 15px 0

</style>
