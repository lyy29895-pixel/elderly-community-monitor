<template>
  <div class="person-page">
    <el-card class="person-page__card" header="个人信息">
      <el-form ref="form" :model="form" label-width="90px" class="person-page__form">
        <el-form-item label="用户名">
          <el-input :value="form.username ?? '—'" disabled/>
        </el-form-item>
        <el-form-item label="角色">
          <el-tag>{{ roleLabel(form.role) }}</el-tag>
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" placeholder="手机号"/>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.password" show-password placeholder="不修改请留空"/>
        </el-form-item>
      </el-form>
      <div class="person-page__actions">
        <el-button type="primary" class="person-page__save" @click="save">保存</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import request from "@/utils/request";

export default {
  name: "Person",
  data() {
    return {
      form: {
        username: '',
        role: '',
        phone: '',
        password: ''
      }
    }
  },
  created() {
    this.load()
  },
  methods: {
    roleLabel(role) {
      const r = String(role || '').toLowerCase()
      const map = {
        community: '社区工作人员',
        elder: '老人端',
        child: '子女端',
        admin: '管理员'
      }
      return map[r] || role || '—'
    },
    load() {
      request.get("/api/profile").then(res => {
        if (res.code === '200' && res.data) {
          this.form = { ...res.data, password: '' }
        }
      }).catch(() => this.$message.error('加载个人信息失败'))
    },
    save() {
      const payload = { ...this.form }
      if (!payload.password) delete payload.password
      Object.keys(payload).forEach(key => {
        if (payload[key] === '') payload[key] = null
      })

      request.put("/api/profile", payload).then(res => {
        if (res.code === '200') {
          this.$message.success("保存成功")
          const user = JSON.parse(sessionStorage.getItem('user') || '{}')
          // 只更新手机号，移除无效字段
          Object.assign(user, { phone: this.form.phone })
          sessionStorage.setItem('user', JSON.stringify(user))
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      }).catch(() => this.$message.error('网络请求失败'))
    }
  }
}
</script>

<style scoped>
.person-page {
  padding: 20px;
  min-height: calc(100vh - var(--yi-header-height, 62px));
  box-sizing: border-box;
  background: var(--yi-page-gradient, linear-gradient(165deg, #f7fbfe 0%, #eef5fc 48%, #e3eef9 100%));
}

.person-page__card {
  max-width: 520px;
  border-radius: 12px;
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.28));
  box-shadow: 0 4px 18px var(--yi-card-shadow, rgba(65, 110, 150, 0.08));
}

.person-page__card :deep(.el-card__header) {
  font-weight: 600;
  color: var(--yi-brand, #365062);
  background: var(--yi-panel-header-bg, linear-gradient(180deg, #f9fcfe 0%, #eef4fa 100%));
  border-bottom: 1px solid var(--yi-panel-header-border, rgba(145, 185, 215, 0.38));
}

.person-page__form {
  padding: 12px 8px;
}

.person-page__actions {
  text-align: center;
  margin-top: 10px;
  padding-bottom: 4px;
}

.person-page__save {
  width: 120px;
}
</style>
