<template>
  <div class="sidenav">
    <accordion :one-at-atime="checked">
      <template v-for="i in items">
        <div class="list-group" v-if="i.children && i.children.length">
          <panel :is-open="$index==activeIndex" v-if="i.children.length > 1">
            <div slot="header">
              <div class="title"><i class="fa fa-{{i.icon}}"></i> {{i.name}}</div>
            </div>
            <a class="list-group-item btn btn-default" v-link="c.link" v-for="c in i.children">
              {{c.name}}
            </a>
          </panel>

          <a class="list-group-item btn btn-default list-group-item-default"
             v-link="c.link" v-for="c in i.children" v-else>
            <i class="fa fa-{{i.icon}}"></i> {{c.name}}
          </a>
        </div>
      </template>
    </accordion>
  </div>
</template>

<script>
const Dialog = require('./dialog');

const Accordion = require('../../node_modules/vue-strap/src/Accordion');
const Panel = require('../../node_modules/vue-strap/src/Panel');

const {
  each
} = require('../utils');

exports.components = {
  Dialog,
  Accordion,
  Panel
};

exports.props = [
  'items'
];

exports.data = () => {
  return {
    show: false,
    checked: true,
    activeIndex: 0,
    scale: false
  };
};

exports.methods = {
  toggle(ev) {
    const _this = this;
    _this.show = !_this.show;
    if (_this.show) {
      const dom = _this.$els.code;
      const point = _this.getPoint(ev);
      dom.style.left = point.x + 'px';
      dom.style.top = point.y + 'px';
    }
  },
  getPoint(ev) {
    let { x, y } = 0;
    const doc = document.documentElement;
    ev = ev || window.event;

    x = 20;
    y = ev.clientY - (256 / 2);
    if (y + 256 + 20 > doc.clientHeight) {
      y = doc.clientHeight - 256 - 20;
    }

    return { x, y };
  }
};

exports.watch = {
  items(value) {
    const _this = this;

    if (value && value.length) {
      const path = _this.$route.path;
      each(value, (item, index) => {
        if (item.children && item.children.length) {
          each(item.children, (child) => {
            if (path.indexOf(child.link) >= 0) {
              _this.activeIndex = index;
            }
          });
        }
      });
    }
  }
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.sidenav
  $side-item-bg = #F4F4F4
  height: 100%
  padding: 0 0 $navbar-height
  background: $white
  .panel-group
    margin: 10px
  .list-group
    margin: 8px 0
    background: $side-item-bg
    border: 1px solid #ccc
    border-radius: 6px
    text-align: center
    .panel
      box-shadow: none
      background: transparent
      border: 0
      padding: 0 12px 6px
      &-heading
        background: transparent
        border: 0
        padding: 0
        .accordion-toggle
          text-decoration: none !important
      .title
        padding: 15px 0
        border-bottom: 1px solid $table-border-color

      &-body
        padding: 0

  .list-group-item
    display: block
    margin: 10px 0
    padding: 12px 0
    font-size: 15px
    border-radius: 6px
    &.v-link-active
      background: $brand-success
      color: $white
    &-default
      margin: 0
      border: 0
      font-size: 18px
      background: $side-item-bg

  .code
    text-align: center
    padding-top: 150px
    position: relative
    &-body
      background: url('../assets/noimage.gif') no-repeat center
      background-size: contain;
      width: 256px
      height: 256px
      margin: 0 auto;
      margin-bottom: 10px;
      position: absolute;
      bottom: 15px;
      left: 20px;
      transform: scale(0.4) translate(-65%, 75%);
      transition: transform 0.3s;
      &.bigger
        transform: scale(1)
</style>
