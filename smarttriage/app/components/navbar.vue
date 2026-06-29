<template>
  <nav class="navbar navbar-inverse navbar-fixed-top">
    <div class="container-fluid">
      <!-- Brand and toggle get grouped for better mobile display -->
      <div class="navbar-header">
        <a class="navbar-brand hidden-xs" v-link="'/'">
          <img src="../assets/logo.png">
        </a>
      </div>
      <span class="logout" @click="logout">
        <i class="fa fa-power-off"></i>
      </span>
    </div><!-- /.container-fluid -->
  </nav>
  <sidenav id="sidenav" class="hidden-xs" :items.sync="sidenav"></sidenav>
  <div id="sidenav-xs" class="visible-xs-block">
    <aside placement="left" header="导航" :show.sync="sideon" :width="160">
      <sidenav :items.sync="sidenav"></sidenav>
    </aside>
  </div>
</template>

<script>
const Aside = require('../../node_modules/vue-strap/src/Aside');
const Sidenav = require('./sidenav');
const ApiV1 = require('../services/api-v1');
const {
  handleError
} = require('../utils');

exports.components = {
  Aside,
  Sidenav
};

exports.props = [
  'title',
  'sidenav',
  'offices',
  'office',
  'username'
];

exports.data = () => {
  return {
    sideon: false
  };
};

exports.methods = {
  logout() {
    ApiV1.logout.post({}, () => {
      this.$router.go('/login');
    }, () => {
      handleError({ message: '登出失败' });
    });
  }
};

</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

//
// Navbars
// --------------------------------------------------

// Wrapper and base class
//
// Provide a static navbar from which we expand to create full-width, fixed, and
// other navbar variations.

.navbar
  position: relative
  height: $navbar-height
  margin-bottom: $navbar-margin-bottom
  border: 1px solid transparent

  // Prevent floats from breaking the navbar
  clearfix()

  @media (min-width $grid-float-breakpoint)
    border-radius: $navbar-border-radius

  &-select
    display: inline-block
    width: auto
    min-width: 80px
    margin-top: 8px
    margin-left: 15px
    background: transparent
    border-color: transparent
    box-shadow: none
    color: $white
    float: left
    option
      color: $text-color

  .logout
    float: right
    font-size: 25px
    color: #425865
    margin-right: 3px
    margin-top: 7px

// Navbar heading
//
// Groups `.navbar-brand` and `.navbar-toggle` into a single component for easy
// styling of responsive aspects.

.navbar-header
  clearfix()

  // @media (min-width $grid-float-breakpoint)
  float: left

//
// Navbar alignment options
//
// Display the navbar across the entirety of the page or fixed it to the top or
// bottom of the page.

// Fix the top/bottom navbars when screen real estate supports it
.navbar-fixed-top
  position: fixed
  right: 0
  left: 0
  top: 0
  border-width: 0 0 1px
  z-index: $zindex-navbar-fixed

  // Undo the rounded corners
  @media (min-width $grid-float-breakpoint)
    border-radius: 0

// Brand/project name

.navbar-brand
  float: left
  padding: 0 $navbar-padding-horizontal
  font-size: $font-size-large
  line-height: $navbar-height
  height: $navbar-height

  img
    vertical-align: middle
    height: 30px

  &:hover
  &:focus
    text-decoration: none

// Navbar toggle
//
// Custom button for toggling the `.navbar-collapse`, powered by the collapse
// JavaScript plugin.

.navbar-toggle
  position: relative
  float: left
  margin-right: $navbar-padding-horizontal
  padding: 9px 10px
  navbar-vertical-align(34px)
  background-color: transparent
  // Reset unusual Firefox-on-Android default style see https://github.com/necolas/normalize.css/issues/214
  background-image: none
  border: 1px solid transparent
  border-radius: $border-radius-base

  // We remove the `outline` here, but later compensate by attaching `:hover`
  // styles to `:focus`.
  &:focus
    outline: 0

  // Bars
  .icon-bar
    display: block
    width: 22px
    height: 2px
    border-radius: 1px
  .icon-bar + .icon-bar
    margin-top: 4px

  @media (min-width $grid-float-breakpoint)
    display: none


// Navbar nav links
//
// Builds on top of the `.nav` components with its own modifier class to make
// the nav the full height of the horizontal nav (above 768px).

.navbar-nav
  margin: ($navbar-padding-vertical / 2) (-($navbar-padding-horizontal))

  > li > a
    padding-top: 10px
    padding-bottom: 10px

  // Uncollapse the nav
  // @media (min-width $grid-float-breakpoint)
  float: left
  margin: 0

  > li
    float: left
    > a
      padding-top: $navbar-padding-vertical
      padding-bottom: $navbar-padding-vertical

// Dropdown menus

// Menu position and menu carets
.navbar-nav > li
  > .dropdown-menu
    display: block
    margin-top: 0
    padding: 0
    border-top-radius(0)
    display: block
    transition: transform .3s
    transform: scaleY(0)
    transform-origin: 0 0
    > li > a
      padding: (($navbar-height - $line-height-computed) / 2) 20px
  &:focus
  &:hover
    .dropdown-menu
      transform: none

.navbar-right
  .dropdown-menu
    right: 0
    left: auto

// Component alignment
//
// Repurpose the pull utilities as their own navbar utilities to avoid specificity
// issues with parents and chaining. Only do this when the navbar is uncollapsed
// though so that navbar contents properly stack and align in mobile.
//
// Declared after the navbar components to ensure more specificity on the margins.

// @media (min-width $grid-float-breakpoint)
.navbar-left
  float: left !important

.navbar-right
  float: right !important
  margin-right: -($navbar-padding-horizontal)

  ~ .navbar-right
    margin-right: 0


// Alternate navbars
// --------------------------------------------------

// Inverse navbar

.navbar-inverse
  background-color: $navbar-inverse-bg
  border-color: $navbar-inverse-border

  .navbar-brand
    color: $navbar-inverse-brand-color
    background: #545353b5
    margin: 2px 0
    border-radius: 1px
    line-height: $navbar-height - 5
    height: $navbar-height - 5
    /*&:hover
    &:focus
      color: $navbar-inverse-brand-hover-color
      background-color: $navbar-inverse-brand-hover-bg
      background: $white*/

  .navbar-nav
    > li > a
      color: $navbar-inverse-link-color
      line-height: $navbar-height
      height: $navbar-height
      padding: 0 15px

      &:hover
      &:focus
        color: $navbar-inverse-link-hover-color
        background-color: $navbar-inverse-link-hover-bg

    > .active > a
      &
      &:hover
      &:focus
        color: $navbar-inverse-link-active-color
        background-color: $navbar-inverse-link-active-bg

    > .disabled > a
      &
      &:hover
      &:focus
        color: $navbar-inverse-link-disabled-color
        background-color: $navbar-inverse-link-disabled-bg

  // Darken the responsive nav toggle
  .navbar-toggle
    border-color: $navbar-inverse-toggle-border-color

    &:hover
    &:focus
      background-color: $navbar-inverse-toggle-hover-bg
    .icon-bar
      background-color: $navbar-inverse-toggle-icon-bar-bg

  // Darken the dropdown
  .dropdown-menu
    background: ($navbar-inverse-bg + 10%)
    border-color: $navbar-inverse-border
    border-top: 0
    > li > a
      background: ($navbar-inverse-bg + 10%)
      border-top: 1px solid $navbar-inverse-border
      color: $navbar-inverse-color
      &:focus
      &:hover
        background: ($navbar-inverse-bg + 15%)


//
// Sidebar
// --------------------------------------------------

#sidenav
  fixed: top $navbar-height left 0
  size: $sidenav-width 100%
  z-index: 1
  &-xs
    .aside
      min-width: 0
      &-header
        height: $navbar-height
        padding-top: 0
        padding-bottom: 0
        background: $navbar-inverse-bg
        border-color: $navbar-inverse-border
        .aside-title
        .close
          margin: 0
          padding-top: 0
          padding-bottom: 0
          line-height: $navbar-height
      &-body
        padding: 0
</style>
