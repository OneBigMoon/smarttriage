<template>
  <div
    class="slider"
    :id="id"
    :class="{ready: ready, touch: touch.s.x && touch.e.x}"
    @touchstart="start"
    @touchmove="move"
    @touchend="end"
    @touchcancel="end">
    <div class="slider-viewport">
      <div
        class="slider-wrap"
        :style="{
          width: (width * slides.length) + 'px',
          transform: 'translate3d(' + (x * -1) + 'px, 0, 0)'
        }">
          <slot></slot>
      </div>
    </div>
    <div v-if="slides.length">
      <div class="slider-plain" v-if="plain != null">
        {{index + 1}}/{{slides.length}}
      </div>
      <div class="slider-pagination" v-if="pagination != null">
        <span
          class="slider-pagination-bullet"
          v-for="s in slides"
          :class="{active: index == $index}"
          @click="go($index)">
        </span>
      </div>
      <div class="slider-pager" v-if="pager != null">
        <div
          class="slider-pager-prev"
          v-if="index > 0 || loop != null"
          @click="prev"
          transition>
          <i class="fa fa-angle-left"></i>
        </div>
        <div
          class="slider-pager-next"
          v-if="index < slides.length - 1 || loop != null"
          @click="next"
          transition>
          <i class="fa fa-angle-right"></i>
        </div>
      </div>
      <div class="slider-scrollbar" :style="styleObject" v-if="scrollbar != null">
        <div
          class="slider-scrollbar-thumb"
          :style="barStyleObject">
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const SLIDE_CLS = 'slider-slide';

const win = window;

const {
  query,
  toArray,
  on,
  off,
  addClass,
  getComputedSize
} = require('../utils');

function getTouchPosition(e, direction) {
  let touch = e.touches || e.targetTouches;
  touch = touch && touch[0] || e;
  return touch[`page${direction}`] || touch[`client${direction}`];
}

// function getMaxComputedSize(els, attr) {
//   const attrs = els.map(function(el) {
//     return getComputedSize(el, attr);
//   });
//   return Math.max.apply(attrs, attrs);
// }

function getAngle(xStart, yStart, xEnd, yEnd) {
  const deltaX = xEnd - xStart;
  const deltaY = yEnd - yStart;
  return 360 * Math.atan(deltaY / deltaX) / (2 * Math.PI);
}

exports.props = [
  'plain', // 当前页数显示
  'pagination', // 分页显示
  'pager',      // 翻页按钮
  'scrollbar',  // 滚动条
  'barwidth',   // 滚动条长度
  'index',      // 页码
  'watcher',    // watch 条件，触发 process
  'loop',       // 循环翻页
  'interval',   // 自动翻页
  'gesture'     // 手势操作
];

exports.data = () => ({
  id: `slider-${~~(Math.random() * 10000)}`,
  height: 0,  // 高度
  width: 0,   // 宽度
  slides: [], // 幻灯片
  x: 0,       // 容器偏移 x
  ready: 0,   // 初始化状态
  touch: {    // 触摸数据
    x: 0,     // 触摸起始容器偏移 x
    s: {      // 触摸起始坐标
      x: 0,
      y: 0
    },
    e: {      // 触摸结束坐标
      x: 0
    }
  },
  timer: 0
});

exports.computed = {
  styleObject() {
    const {
      barwidth,
      slides
    } = this;
    if (!barwidth || !slides.length) {
      return {};
    }

    const width = `${barwidth * slides.length}px`;

    return { width };
  },
  barStyleObject() {
    const {
      barwidth,
      width,
      x,
      slides
    } = this;

    if (!slides.length) {
      return {};
    }

    const slideWidth = width / slides.length;
    if (!barwidth || barwidth > slideWidth) {
      return {
        width: `${slideWidth}px`,
        transform: `translate3d(${x / slides.length}px, 0, 0)`
      };
    }

    const multiple = slideWidth / barwidth;
    return {
      width: `${barwidth}px`,
      transform: `translate3d(${x / slides.length / multiple}px, 0, 0)`
    };
  }
};

exports.methods = {
  resize() {
    const _this = this;
    _this.ready = 0;
    _this.process();
  },
  /**
   * 计算尺寸、初始化
   */
  process() {
    const _this = this;
    const el = _this.$el;
    const id = _this.id;
    let index = 0;
    const slides = toArray(query(`#${id} .slider-wrap`).childNodes).filter((node) => {
      if (node.tagName) {
        addClass(node, SLIDE_CLS);
        addClass(node, `${SLIDE_CLS}-${index++}`);
        return true;
      }
      return false;
    });
    const width = _this.width = getComputedSize(el, 'width');
    _this.slides = slides.map((slide) => {
      slide.style.width = `${width}px`;
      return {
        active: false
      };
    });
    // _this.height = getMaxComputedSize(slides, 'height');

    if (!_this.ready) {
      _this.ready = 1;

      // 初始化 index
      if (_this.index == null) {
        _this.index = 0;
      } else {
        _this.x = _this.index * _this.width;
      }
    }
  },
  /**
   * 滑到指定页码
   */
  go(index) {
    const _this = this;
    _this.index = index;
  },
  /**
   * 上一页
   */
  prev() {
    this.index--;
  },
  /**
   * 下一页
   */
  next() {
    this.index++;
  },
  /**
   * 触摸事件开始
   */
  start(e) {
    const _this = this;
    const touch = _this.touch;
    const start = touch.s;
    if (_this.gesture) {
      start.x = getTouchPosition(e, 'X');
      start.y = getTouchPosition(e, 'Y');
      touch.x = _this.x;
    }
  },
  /**
   * 触摸事件移动
   */
  move(e) {
    const _this = this;
    const touch = _this.touch;
    const start = touch.s;
    const end = touch.e;
    const xStart = start.x;
    const xEnd = getTouchPosition(e, 'X');
    const yEnd = getTouchPosition(e, 'Y');

    const angle = Math.abs(~~getAngle(xStart, start.y, xEnd, yEnd));

    // 消除抖动，角度小于 20 度，确认在左右滑动
    if (_this.gesture && xStart > 0 && xEnd > 0 && Math.abs(xStart - xEnd) > 6 && angle < 20) {
      end.x = xEnd;
      const _x = touch.x + xStart - xEnd;
      if (_this.loop != null || (_x >= 0 && _x <= ((_this.slides.length - 1) * _this.width))) {
        _this.x = _x;
      }

      // 修复微信不响应 touchmove
      e.preventDefault();
    }
  },
  /**
   * 触摸事件结束
   */
  end() {
    const _this = this;
    const touch = _this.touch;
    const length = _this.slides.length;
    const start = touch.s;
    const end = touch.e;
    const xStart = start.x;
    const xEnd = end.x;
    let index = _this.index;
    let delta;
    if (_this.gesture && xStart > 0 && xEnd > 0) {
      delta = (xEnd - xStart) / _this.width;

      // 允许拖动超过 0.3 时即认为滑动到上一页或下一页
      if (delta > 0.3) {
        index--;
      } else if (delta < -0.3) {
        index++;
      }

      // 处理 index 在第一页或最后一页的情况
      if (index < 0) {
        index = 0;
      } else if (index >= length) {
        index = length - 1;
      }

      _this.index = index;
      _this.x = index * _this.width; // 自动滑动到每一页的合适位置
      start.x = end.x = 0;
    }
  }
};

exports.watch = {
  index(index) {
    const _this = this;
    if (index != null) {
      if (index < 0) {
        _this.index = _this.loop != null ? _this.slides.length - 1 : 0;
        return;
      }
      if (index >= _this.slides.length) {
        _this.index = _this.loop != null ? 0 : _this.slides.length - 1;
        return;
      }
      _this.x = index * _this.width;
    }
  },
  watcher: 'process'
};

exports.ready = function() {
  const _this = this;
  _this.process();

  // 窗口大小变化时重新计算大小
  on(win, 'resize', _this.resize);

  if (_this.gesture == null || _this.gesture === '') {
    _this.gesture = true;
  }

  if (_this.interval) {
    _this.timer = setInterval(() => {
      _this.next();
    }, _this.interval);
  }
};

exports.beforeDestroy = function() {
  const _this = this;
  off(win, 'resize', _this.resize);
  if (_this.timer) {
    clearInterval(_this.timer);
  }
};
</script>

<style lang="stylus">
@import "../styles/variables"
@import "../styles/mixins"

.slider
  $slider-bullet-color = #808080
  $slider-bullet-size = 30px
  $slider-pager-shadow = 0 0 2px $black

  position: relative
  width: 100%
  user-select: none
  -webkit-user-drag: none
  -webkit-tap-highlight-color: transparent

  &-viewport
    width: 100%
    overflow: hidden

  &-wrap
    clearfix()
    width: 100%
    transition: transform .3s

  &-slide
    width: 100%

  &-plain
    absolute: bottom 10px left 0 right 0
    text-align: center

  &-pagination
    absolute: bottom 0 left 0 right 0
    text-align: center

    &-bullet
      display: inline-block
      width: ($slider-bullet-size / 2) + 10
      color: $white
      font-size: $slider-bullet-size
      line-height: 1
      text-align: center
      text-shadow: $slider-pager-shadow
      cursor: pointer
      transition: color .3s
      &:before
        content: "\2022"
      &.active
        color: $slider-bullet-color

  &-pager
    &-prev
    &-next
      absolute: top 0 bottom $slider-bullet-size
      width: 50px
      text-shadow: $slider-pager-shadow
      cursor: pointer

      .fa
        absolute: top 50%
        width: 100%
        margin: ($font-size-h2 / -2) 0 0
        color: $white
        font-size: $font-size-h2
        line-height: 1
        text-align: center

      &.v-transition
        opacity: 1
        transition: opacity .3s
      &.v-enter
      &.v-leave
        opacity: 0

    &-prev
      left: 0
    &-next
      right: 0

  &-scrollbar
    absolute: bottom 0 left 0 right 0
    height: 3px
    overflow: hidden
    margin: 0 auto

    &-thumb
      absolute: bottom 0 left 0
      height: 3px
      background: $brand-primary
      transition: transform .3s

  &.ready
    .slider-slide
      float: left

  &.touch
    .slider
      &-wrap
      &-scrollbar-thumb
        transition: none
</style>
