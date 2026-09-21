<template>
  <el-menu
      class="app-aside"
      :default-active="$route.path"
      router
  >
    <template v-if="String(user.role || '').toLowerCase() === 'community'">
      <el-menu-item index="/elderly-mgmt">
        <span class="app-aside__label">老人监测</span>
      </el-menu-item>
      <el-menu-item index="/alerts">
        <span class="app-aside__label">事件处理</span>
      </el-menu-item>
      <el-menu-item index="/elderly-archive">
        <span class="app-aside__label">老人档案</span>
      </el-menu-item>
      <el-menu-item index="/announcements">
        <span class="app-aside__label">公告发布</span>
      </el-menu-item>
    </template>

    <el-menu-item v-if="String(user.role || '').toLowerCase() !== 'community'" index="/announcements">
      <span class="app-aside__label">公告</span>
    </el-menu-item>
  </el-menu>
</template>

<script>
export default {
  data() {
    return {
      user: {}
    }
  },
  watch: {
    $route() {
      this.refreshUser()
    }
  },
  created() {
    this.refreshUser()
  },
  methods: {
    refreshUser() {
      this.user = JSON.parse(sessionStorage.getItem('user') || '{}')
    }
  }
}
</script>

<style scoped>
.app-aside {
  width: 220px;
  height: 100%;
  border-right: none !important;
  padding: 10px 0 20px;
  box-sizing: border-box;
  overflow-y: auto;
  background: var(--yi-aside-gradient, linear-gradient(180deg, #f7fbfe 0%, #eef5fc 52%, #f9fcfe 100%)) !important;
  box-shadow: inset -1px 0 0 var(--yi-brand-softer, rgba(130, 170, 205, 0.28));
}

.app-aside__label {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

/* Element Plus 菜单项：圆角与间距 */
.app-aside :deep(.el-menu-item) {
  height: 44px !important;
  line-height: 44px !important;
  margin: 3px 10px;
  padding: 0 14px !important;
  border-radius: 10px;
  border: 1px solid transparent;
  color: var(--yi-text-body, #415a6e) !important;
  transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.app-aside :deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.85) !important;
  border-color: var(--yi-card-border, rgba(130, 170, 205, 0.28));
}

.app-aside :deep(.el-menu-item.is-active) {
  background: var(--yi-brand-soft, rgba(54, 80, 98, 0.12)) !important;
  color: var(--yi-brand, #365062) !important;
  font-weight: 600;
  border-color: rgba(79, 148, 196, 0.42);
  box-shadow: 0 2px 10px rgba(79, 148, 196, 0.14);
}

.app-aside :deep(.el-menu-item.is-active .app-aside__label) {
  color: var(--yi-brand, #365062);
}
</style>
