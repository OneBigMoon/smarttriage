<template>
  <div class="panel panel-default widget">
    <div class="panel-heading">
      <h4 class="panel-title">
        {{header}}
        <slot name="header"></slot>
      </h4>
    </div>
    <div class="table-responsive">
      <slot></slot>
    </div>
  </div>
</template>

<script>
exports.props = {
  header: {
    type: String
  }
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.panel
  &.widget
    .panel
      &-heading
        position: relative
        &:before
          content: ""
          absolute: top $padding-base-vertical left (($grid-gutter-width - 4) / 2)
          size: 4px $font-size-h4
          background: $brand-primary

        .checkbox
          margin: 2px 0
          font-size: 14px

      &-collapse
        .table
          > tbody
            > tr
              &:first-child
                > th
                > td
                  border-top: 0
</style>
