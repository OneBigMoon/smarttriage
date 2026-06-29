<template>
  <div>
    <modal class="dialog" :show.sync="show" :width="width" :backdrop="backdrop">
      <div slot="modal-header">
        <slot name="dialog-header">
          <div class="modal-header">
            <button type="button" class="close" @click="fn(0)"><span>&times;</span></button>
            <h4 class="modal-title" >
              <slot name="title">
                {{title}}
              </slot>
            </h4>
          </div>
        </slot>
      </div>
      <div slot="modal-body" class="modal-body">
        <slot name="dialog-body"></slot>
      </div>
      <div slot="modal-footer" class="modal-footer">
        <slot name="dialog-footer">
          <div class="footer">
            <button type="button" class="btn btn-warning" @click="fn(1)">
              确定
            </button>
            <button type="button" class="btn btn-default" @click="fn(0)" v-if="alert == null">
              取消
            </button>
          </div>
        </slot>
      </div>
    </modal>
  </div>
</template>

<script>
const Modal = require('../../node_modules/vue-strap/src/Modal');
const {
  addClass,
  fireEvent,
  isFunction
} = require('../utils');
const doc = document;

exports.components = {
  Modal
};

exports.props = {
  show: {
    type: Boolean,
    default: true
  },
  title: {
    type: String
  },
  width: {
    type: Number,
    default: 430
  },
  form: {
    type: String
  },
  confirm: {
  },
  alert: {
  },
  backdrop: {
    type: Boolean,
    default: true
  }
};

exports.methods = {
  fn(confirm) {
    const _this = this;
    const form = _this.form;
    if (confirm && form) {
      fireEvent(doc[form], 'submit');
    }
    if (!confirm || _this.confirm == null || _this.confirm === true ||
        (isFunction(_this.confirm) && _this.confirm())) {
      _this.show = false;
    }
  }
};

exports.ready = function() {
  const _this = this;
  const form = _this.form;
  if (form) {
    const submit = doc.createElement('input');
    submit.type = 'submit';
    addClass(submit, 'hidden');
    doc[form].appendChild(submit);
  }
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.dialog
  .modal
    &-content
      background: $body-bg
      border: 0

    &-header
      .close
        absolute: top 8px right $modal-title-padding
        font-size: 20px
        font-weight: normal
        margin: 0
        outline: none

    &-footer
      .footer
        text-align: center
        .btn
          &
          + .btn
              margin: 0 ($grid-gutter-width / 2)
            min-width: 90px

    &-dialog
      max-height: 100%
      padding: $grid-gutter-width 15px
      overflow-y: auto

      @media (max-width $grid-float-breakpoint-max)
        width: 90% !important
        padding: $grid-gutter-width 0
        margin: 0 auto

@media(min-width: $grid-float-breakpoint)
  .dialog.modal.zoom
    .modal-dialog
      absolute: top 50% left 50%
      transform: translate3d(-50%, 0, 0) scale(.1)
    &.in
      .modal-dialog
        margin: 0
        transform: translate(-50%, -50%)
</style>
