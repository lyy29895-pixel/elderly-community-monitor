<script>
// 导入ElementPlus图标、axios请求工具
import { Avatar, Lock } from "@element-plus/icons-vue";
import request from '@/utils/request'

export default {
  name: "Login",
  data() {
    return {
      form: {
        username: '', // 用户名
        password: ''  // 密码
      },
      rules: { // 表单校验规则
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      Avatar,
      Lock
    }
  },
  methods: {
    /**
     * 登录提交：表单校验 → 调用登录接口 → 缓存token与用户信息 → 根据角色跳转页面
     */
    login() {
      this.$refs['form'].validate((valid) => {
        if (!valid) return
        request.post("/api/auth/login", this.form).then(res => {
          if (res.code === "200" && res.data && res.data.user) {
            sessionStorage.setItem("token", res.data.token) // 保存登录令牌
            sessionStorage.setItem("user", JSON.stringify(res.data.user)) // 保存用户信息
            this.$message.success("登录成功")
            setTimeout(() => {
              const role = String(res.data.user.role || '').toLowerCase()
              // 社区管理员跳老人管理页，其余角色跳个人中心
              if (role === 'community') this.$router.push("/elderly-mgmt")
              else this.$router.push("/person")
            }, 300)
          } else {
            this.$message.error(res.msg || "登录失败")
          }
        }).catch((err) => {
          // 捕获网络、后端异常
          const msg = err.response && err.response.data && err.response.data.msg
          this.$message.error(msg || "请求失败，请检查网络或后端服务")
        })
      })
    }
  }
}
</script>

<template>
  <!-- 登录页面容器 -->
  <div class="auth-page">
    <!-- 登录面板 -->
    <div class="auth-page__panel">
      <h1 class="auth-page__title">颐安云护</h1>
      <!-- 登录表单 -->
      <el-form ref="form" class="auth-form" :model="form" label-position="top" size="normal" :rules="rules">
        <el-form-item label="用户名" prop="username">
          <el-input :prefix-icon="Avatar" v-model="form.username" placeholder="请输入用户名"/>
        </el-form-item>
        <!-- 密码输入框，开启密码显隐切换 -->
        <el-form-item label="密码" prop="password">
          <el-input :prefix-icon="Lock" v-model="form.password" show-password placeholder="请输入密码"/>
        </el-form-item>
        <el-form-item>
          <el-button style="width:100%" type="primary" @click="login">登 录</el-button>
        </el-form-item>
        <!-- 注册页面跳转入口 -->
        <el-form-item>
          <div class="login-register-line">
            <span class="login-register-line__hint">没有账户？</span>
            <router-link class="login-register-line__link" to="/register">立即注册</router-link>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<!-- 局部样式：登录注册跳转行 -->
<style scoped>
.login-register-line {
  width: 100%;
  text-align: center;
  font-size: 14px;
  line-height: 1.5;
}

.login-register-line__hint {
  color: var(--yi-text-muted, #6d8aa0);
}

.login-register-line__link {
  margin-left: 2px;
  color: var(--el-color-primary);
  font-weight: 500;
  text-decoration: none;
}

.login-register-line__link:hover {
  text-decoration: underline;
}
</style>
<!-- 引入登录页面公共样式 -->
<style src="@/assets/css/auth-page.css"></style>




