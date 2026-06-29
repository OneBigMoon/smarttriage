<template>
  <div></div>
</template>

<script>
const Auth = require('../../services/auth');
exports.ready = function() {
  const query = this.$route.query;
  const salt = query.salt || '';
  const md5token = query.md5token || '';
  const dock = query.dock || '';
  Auth.fix((redirectUrl) => {
    const state = {
      salt,
      md5token
    };
    const stateStr = encodeURIComponent(JSON.stringify(state));
    window.location.href = `${redirectUrl}&state=${stateStr}&dock=${dock}`;
  });
};
</script>
