<template>
  <div class="panel card">
    <div class="head">{{title}}</div>
    <div class="table-responsive">
      <slot></slot>
    </div>
  </div>
</template>

<script>
  exports.props = [
    'title'
  ];
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.card
  .head
    float: left
    width: $w = 24px
    padding: 5px
    background: $brand-info
    color: $white

    + .table-responsive
      margin-left: $w
      border: 1px solid $table-border-color
      width: auto

</style>
