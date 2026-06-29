# 智慧银行

## 项目说明

* 项目目录结构

```
+-app                         # 前端代码
| +-assets                    # 静态资源文件
| +-components                # 界面组件
| +-services                  # 服务
| +-styles                    # 样式
| +-utils                     # 工具方法
| +-views                     # 路由视图
| +-app.vue                   # 总体视图框架
| +-config.js                 # 前端配置
| +-index.html                # 入口 HTML
| +-index.js                  # 入口 JS
| +-route.js                  # 前端路由
|
+-build                       # 打包构建配置
+-config                      # 项目配置
| +-env                       # 不同环境的不同配置(production/development/test ...)
|
+-dist                        # 构建打包目录
+-init                        # 初始化
+-lib                         # 后端代码
| +-config                    # 模块配置(express/log4js ...)
| +-controllers               # 控制器，主要逻辑
| +-models                    # 数据模型
| +-utils                     # 工具集合
| +-route.js                  # 后端路由
|
+-logs                        # 日志
+-node_modules                # node 模块
+-gulpfile.js                 # gulp 入口
+-package.json                # 后端依赖库描述文件
+-server.js                   # 服务启动入口
```

**`node_modules` 目录下的包要通过 `npm` 下载**

## 环境依赖

* [Node.js](http://nodejs.org/) - [下载和安装](http://nodejs.org/download)
* 数据库 [MongoDB](http://www.mongodb.org/) - [下载和安装](http://www.mongodb.org/downloads)

## 使用方法

### 一键启动（推荐）

#### 方式一：docker-compose（推荐，最稳定）

```bash
cp .env.example .env
./start-stack.sh
```

服务启动后打开：

- 管理端：http://127.0.0.1:7016/
- 健康检查（仅用于容器）：http://127.0.0.1:7016

默认管理员测试账号（仅示例）：`root / root1234`，请在生产环境替换为安全密码。

## 重要说明（重构方向）

你提的 py+React 重构不是这个仓库的默认演进主线：

- 旧版：本仓库（Node + Koa + Vue）
- 重构目标：
  - 后端：`/Users/x/Documents/triage-server`（Django + DRF + Channels）
  - 管理端：`/Users/x/Documents/triage-admin`（React + Vite）
  - 安卓端：`/Users/x/Documents/triage-android`（待补齐）

如果你现在要“马上可验证管理页”，建议先用重构版打通体验，再决定是否把该旧仓库作为历史兼容链路保留。

### 根据 `package.json` 下载相应包:

    cd ${项目目录}
    npm install

### 运行平台 - 开发模式

    npm start

### 打发行包

    npm run build

项目目录下的 `dist/smarttriage-*.tar.gz` 为发行包

## 部署

CentOS 版本：CentOS 7.0

### gcc, g++, openssl, python(要求2.6或2.7版本)

    yum install gcc gcc-c++ openssl-devel

### Node.js

    wget https://npm.taobao.org/mirrors/node/latest/node-v5.6.0-linux-x64.tar.gz
    tar zxvf node-v5.6.0-linux-x64.tar.gz
    mv node-v5.6.0-linux-x64 ${node安装目录}

修改 `/etc/profile`， 添加

    export NODE_HOME=${node安装目录}
    export PATH=$NODE_HOME/bin:$PATH

执行

	source /etc/profile

设置 npm 国内镜像

    npm config set registry https://registry.npm.taobao.org

### PM2

    npm install pm2 -g

**注意**：详细使用可参考 [PM2](https://github.com/Unitech/pm2)

### 部署发行包

建立 `smarttriage` 目录，并进入目录。

复制发行包 `smarttriage-*.tar.gz` 到 `smarttriage`。

    tar zxvf smarttriage-*.tar.gz

### MongoDB

    wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-3.2.3.tgz
    tar zxvf mongodb-linux-x86_64-3.2.1.tgz
    mv mongodb-linux-x86_64-3.0.3.tgz ${mongodb安装目录}
    cd ${mongodb安装目录}
    mkdir -p data
    export MONGO_HOME=${mongodb安装目录}
    vi mongo.conf

内容为

    dbpath = ${mongodb安装目录}/data
    logpath = ${mongodb安装目录}/mongo.log
    pidfilepath = ${mongodb安装目录}/mongo.pid
    directoryperdb = true
    logappend = true
    fork = true
    rest = true
    httpinterface = true
    profile = 1
    storageEngine = wiredTiger
    oplogSize = 1024
    # auth = true

启动 MongoDB

    ${mongodb安装目录}/bin/mongod -f ${mongodb安装目录}/mongo.conf

添加 `admin` 用户、设置权限：

    参考 http://ibruce.info/2015/03/03/mongodb3-auth/

    use admin;
    db.createUser({
        user: "admin",
        pwd: "${mongodb的admin密码}",
        roles: [ { role: "userAdminAnyDatabase", db: "admin" } ]
    });
    show users;

修改 `mongo.conf`，打开 `auth = true`，并使用 `kill -2 pid` 或 `db.shutdownServer()` 重启 MongoDB。

此时 `admin` 用户只拥有用户管理的权限。

继续添加 `smarttriage` 用户、设置权限：

    use admin;
    db.auth("admin", "${mongodb的admin密码}");

    use smarttriage;
    db.createUser({
       user: "smarttriage",
       pwd: "${mongodb的smarttriage密码}",
       roles: [
          { role: "dbOwner", db: "smarttriage" }
       ]
    });
    show users;

修改项目 `smarttriage/config/production.js` 设置 MongoDB 的连接参数

### 初始化数据

    npm run init

### 设置服务语言环境

    修改项目 `smarttriage/package.json` 设置参数language的值
    zh-CN 中文
    en 英文

### 启动服务

    npm run pm2-start

或

    pm2 start -n smarttriage server.js

### 设置服务自启动

    pm2 save

### 停止服务

    npm run pm2-stop

或

    pm2 delete smarttriage
