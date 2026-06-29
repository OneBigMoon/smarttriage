<template>
  <ul class="tree-menu">
    <li v-for="item in data">
      <div class="node" :class="selectNodeId == item._id ? 'selected' : ''" @click="select(item)">
        <span @click="toggle(item, $index)">
          <i :class="['fa', folderIconList[$index]]"></i>
          {{ item.name }}
        </span>
        <i class="icon fa fa-plus" style="margin-left:30px;padding:0 5px;"
         v-if="isEdit&&item._id!=-1" @click="$dispatch('node-add', item)"></i>
        <i class="icon fa fa-trash" style="margin-left:20px;padding:0 5px;"
         v-if="isEdit&&item._id!=-1&&item._id!=0" @click="$dispatch('node-remove', item)"></i>
      </div>
      <tree-menu v-if="scope[$index]" :data="item.children" :is-edit="isEdit"
       :select-able="selectAble" :select-node-id="selectNodeId"></tree-menu>
    </li>
  </ul>
</template>

<script>
exports.name = 'treeMenu'; // 必须命名 自己使用

exports.props = {
  data: Array,
  name: String,
  isEdit: { type: Boolean, default: false },
  selectAble: { type: Boolean, default: false },
  selectNodeId: { type: Number, default: null }
};

exports.data = () => {
  return {
    folderIconList: [],
    scope: []
  };
};

exports.ready = function() {
  this.data.forEach((item, index) => {
    if (item.children && item.children.length) {
      this.folderIconList.$set(index, 'fa-folder');
    } else {
      this.folderIconList.$set(index, 'fa-folder-o');
    }
  }, this);
};

exports.watch = {
  data() {
    this.data.forEach((item, index) => {
      if (item.children && item.children.length) {
        this.folderIconList.$set(index, 'fa-folder');
      } else {
        this.folderIconList.$set(index, 'fa-folder-o');
      }
    }, this);
  }
};

exports.methods = {
  doTask(index) {
    this.scope.$set(index, !this.scope[index]);
    this.folderIconList.$set(index, this.scope[index] ? 'fa-folder-open' : 'fa-folder');
  },
  toggle(item, index) {
    if (this.isEdit) {
      this.$dispatch('node-edit', item);
    }
    if (item.children && item.children.length) {
      this.doTask(index);
    }
  },
  select(item) {
    if (this.selectAble) {
      this.$dispatch('node-select', item._id, this);
    }
  }
};

exports.events = {
  'node-select'(id, thiz) {
    if (thiz != this) {
      this.$dispatch('node-select', id, this);
    }
  }
};

</script>

<style scoped>
  .tree-menu {
    list-style: none;
  }
  .tree-menu li {
    line-height: 2;
  }
  .tree-menu li span {
    cursor: default;
  }
  .icon {
    display: none;
  }
  .node:hover .icon {
    display: inline-block;
  }
  .node.selected {
    background-color: #8bcab796;
  }
</style>
