<template>
  <div class="photo">
    <!--<img :height="height" :src="value" v-if="value">-->
    <!--<div v-else>-->
      <!--<img :height="height" src="../assets/photo-female.png" v-if="sex=='女'">-->
      <!--<img :height="height" src="../assets/photo.png" v-else>-->
    <!--</div>-->
    <div class="photo-content" :style="{ backgroundImage: 'url(' + value + ')' }" v-if="value"></div>
    <img :height="height" src="../assets/photo-female.png" v-if="sex=='女'||sex=='F'">
    <img :height="height" src="../assets/photo-default.png" v-else>
  </div>
</template>

<script>
exports.props = [
  'value',
  'sex',
  'height'
];
</script>

<style lang="stylus">
.photo
  position: relative
  &-content
    background-size: cover
    background-repeat: no-repeat
    background-position: center
    position: absolute
    top: 0
    bottom: 0
    left: 0
    right: 0
.circle
  img
    border-radius: 100%
</style>
