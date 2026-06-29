<script>
module.exports = require('../../node_modules/vue-strap/src/Tabset');
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.nav-tabs
  margin: 0 !important
.tab-content
  padding: 15px

.tabs-nav
  > .nav-tabs
    background: $white
    border: 0
    > li
      > a
        color: $text-color
        border: 0
        border-bottom: 2px solid transparent
        min-width: 80px
        text-align: center
        margin-left: 20px
        &:hover
        &:focus
          background: $white
      &.active > a
        &
        &:hover
        &:focus
          background: $white
          color: $brand-warning
          border: 0
          border-bottom: 2px solid $brand-warning

.tabs-pill
  $tab-li-radius = 4px
  > .nav-tabs
    border: 0
    text-align: center
    padding-top: 15px
    > li
      display: inline-block
      float: none
      min-width: 80px
      > a
        background: $gray-lighter !important
        color: $text-color
        margin: 0
        border: 0 !important
      &:first-child
        > a
          border-top-left-radius: $tab-li-radius
          border-bottom-left-radius: $tab-li-radius
      &:last-child
        > a
          border-top-right-radius: $tab-li-radius
          border-bottom-right-radius: $tab-li-radius
      &.active > a
        background: $brand-success !important
        color: $white !important

.tabs-fixed-top
  > .nav-tabs
    position: fixed
    top: $navbar-height
    width: 100%
    z-index: $zindex-navbar-fixed - 5
  > .tab-content
    padding-top: 40px + 15
  &.tabs-pill
    > .nav-tabs
      left: ($sidenav-width / 2)
    > .tab-content
      padding-top: 40px + 15 + 15
</style>
