<template>
  <div class="upload">
    <div :id="id">
      <div class="dz-default dz-message" @click="show=true">
        <slot name="btn">
          <i v-if='icon != null' :class='rendIcon'></i> {{label}}
        </slot>
      </div>
      <input type="file" class="dz-hidden-input">
    </div>

    <div class="detail" :class="{in: upload}">
      <div id="{{id}}-previews" class="dropzone-previews"></div>
    </div>
  </div>
</template>

<script>
const Modal = require('vue-strap').modal;
const Dropzone = require('dropzone');
Dropzone.autoDiscover = false;

const TYPE_POSTFIX = {
  xls: ['xls', 'xlsx'],
  image: ['jpg', 'jpeg', 'png', 'gif', 'bmp'],
  video: ['mpeg4', 'mp4', 'ts', 'mkv', 'avi', 'wmv', 'mov', 'mpg']
};
TYPE_POSTFIX.stuff = TYPE_POSTFIX.image.concat(TYPE_POSTFIX.video);

function generateKey(code) {
  // 对于用户上传素材，文件名规则为：`素材类别编码_毫秒时间戳_4位随机数字`
  return code + '_' + Date.now() + ('_' + (1000 + Math.random() * 10000)).substr(0, 5);
}

// 错误解析
function parseError(code) {
  switch (code) {
    case 400:  // 请求错误
    case 401:  // 客户端认证授权失败
    case 405:  // 客户端请求错误
    case 631:  // 指定空间不存在
    case 701:  // 上传数据块校验出错
      return '请求错误，请稍后重试';
    case 579:  // 资源上传成功，但回调失败
    case 614:  // 文件已经存在
      return '';
    default:
      return '';
  }
}

function validFileName(name, key) {
  const strArray = name && name.split('.') || [];
  const length = strArray.length;
  if (length <= 1) {
    return false;
  }

  return TYPE_POSTFIX[key].indexOf(strArray[length - 1]) >= 0;
}

exports.components = {
  Modal
};

exports.props = [
  'key',
  'model',
  'icon',
  'label',
  'maxFilesize',
  'onSuccess',
  'onError',
  'upload'
];

exports.data = () => {
  return {
    id: 'dropzone' + ~~(Math.random() * 10000),
    percent: 0
  };
};

exports.computed = {
  rendIcon() {
    return this.icon.split(/\s/g);
  }
};

exports.ready = function() {
  const vm = this;
  vm.key = vm.key || 'stuff';
  vm.upload = false;
  let dropzone;
  let key;

  dropzone = new Dropzone('#' + vm.id, {
    previewsContainer: `#${vm.id}-previews`,
    dictDefaultMessage: '',
    dictFileTooBig: '请上传小于5M的文件',
    dictResponseError: '服务器异常，请再稍后尝试',
    url: `/api/v1/upload/${vm.key}`,
    maxFilesize: vm.maxFilesize || 5,
    accept: (file, done) => {
      if (!validFileName(file && file.name, vm.key)) {
        let errMsg = '无效的文件类型，请重新上传';
        if (TYPE_POSTFIX[vm.key]) {
          errMsg = `错误的文件类型，只允许上传${TYPE_POSTFIX[vm.key].join('，')}类型的文件`;
        }
        done(errMsg);
        return;
      }
      key = generateKey(vm.key);
      done();
    }
  });

  dropzone.on('sending', function(file, xhr, formData) {
    // 设置上传的key
    formData.append('key', key);
    vm.upload = true;
  });
  dropzone.on('complete', function(file) {
    vm.upload = false;
    key && (vm.model = key);
    dropzone.removeFile(file);
  });
  dropzone.on('success', function(file, xhr) {
    vm.onSuccess && vm.onSuccess(file, key, xhr);
  });
  dropzone.on('error', function(file, error, xhr) {
    const errorDesc = error || parseError(xhr && xhr.status);
    file && (file.previewElement = '');
    if (errorDesc) {
      vm.onError && vm.onError({
        message: errorDesc
      });
    }
  });
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.upload
  display: inline-block
  position: relative

  input[type=file]
    absolute: top 0 left 0
    size: 100%
    opacity: 0
    cursor: pointer

  .detail
    background: $white
    height: h=6px
    opacity: 0
    &.in
      opacity: 1
    .dropzone-previews
      .dz
        &-image
        &-details
        &-success-mark
        &-error-message
        &-error-mark
          display: none
        &-progress
          position: relative
          size: 100% h
          margin: 0 auto
          .dz-upload
            absolute: top 0 left 0
            height: 100%
            background: $brand-primary
            border-radius: (h / 2)

.dz-hidden-input
  visibility: hidden
  absolute: top 0
</style>
