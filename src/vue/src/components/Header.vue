<script>
import { ArrowDown } from '@element-plus/icons-vue'
import brandLogo from '@/assets/images/yi-logo.png'

export default {
  name: 'HeaderBar',
  components: { ArrowDown },
  data() {
    return {
      displayName: '用户',
      brandLogo
    }
  },
  watch: {
    $route() {
      this.loadName()
    }
  },
  created() {
    this.loadName()
  },
  methods: {
    loadName() {
      try {
        const u = JSON.parse(sessionStorage.getItem('user') || '{}')
        this.displayName = u.username || '用户'
      } catch (e) {
        this.displayName = '用户'
      }
    },
    async logout() {
      const token = sessionStorage.getItem('token')
      if (token) {
        try {
          const request = (await import('@/utils/request')).default
          await request.post('/api/auth/logout')
        } catch (e) { /* ignore */ }
      }
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user')
      this.$router.push('/login')
    }
  }
}
</script>

<template>
  <header class="header-bar">
    <div class="header-bar__brand">
      <img :src="brandLogo" class="header-bar__logo" width="48" height="48" alt="颐安云护"/>
      <div class="header-bar__brand-text">
        <span class="header-bar__brand-name">颐安云护</span><span class="header-bar__brand-suffix">社区端</span>
      </div>
    </div>
    <div class="header-bar__spacer"/>
    <div class="header-bar__user">
      <el-dropdown popper-class="header-bar__dropdown" trigger="click">
        <span class="header-bar__trigger">
          <span class="header-bar__trigger-name">{{ displayName }}</span>
          <el-icon class="header-bar__trigger-icon"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="$router.push('/person')">个人信息</el-dropdown-item>
            <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped>
.header-bar {
  height: var(--yi-header-height, 62px);
  padding: 0 20px 0 10px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  background: linear-gradient(180deg, #ffffff 0%, rgba(255, 255, 255, 0.94) 100%);
  border-bottom: 1px solid var(--yi-brand-softer, rgba(26, 43, 74, 0.08));
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.85) inset, 0 2px 10px -2px var(--yi-brand-softer, rgba(26, 43, 74, 0.08));
  z-index: 20;
}

.header-bar__brand {
  min-width: 300px;
  padding-left: 6px;
  gap: 14px;
  display: flex;
  align-items: center;
  align-self: stretch;
}

.header-bar__logo {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  object-fit: contain;
  display: block;
  border-radius: 50%;
  box-shadow: 0 1px 4px rgba(65, 110, 150, 0.12);
}

.header-bar__brand-text {
  display: flex;
  align-items: baseline;
  flex-wrap: nowrap;
  white-space: nowrap;
  line-height: 1.2;
}

.header-bar__brand-name {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--yi-brand, #365062);
}

.header-bar__brand-suffix {
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--yi-text-muted, #6d8aa0);
  margin-left: 2px;
}

.header-bar__spacer {
  flex: 1;
}

.header-bar__user {
  padding-right: 4px;
  display: flex;
  align-items: center;
  align-self: stretch;
}

.header-bar__trigger {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  line-height: 1;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid var(--yi-brand-softer, rgba(26, 43, 74, 0.12));
  color: var(--yi-text-body, #415a6e);
  font-size: 14px;
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease;
}

.header-bar__trigger:hover {
  background: #fff;
  border-color: var(--el-color-primary);
  color: var(--yi-brand, #365062);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.header-bar__trigger-name {
  max-width: 168px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-bar__trigger-icon {
  font-size: 13px;
  color: var(--yi-text-muted, #6d8aa0);
}
</style>

<style>
/* 下拉层挂载在 body，需非 scoped */
.header-bar__dropdown.el-popper {
  border-radius: 12px;
  box-shadow: 0 8px 24px var(--yi-brand-softer, rgba(26, 43, 74, 0.12));
  border: 1px solid var(--yi-brand-softer, rgba(26, 43, 74, 0.12));
}

.header-bar__dropdown .el-dropdown-menu__item {
  font-size: 14px;
  padding: 10px 18px;
  line-height: 1.45;
}
</style>
