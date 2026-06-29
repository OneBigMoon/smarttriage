<template>
  <logout :show.sync="logout"></logout>
  <ul class="nav navbar-nav navbar-right">
    <li>
      <a href="javascript:;" @click="openList">
        <i class="fa fa-volume-up fa-fw"></i>
      </a>
    </li>
    <li class="dropdown">
      <a href="javascript:;" class="dropdown-toggle">
        <i class="fa fa-fw fa-user"></i> {{name}} <span class="caret"></span>
      </a>
      <ul class="dropdown-menu">
        <li v-for="i in usermenu">
          <a v-link="i.link">
            <i class="fa fa-fw fa-{{i.icon}}"></i> {{i.name}}
          </a>
        </li>
        <li>
          <a href="javascript:;" @click="refresh">
            <i class="fa fa-fw fa-refresh"></i> {{$t('misc.check')}}
          </a>
        </li>
        <li>
          <a href="javascript:;" @click="setting">
            <i class="fa fa-fw fa-cog"></i> {{$t('misc.set')}}
          </a>
        </li>
        <li>
          <a href="javascript:;" @click="logout = true">
            <i class="fa fa-fw fa-power-off"></i> {{$t('misc.quit')}}
          </a>
        </li>
      </ul>
    </li>
  </ul>
</template>

<script>
const Logout = require('./logout');
const Monitor = require('../../services/monitor');
const Auth = require('../../services/auth');

const {
  alert
} = require('../../utils');

exports.components = {
  Logout
};

exports.props = [
  'name'
];

exports.data = () => {
  return {
    logout: false,
    custinfo: {
      show: false,
      type: '',
      value: {}
    }
  };
};

exports.methods = {
  login() {
    const user = Auth.user();
    Monitor.login((msg) => {
      if (!user || !user.name || user.name != msg.receiver) {
        return;
      }

      const _this = this;
      if (msg.type == 'ticket') {
        // vip取票 身份证customer.idcard, 银行卡customer.idcard,票号cutomer.ticketnumber
        alert(msg.content, 'success', 10 * 1000, () => {
          _this.custinfo.show = true;
          _this.custinfo.value = {
            n: msg.customer.name,
            can: msg.customer.idcard,
            csno: msg.customer.bankcard,
            ticket: msg.customer.ticketnumber
          };
        });
      } else {
        alert(msg.content, 'success', 10 * 1000);
      }
    });
  },
  openList() {
  },
  refresh() {
  },
  setting() {
  }
};

exports.events = {
  'app.refresh'() {
    this.login();
  }
};
</script>
