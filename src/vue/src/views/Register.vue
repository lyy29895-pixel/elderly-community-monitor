<template>
  <div class="auth-page">
    <div class="auth-page__panel">
      <h1 class="auth-page__title">颐安云护</h1>
      <el-form
          ref="form"
          class="auth-form"
          :model="form"
          label-position="top"
          size="normal"
          :rules="rules"
      >
        <el-form-item label="用户名" prop="username">
          <el-input :prefix-icon="Avatar" v-model="form.username" placeholder="请输入用户名"/>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input :prefix-icon="Lock" v-model="form.password" show-password placeholder="请输入密码"/>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input :prefix-icon="Lock" v-model="form.confirm" show-password placeholder="请再次输入密码"/>
        </el-form-item>
        <el-form-item>
          <el-button style="width:100%" type="primary" @click="register">注 册</el-button>
        </el-form-item>
        <el-form-item>
          <el-button style="width:100%" link type="primary" @click="$router.push('/login')">返回登录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { Avatar, Lock } from "@element-plus/icons-vue";
import request from '@/utils/request'

export default {
  name: "Register",
  components: {
    Avatar,
    Lock
  },
  data() {
    // 密码一致性校验
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.form.password) {
        return callback(new Error('两次密码输入不一致'))
      }
      callback()
    }

    return {
      form: {
        username: '',
        password: '',
        confirm: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ],
        confirm: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: validateConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    register() {
      this.$refs.form.validate((valid) => {
        if (!valid) return

        const payload = {
          username: this.form.username.trim(),
          password: this.form.password,
          role: 'community'
        }

        request.post("/api/auth/register", payload)
            .then(res => {
              if (res.code === '200') {
                this.$message.success("注册成功，请登录")
                this.$router.push("/login")
              } else {
                this.$message.error(res.msg || '注册失败')
              }
            })
            .catch(() => {
              this.$message.error('网络请求失败，请稍后重试')
            })
      })
    }
  }
}
</script>

<style src="@/assets/css/auth-page.css"></style>
