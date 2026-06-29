<template>
  <div class="toolbar clearfix">
    <slot></slot>
  </div>
</template>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.toolbar
  background: $white
  margin-bottom: $form-group-margin-bottom
  padding: ($grid-gutter-width / 2)

  @media (min-width: $grid-float-breakpoint)
    .form-group + .form-group
      margin-left: ($grid-gutter-width / 2)
    .toolbar-right
      float: right
</style>
