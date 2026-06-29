<template>
  <div class="back">
    <button type="button" class="btn btn-info" @click="back">
      <i class="fa fa-fw fa-angle-double-left"></i>
    </button>
  </div>
</template>

<script>
exports.methods = {
  back() {
    history.back();
  }
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.back
  margin-bottom: 10px
  .btn
    padding: 0 20px
    font-size: 22px
</style>
