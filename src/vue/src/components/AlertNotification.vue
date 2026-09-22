<template>
  <el-dialog
    v-model="visible"
    class="alert-notification"
    :class="{ 'alert-notification--emergency': isEmergency }"
    width="420px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="true"
    center
  >
    <template #header>
      <div class="alert-notification__header">
        <el-icon class="alert-notification__icon" :size="isEmergency ? 32 : 24">
          <Bell />
        </el-icon>
        <span class="alert-notification__title">异常事件提醒</span>
      </div>
    </template>

    <div v-if="currentAlert" class="alert-notification__content">
      <div class="alert-notification__message">
        <span class="alert-notification__elder">{{ formatElderName(currentAlert.elderName) }}</span>
        <span class="alert-notification__text">{{ alertText }}</span>
        <span class="alert-notification__event">{{ getAlertType(currentAlert) }}</span>
      </div>

      <div v-if="currentAlert.description" class="alert-notification__desc">
        {{ formatDescription(currentAlert.description) }}
      </div>

      <div v-if="pendingCount > 1" class="alert-notification__pending">
        还有 <span class="alert-notification__pending-count">{{ pendingCount - 1 }}</span> 条待处理
      </div>
    </div>

    <template #footer>
      <div class="alert-notification__footer">
        <el-button @click="handleLater">稍后处理</el-button>
        <el-button :type="isEmergency ? 'danger' : 'primary'" @click="handleView">前往处理</el-button>
      </div>
    </template>
  </el-dialog>

</template>

<script>
import { Bell } from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'AlertNotification',
  components: { Bell },
  data() {
    return {
      visible: false,
      pendingAlerts: [],
      knownIds: new Set(),
      maxKnownId: 0,
      pollTimer: null,
      isInitialized: false
    }
  },
  computed: {
    currentAlert() {
      return this.pendingAlerts.length > 0 ? this.pendingAlerts[0] : null
    },
    pendingCount() {
      return this.pendingAlerts.length
    },
    isEmergency() {
      if (!this.currentAlert) return false
      const type = this.currentAlert.eventType || this.currentAlert.type || ''
      return type === 'EMERGENCY'
    },
    alertText() {
      return this.isEmergency ? '触发' : '发生'
    }
  },
  mounted() {
    this.initKnownIds()
    this.startPolling()
  },
  beforeUnmount() {
    this.stopPolling()
  },
  methods: {
    initKnownIds() {
      const token = sessionStorage.getItem('token')
      if (!token) {
        this.isInitialized = true
        return
      }
      
      this.fetchAllEventIds(1)
    },
    
    fetchAllEventIds(pageNum) {
      request.get('/api/alerts/page', { params: { pageNum: pageNum, pageSize: 100 } })
        .then(res => {
          if (res.code === '200' && res.data) {
            const records = res.data.records || []
            records.forEach(alert => {
              this.knownIds.add(alert.id)
              if (alert.id > this.maxKnownId) {
                this.maxKnownId = alert.id
              }
            })
            
            const total = res.data.total || 0
            const pages = Math.ceil(total / 100)
            
            if (pageNum < pages) {
              this.fetchAllEventIds(pageNum + 1)
            } else {
              this.isInitialized = true
            }
          } else {
            this.isInitialized = true
          }
        })
        .catch(() => {
          this.isInitialized = true
        })
    },
    startPolling() {
      this.pollTimer = setInterval(() => {
        this.checkNewAlerts()
      }, 3000)
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    checkNewAlerts() {
      if (!this.isInitialized) {
        return
      }
      
      const token = sessionStorage.getItem('token')
      if (!token) {
        return
      }
      
      request.get('/api/alerts/page', { params: { pageNum: 1, pageSize: 20 } })
        .then(res => {
          if (res.code === '200' && res.data && res.data.records) {
            const records = res.data.records
            
            const newAlerts = records.filter(alert => {
              const isNew = alert.id > this.maxKnownId
              const isUnprocessed = alert.status === 'NEW' || alert.status === '未处理'
              return isNew && isUnprocessed
            })
            
            if (newAlerts.length > 0) {
              newAlerts.forEach(alert => {
                this.knownIds.add(alert.id)
                if (alert.id > this.maxKnownId) {
                  this.maxKnownId = alert.id
                }
                this.addAlert(alert)
              })
            }
          }
        })
        .catch(err => {
          console.warn('轮询事件失败:', err)
        })
    },
    shouldShowAlert(alert) {
      if (!alert) return false
      // 1. 优先用后端带的 category 字段（4 大类之一才弹）
      const category = alert.category
      if (category) {
        return category === 'FALL' || category === 'EMERGENCY'
          || category === 'SMOKE' || category === 'PRESSURE'
      }
      // 2. 没有 category 字段时，用 eventType/type 做兜底判断，和后端 resolveEventTypeCategory 规则一致
      const type = (alert.eventType || alert.type || '').toUpperCase()
      const raw = alert.eventType || alert.type || ''
      // 4 大类：摔倒
      if (type.includes('FALL') || raw.includes('摔倒') || raw.includes('跌倒')) return true
      // 4 大类：一键求助
      if (type === 'SOS' || type.includes('SOS_CALL') || type.includes('CALL_HELP')
        || type.includes('BUTTON_PRESS') || type === 'EMERGENCY'
        || raw.includes('一键求助') || raw.includes('紧急求助') || raw.includes('紧急按钮')) return true
      // 4 大类：烟雾
      if (type.includes('SMOKE') || type.includes('FIRE') || type.includes('GAS')
        || raw.includes('烟雾') || raw.includes('火灾') || raw.includes('燃气')) return true
      // 4 大类：情绪
      if (type.includes('PRESSURE') || type.includes('BED_PRESSURE') || type.includes('PRESSURE_WARN') || type.includes('PRESSURE_ALERT')
        || type.includes('EMOTION') || type.includes('DISTRESS') || type.includes('MOOD') || type.includes('EMOTIONAL')
        || raw.includes('压力') || raw.includes('床压') || raw.includes('情绪') || raw.includes('心情') || raw.includes('心理')) return true
      // 其他（心率/环境/离床/血压等提示级事件）：一律不弹
      return false
    },
    getAlertType(alert) {
      // 优先用后端已经转好的中文 title（避免弹窗显示 HEART_ALERT 英文）
      if (alert && alert.title) return alert.title
      const type = alert.eventType || alert.type || '异常'
      const typeMap = {
        'FALL': '摔倒报警',
        'SMOKE': '烟雾报警',
        'GAS': '烟雾报警',
        'FIRE': '烟雾报警',
        'EMERGENCY': '一键求助',
        'SOS': '一键求助',
        'PRESSURE': '情绪低落',
        'PRESSURE_ALERT': '情绪低落',
        'BED_PRESSURE': '情绪低落',
        'PRESSURE_WARN': '情绪低落',
        '压力告警': '情绪低落',
        '床压告警': '情绪低落',
        'EMOTIONAL_DISTRESS': '情绪低落',
        'HEART_RATE': '心率波动异常',
        'HEART_ALERT': '心率波动异常',
        'HR_ALERT': '心率波动异常',
        'LEAVE_BED': '离床未归',
        'OUT_OF_BED': '离床未归',
        'ENV_ALERT': '环境参数异常',
        'TEMP_ALERT': '环境参数异常',
        'BLOOD_PRESSURE': '血压异常',
        'BLOOD_OXYGEN': '血氧异常',
        'TEMPERATURE': '体温异常',
        'SLEEP_ABNORMAL': '睡眠异常',
        'ACTIVITY_LOW': '活动次数异常',
        'EMOTION': '情绪异常',
        'ABNORMAL_BEHAVIOR': '异常行为'
      }
      return typeMap[type] || type
    },
    addAlert(alert) {
      // ====================== 弹窗规则：只弹 4 大类，心率/环境/离床等归其他的提示级不弹 ======================
      if (!this.shouldShowAlert(alert)) {
        return
      }
      const exists = this.pendingAlerts.some(a => a.id === alert.id)
      if (!exists) {
        this.pendingAlerts.push(alert)
        if (!this.visible) {
          this.showNext()
        }
      }
    },
    formatElderName(name) {
      if (!name || name === '监测老人' || name.trim() === '') {
        return '陈守田'
      }
      return name
    },
    formatDescription(desc) {
      if (!desc) return ''
      let result = desc
      result = result.replace(/压力告警/g, '情绪低落')
      result = result.replace(/床压告警/g, '情绪低落')
      result = result.replace(/当前压力过大\s*（指数[^）]*），/g, '情绪状态偏低，')
      result = result.replace(/当前压力过大\s*\(指数[^)]*\)，/g, '情绪状态偏低，')
      result = result.replace(/监测老人/g, '')
      result = result.replace(/AI\s*摄像头/g, '摄像头')
      result = result.replace(/\s*fallenStatus\s*=\s*\d+\s*，/g, '')
      result = result.replace(/\s*fallenStatus\s*=\s*\d+\s*,/g, '')
      result = result.replace(/\s*fallenStatus\s*=\s*\d+\s*/g, '')
      result = result.replace(/在\s*居家监测\s*被\s*/g, '')
      result = result.replace(/居家监测\s*/g, '')
      if (/烟雾浓度异常|烟雾报警|检测到烟雾/g.test(result)) {
        result = result.replace(/陈守田\s*在\s*检测到烟雾浓度异常，请立即查看并确认安全。/g, '陈守田家中烟雾浓度异常，请尽快核查安全状况。')
        result = result.replace(/陈守田\s*在\s*家中烟雾浓度异常/g, '陈守田家中烟雾浓度异常')
        result = result.replace(/陈守田\s*家中家中烟雾浓度异常/g, '陈守田家中烟雾浓度异常')
        result = result.replace(/检测到烟雾浓度异常，请立即查看并确认安全。/g, '家中烟雾浓度异常，请尽快核查安全状况。')
        if (!/家中烟雾浓度异常/.test(result) && !/陈守田/.test(result)) {
          result = result.replace(/烟雾浓度异常.*$/g, '家中烟雾浓度异常，请尽快核查安全状况。')
        } else if (/陈守田/.test(result) && !/家中烟雾浓度异常/.test(result)) {
          result = result.replace(/烟雾浓度异常.*$/g, '家中烟雾浓度异常，请尽快核查安全状况。')
        }
        result = result.replace(/。。/g, '。')
      }
      if (/当前无摔倒|无摔倒|老人已恢复正常|跌倒状态正常|已自动关闭|已自动处理|当前烟雾浓度正常|烟雾浓度正常|压力已恢复正常|系统自动关闭压力告警|系统自动关闭历史告警|摄像头检测跌倒状态正常/g.test(result)) {
        if (/当前无摔倒|无摔倒|跌倒状态正常|摄像头检测跌倒状态正常|老人已恢复正常/.test(result)) {
          result = result.replace(/当前无摔倒.*/g, '跌倒状态恢复正常，已自动关闭告警')
          result = result.replace(/摄像头检测跌倒状态正常.*$/g, '跌倒状态恢复正常，已自动关闭告警')
          result = result.replace(/跌倒状态恢复正常.*/g, '跌倒状态恢复正常，已自动关闭告警')
        } else if (/当前烟雾浓度正常|烟雾浓度正常|系统自动关闭历史告警/.test(result)) {
          result = result.replace(/当前烟雾浓度正常.*/g, '烟雾浓度已恢复正常，已自动关闭告警')
          result = result.replace(/烟雾浓度已恢复正常.*/g, '烟雾浓度已恢复正常，已自动关闭告警')
          result = result.replace(/系统自动关闭历史告警.*/g, '烟雾浓度已恢复正常，已自动关闭告警')
        } else if (/压力已恢复正常|系统自动关闭压力告警/.test(result)) {
          result = result.replace(/压力已恢复正常.*/g, '情绪状态已恢复正常，已自动关闭告警')
          result = result.replace(/系统自动关闭压力告警.*/g, '情绪状态已恢复正常，已自动关闭告警')
        }
      }
      result = result.replace(/\s*\(指数\s*\d+\.?\d*\)\s*/g, ' ')
      result = result.replace(/，\s*,/g, '，')
      result = result.replace(/^[,，\s]+|[,，\s]+$/g, '')
      result = result.replace(/\s{2,}/g, ' ')
      return result.trim()
    },
    showNext() {
      if (this.pendingAlerts.length > 0) {
        this.visible = true
        this.playAlertSound()
      }
    },
    playAlertSound() {
      if (window.AudioContext || window.webkitAudioContext) {
        try {
          const AudioCtx = window.AudioContext || window.webkitAudioContext
          const audioCtx = new AudioCtx()
          
          if (this.isEmergency) {
            // 紧急按钮：急促连续音
            for (let i = 0; i < 3; i++) {
              const osc = audioCtx.createOscillator()
              const gain = audioCtx.createGain()
              osc.connect(gain)
              gain.connect(audioCtx.destination)
              osc.frequency.value = 880 + i * 100
              osc.type = 'square'
              gain.gain.setValueAtTime(0.4, audioCtx.currentTime + i * 0.25)
              gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + i * 0.25 + 0.2)
              osc.start(audioCtx.currentTime + i * 0.25)
              osc.stop(audioCtx.currentTime + i * 0.25 + 0.2)
            }
          } else {
            // 普通告警：单次音
            const oscillator = audioCtx.createOscillator()
            const gainNode = audioCtx.createGain()
            oscillator.connect(gainNode)
            gainNode.connect(audioCtx.destination)
            oscillator.frequency.value = 880
            oscillator.type = 'sine'
            gainNode.gain.setValueAtTime(0.3, audioCtx.currentTime)
            gainNode.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5)
            oscillator.start(audioCtx.currentTime)
            oscillator.stop(audioCtx.currentTime + 0.5)
          }
        } catch (e) {}
      }
    },
    handleLater() {
      this.pendingAlerts.shift()
      this.visible = false
      if (this.pendingAlerts.length > 0) {
        setTimeout(() => this.showNext(), 1000)
      }
    },
    handleView() {
      this.pendingAlerts.shift()
      this.visible = false
      this.$router.push('/alerts')
      if (this.pendingAlerts.length > 0) {
        setTimeout(() => this.showNext(), 500)
      }
    }
  }
}
</script>

<style scoped>
.alert-notification :deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

.alert-notification :deep(.el-dialog__header) {
  padding: 16px 20px;
  margin-right: 0;
  background: linear-gradient(180deg, #fff5f5 0%, #ffe8e8 100%);
  border-bottom: 1px solid #ffd1d1;
}

.alert-notification__header {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
}

.alert-notification__icon {
  color: #f56c6c;
  animation: alert-pulse 1.5s ease-in-out infinite;
}

@keyframes alert-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.alert-notification__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.alert-notification__content {
  padding: 24px 20px;
  text-align: center;
}

.alert-notification__message {
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.alert-notification__elder {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
}

.alert-notification__text {
  margin: 0 8px;
  color: #606266;
}

.alert-notification__event {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

.alert-notification__desc {
  font-size: 14px;
  color: #909399;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 16px;
  line-height: 1.5;
}

.alert-notification__pending {
  margin-top: 16px;
  padding: 10px 16px;
  background: #fdf6ec;
  border-radius: 8px;
  font-size: 13px;
  color: #e6a23c;
}

.alert-notification__pending-count {
  font-weight: 700;
  font-size: 16px;
  margin: 0 4px;
}

.alert-notification__footer {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.alert-notification__footer :deep(.el-button) {
  padding: 10px 28px;
  border-radius: 8px;
  font-size: 14px;
}

/* ===== 紧急按钮弹窗 - 醒目的红色风格 ===== */
.alert-notification--emergency :deep(.el-dialog) {
  border: 3px solid #f56c6c !important;
  box-shadow: 0 0 30px rgba(245, 108, 108, 0.5), 0 0 60px rgba(245, 108, 108, 0.2);
  animation: emergency-glow 1.5s ease-in-out infinite;
}

@keyframes emergency-glow {
  0%, 100% { box-shadow: 0 0 30px rgba(245, 108, 108, 0.5), 0 0 60px rgba(245, 108, 108, 0.2); }
  50% { box-shadow: 0 0 45px rgba(245, 108, 108, 0.7), 0 0 90px rgba(245, 108, 108, 0.35); }
}

.alert-notification--emergency :deep(.el-dialog__header) {
  background: linear-gradient(180deg, #fee2e2 0%, #fecaca 100%);
  border-bottom: 2px solid #f87171;
}

.alert-notification--emergency .alert-notification__icon {
  color: #dc2626;
  animation: emergency-bell 0.5s ease-in-out infinite;
}

@keyframes emergency-bell {
  0%, 100% { transform: scale(1) rotate(0deg); }
  25% { transform: scale(1.15) rotate(-15deg); }
  50% { transform: scale(1.15) rotate(15deg); }
  75% { transform: scale(1.15) rotate(-15deg); }
}

.alert-notification--emergency .alert-notification__title {
  font-size: 20px;
  color: #991b1b;
}

.alert-notification--emergency .alert-notification__elder {
  font-size: 24px;
  color: #dc2626;
}

.alert-notification--emergency .alert-notification__event {
  font-size: 24px;
  color: #b91c1c;
}

.alert-notification--emergency .alert-notification__text {
  font-size: 18px;
}

.alert-notification--emergency .alert-notification__message {
  font-size: 20px;
}

.alert-notification--emergency .alert-notification__desc {
  font-size: 16px;
  color: #7f1d1d;
  background: #fef2f2;
  border: 1px solid #fecaca;
  padding: 14px 18px;
}

.alert-notification--emergency .alert-notification__pending {
  font-size: 15px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
}

.alert-notification--emergency .alert-notification__footer :deep(.el-button--primary) {
  background: #dc2626;
  border-color: #b91c1c;
  font-size: 16px;
  padding: 12px 32px;
}

.alert-notification--emergency .alert-notification__footer :deep(.el-button--primary:hover) {
  background: #b91c1c;
}

.alert-notification--emergency .alert-notification__footer :deep(.el-button) {
  font-size: 15px;
  padding: 12px 32px;
}
</style>
