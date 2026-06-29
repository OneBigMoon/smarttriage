<template>
  <div class="loading" v-show="show" transition>
    <i class="fa fa-spinner fa-pulse"></i>
  </div>
</template>

<script>
exports.props = [
  'show'
];
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.loading
  fixed: top 0 bottom 0 left 0 right 0
  background: radial-gradient(circle, $white, transparent 30%)
  z-index: $zindex-modal + 5
  &.v
    &-transition
      transition: all .3s
      opacity: 1
      visibility: visible
    &-enter
    &-leave
      opacity: 0
      visibility: hidden
  .fa
    fixed: top 50% left 50%
    width: ($font-size-h1 * 2)
    margin: (@width / -2) 0 0 (@width / -2)
    line-height: 1
    color: $text-muted
    font-size: @width
</style>
