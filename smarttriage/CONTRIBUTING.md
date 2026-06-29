# 项目格式规范

## 源代码格式

插件 [EditorConfig](http://editorconfig.org/) 配置文件 `.editorconfig`

* 缩进 2 个空格
* 文件末尾保留一个空行

## JavaScript 代码规范

[Airbnb JavaScript Style Guide](https://github.com/yuche/javascript)

## 命名规范

### 资源

小写加横杠分隔

* 文件名、文件夹名

```
foo-bar/foo-bar.js
```

* URL

```
http://foobar.com/foo-bar/1
```

* HTML 标签名、属性、ID、Class

```html
<foo-bar id="foo-bar" class="foo foo-bar" foo-bar="text"></foo-bar>
```

### 变量

驼峰命名

* 类名、数据模型名

大写开头驼峰命名

```js
const FooBar = require('foo-bar.js');

const fooBar = new FooBar();
```

* 变量名、方法名

小写开头驼峰命名

```js
function fooBar1() {
}

const fooBar2 = {
	fooBar3: 0,
    fooBar4() {
    	const fooBar5 = 1;
    }
};
```

### 数据结构

小写加下划线

* 数据模型字段

```js
{
	foo_bar: { type: String, default: '' }
}
```

* API 变量字段

```
http://foobar.com/foo-bar/:foo_bar_id
```
