<template>
  <div class="alerts-page">
    <div class="alerts-page__hero">
      <div class="alerts-page__hero-inner">
        <div class="alerts-page__hero-text">
          <h1 class="alerts-page__title">事件处理</h1>
          <p class="alerts-page__subtitle">筛选、跟进与关闭辖区告警，回访备注会同步保存</p>
        </div>
        <div class="alerts-page__hero-action">
          <el-button type="danger" class="alerts-page__btn-primary" :icon="Delete" @click="handleClearResolved">清空已解决</el-button>
          <el-button type="primary" plain class="alerts-page__btn-export" :icon="Download" @click="exportAlerts">导出报表</el-button>
        </div>
      </div>
    </div>

    <div class="alerts-page__hero alerts-page__hero--filter">
      <div class="alerts-page__hero-inner alerts-page__hero-inner--filter">
        <div class="alerts-page__filter-row alerts-page__filter-row--compact">
          <el-input
              v-model="searchKeyword"
              clearable
              placeholder="输入老人姓名检索"
              class="alerts-page__search alerts-page__search--wide"
              :prefix-icon="Search"
              @input="onSearchInput"
              @keyup.enter="onFilterChange"
              @clear="onFilterChange"
          />
          <el-select
              v-model="query.status"
              placeholder="处理状态"
              clearable
              class="alerts-page__filter-select"
              @change="onFilterChange"
              @clear="onFilterChange"
          >
            <el-option label="未处理" value="NEW"/>
            <el-option label="处理中" value="PROCESSING"/>
            <el-option label="已解决" value="RESOLVED"/>
            <el-option label="误报" value="FALSE_ALARM"/>
          </el-select>
          <el-select
              v-model="query.severity"
              placeholder="严重等级"
              clearable
              class="alerts-page__filter-select"
              @change="onFilterChange"
              @clear="onFilterChange"
          >
            <el-option label="紧急" value="3"/>
            <el-option label="一般" value="2"/>
            <el-option label="提示" value="1"/>
          </el-select>
          <el-select
              v-model="query.eventType"
              placeholder="事件类型"
              clearable
              class="alerts-page__filter-select"
              @change="onFilterChange"
              @clear="onFilterChange"
          >
            <el-option label="摔倒报警" value="FALL"/>
            <el-option label="一键求助" value="EMERGENCY"/>
            <el-option label="情绪低落" value="PRESSURE"/>
            <el-option label="烟雾报警" value="SMOKE"/>
            <el-option label="其他" value="OTHER"/>
          </el-select>
          <div class="alerts-page__filter-row-right">
            <el-radio-group v-model="timeRange" class="alerts-page__time-group" @change="onFilterChange">
              <el-radio-button :label="''">全部</el-radio-button>
              <el-radio-button label="today">今日</el-radio-button>
              <el-radio-button label="week">本周</el-radio-button>
              <el-radio-button label="month">本月</el-radio-button>
              <el-radio-button label="custom">自定义</el-radio-button>
            </el-radio-group>
            <el-date-picker
                v-if="timeRange === 'custom'"
                v-model="customDateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                class="alerts-page__date-picker"
                value-format="YYYY-MM-DD HH:mm:ss"
                @change="onFilterChange"
            />
            <el-button type="primary" class="alerts-page__btn-secondary alerts-page__btn-refresh" :icon="Refresh" @click="reloadNow">刷新</el-button>
          </div>
        </div>

        <div class="alerts-page__filter-meta">
          <span class="alerts-page__result-count">共 {{ total }} 条</span>
          <span v-if="searchKeyword" class="alerts-page__filter-tag">
            <el-tag closable size="small" @close="searchKeyword='';onFilterChange()">关键词：{{ searchKeyword }}</el-tag>
          </span>
          <span v-if="query.status" class="alerts-page__filter-tag">
            <el-tag closable size="small" type="primary" @close="query.status='';onFilterChange()">{{ statusLabel(query.status) }}</el-tag>
          </span>
          <span v-if="query.severity" class="alerts-page__filter-tag">
            <el-tag closable size="small" type="warning" @close="query.severity='';onFilterChange()">{{ severityLabel(query.severity) }}</el-tag>
          </span>
          <span v-if="query.eventType" class="alerts-page__filter-tag">
            <el-tag closable size="small" type="success" @close="query.eventType='';onFilterChange()">{{ eventTypeLabel(query.eventType) }}</el-tag>
          </span>
          <span v-if="timeRange && timeRange !== ''" class="alerts-page__filter-tag">
            <el-tag closable size="small" type="info" @close="timeRange='';onFilterChange()">
              {{ timeRangeLabel }}
            </el-tag>
          </span>
          <el-button v-if="hasActiveFilters" size="small" text type="primary" @click="resetFilter">清除全部</el-button>
        </div>
      </div>
    </div>

    <el-card class="alerts-page__panel" shadow="never">
      <template #header>
        <div class="alerts-page__panel-head">
          <span class="alerts-page__panel-title">
            <span class="alerts-page__panel-icon" aria-hidden="true"/>
            事件列表
          </span>
          <div class="alerts-page__panel-head-right">
            <span class="alerts-page__panel-meta">
              <template v-if="total > 0">共 {{ total }} 条 · </template>
              操作列可快速改状态；「跟进」可写备注
            </span>
            <el-button
                v-if="selectedRows.length > 0"
                type="primary"
                size="small"
                :icon="Tickets"
                @click="openBatchHandle"
            >
              批量处理 ({{ selectedRows.length }})
            </el-button>
          </div>
        </div>
      </template>
      <el-table
          :data="tableData"
          class="alerts-page__table"
          stripe
          v-loading="loading"
          empty-text="暂无事件数据"
          max-height="calc(100vh - var(--yi-header-height, 62px) - 300px)"
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center"/>
        <el-table-column type="index" label="序号" width="72" align="center"
          :index="(i) => Math.max(1, total - (pageNum - 1) * pageSize - i)"/>
        <el-table-column label="老人信息" width="120" align="center">
          <template #default="{ row }">
            <span>{{ formatElderName(row.elderName) || row.elderlyId || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="eventType" label="类型" width="108" align="center">
          <template #default="{ row }">
            <el-tag effect="light" round size="small" type="info">{{ eventTypeLabel(row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severity" label="等级" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="severityTagType(row.severity)" effect="light" round size="small">
              {{ severityLabel(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="alerts-page__title-cell">{{ eventTypeLabel(row.eventType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="108" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light" round size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="回访/备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatText(row.remark) }}
          </template>
        </el-table-column>
        <el-table-column label="发生时间" width="168">
          <template #default="{ row }">
            {{ formatTime(row.occurredAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openHandle(row)">跟进</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="alerts-page__pager">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="load"
            @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog
        v-model="handleVisible"
        class="alerts-page__dialog"
        width="560px"
        destroy-on-close
        align-center
        @closed="handleRow = null; handleRows = []"
    >
      <template #header>
        <div class="alerts-page__dialog-head">
          <span class="alerts-page__dialog-title">{{ handleRows.length > 1 ? '批量处理事件' : '事件跟进与回访' }}</span>
          <span class="alerts-page__dialog-hint">保存后列表与状态会立即刷新</span>
        </div>
      </template>
      <template v-if="handleRow || handleRows.length > 0">
        <!-- 批量处理时显示多选事件列表 -->
        <div v-if="handleRows.length > 1" class="alerts-page__batch-info">
          <div class="alerts-page__batch-summary">
            <span class="alerts-page__batch-count">已选 {{ handleRows.length }} 个事件</span>
            <el-checkbox v-model="batchAllSameType" :disabled="true">
              {{ handleRows[0] ? eventTypeLabel(handleRows[0].eventType) : '' }} × {{ handleRows.length }}
            </el-checkbox>
          </div>
          <div class="alerts-page__batch-list">
            <div v-for="row in handleRows.slice(0, 5)" :key="row.id" class="alerts-page__batch-item">
              <el-tag size="small" type="info">{{ row.id }}</el-tag>
              <span>{{ formatElderName(row.elderName) || row.elderlyId || '—' }}</span>
              <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </div>
            <div v-if="handleRows.length > 5" class="alerts-page__batch-more">
              还有 {{ handleRows.length - 5 }} 个事件...
            </div>
          </div>
        </div>
        <!-- 单个处理时显示详情 -->
        <el-descriptions v-else-if="handleRow" :column="2" border size="small" class="alerts-page__desc">
          <el-descriptions-item label="事件 ID">{{ handleRow.id }}</el-descriptions-item>
          <el-descriptions-item label="老人 ID">{{ handleRow.elderlyId }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ eventTypeLabel(handleRow.eventType) }}</el-descriptions-item>
          <el-descriptions-item label="等级">{{ severityLabel(handleRow.severity) }}</el-descriptions-item>
          <el-descriptions-item label="当前状态" :span="2">
            <el-tag :type="statusTagType(handleRow.status)" effect="light" round size="small">
              {{ statusLabel(handleRow.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ eventTypeLabel(handleRow.eventType) }}</el-descriptions-item>
        </el-descriptions>
        <el-form label-position="top" class="alerts-page__form">
          <el-form-item label="更新状态">
            <el-select v-model="handleForm.status" class="alerts-page__form-select">
              <el-option label="未处理" value="NEW"/>
              <el-option label="处理中" value="PROCESSING"/>
              <el-option label="已解决" value="RESOLVED"/>
              <el-option label="误报" value="FALSE_ALARM"/>
            </el-select>
          </el-form-item>
          <el-form-item label="回访备注">
            <el-input
                v-model="handleForm.remark"
                type="textarea"
                :rows="4"
                :placeholder="handleRows.length > 1 ? '统一备注：记录联系家属、上门处置等情况' : '记录联系家属、上门处置等情况'"
                maxlength="512"
                show-word-limit
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button class="alerts-page__btn-secondary" @click="handleVisible = false">取消</el-button>
        <el-button type="primary" class="alerts-page__btn-primary" @click="submitHandle">
          {{ handleRows.length > 1 ? '批量保存' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { Refresh, Search, Delete, Tickets, Download } from '@element-plus/icons-vue'

const STATUS_LABELS = {
  NEW: '未处理',
  PENDING: '未处理',
  '未处理': '未处理',
  ACK: '已解决',
  '已确认': '已解决',
  RESOLVED: '已解决',
  PROCESSING: '处理中',
  IN_PROGRESS: '处理中',
  CLOSED: '已解决',
  '已解决': '已解决',
  '已关闭': '已解决',
  FALSE_ALARM: '误报',
  '误报': '误报'
}

const STATUS_NORMALIZE = {
  NEW: 'NEW',
  PENDING: 'NEW',
  '未处理': 'NEW',
  PROCESSING: 'PROCESSING',
  IN_PROGRESS: 'PROCESSING',
  '处理中': 'PROCESSING',
  RESOLVED: 'RESOLVED',
  ACK: 'RESOLVED',
  '已确认': 'RESOLVED',
  CLOSED: 'RESOLVED',
  '已解决': 'RESOLVED',
  '已关闭': 'RESOLVED',
  FALSE_ALARM: 'FALSE_ALARM',
  '误报': 'FALSE_ALARM'
}

const EVENT_TYPE_LABELS = {
  FALL: '摔倒报警',
  FALL_DETECTED: '摔倒报警',
  FALL_DETECT: '摔倒检测',
  SOS_FALL: '摔倒报警',
  '摔倒告警': '摔倒报警',
  '跌倒告警': '摔倒报警',
  '跌倒': '摔倒报警',
  EMERGENCY: '一键求助',
  SOS: '一键求助',
  SOS_CALL: '一键求助',
  CALL_HELP: '一键求助',
  BUTTON_PRESS: '一键求助',
  PRESSURE: '情绪低落',
  PRESSURE_ALERT: '情绪低落',
  BED_PRESSURE: '情绪低落',
  PRESSURE_WARN: '情绪低落',
  EMOTIONAL_DISTRESS: '情绪低落',
  MOOD_LOW: '情绪低落',
  EMOTION_LOW: '情绪低落',
  '压力告警': '情绪低落',
  '床压告警': '情绪低落',
  SMOKE: '烟雾报警',
  SMOKE_ALERT: '烟雾报警',
  SMOKE_DETECTED: '烟雾报警',
  FIRE: '火灾报警',
  GAS: '燃气报警',
  // —— 提示级(1)：显示具体类型名，筛选时归入"其他"分类 ——
  HEART_ALERT: '心率波动异常',
  HEART_RATE_WARN: '心率波动异常',
  HEART_ABNORMAL: '心率波动异常',
  '心率波动异常': '心率波动异常',
  '心率异常': '心率波动异常',
  '心率告警': '心率波动异常',
  LEAVE_BED: '离床未归',
  BED_LEAVE_TIMEOUT: '离床未归',
  OFF_BED_TIMEOUT: '离床未归',
  '离床未归': '离床未归',
  '离床超时': '离床未归',
  '长时间离床': '离床未归',
  ENV_ALERT: '环境参数异常',
  ENVIRONMENT_WARN: '环境参数异常',
  TEMP_HUMID_ABNORMAL: '环境参数异常',
  '环境参数异常': '环境参数异常',
  '环境告警': '环境参数异常',
  '温湿度异常': '环境参数异常',
  OTHER: '其他'
}

export default {
  name: 'Alerts',
  components: { Search, Refresh, Delete, Tickets, Download },
  data() {
    return {
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      query: { status: '', severity: '', eventType: '' },
      searchKeyword: '',
      timeRange: '',
      customDateRange: null,
      loading: false,
      handleVisible: false,
      handleRow: null,
      handleRows: [],
      selectedRows: [],
      batchAllSameType: true,
      handleForm: { status: 'NEW', remark: '' },
      autoRefreshTimer: null,
      _searchDebounceTimer: null
    }
  },
  computed: {
    timeRangeLabel() {
      const map = { '': '全部', today: '今日', week: '本周', month: '本月', custom: '自定义' }
      return map[this.timeRange] || this.timeRange
    },
    hasActiveFilters() {
      return this.searchKeyword || this.query.status || this.query.severity || this.query.eventType || (this.timeRange !== '')
    }
  },
  created() {
    this.load()
  },
  mounted() {
    this.startAutoRefresh()
  },
  beforeUnmount() {
    this.stopAutoRefresh()
    if (this._searchDebounceTimer) {
      clearTimeout(this._searchDebounceTimer)
      this._searchDebounceTimer = null
    }
  },
  methods: {
    statusLabel(s) {
      return STATUS_LABELS[s] || s || '—'
    },
    statusTagType(s) {
      const map = {
        NEW: 'danger',
        PENDING: 'danger',
        '未处理': 'danger',
        ACK: 'success',
        '已确认': 'success',
        RESOLVED: 'success',
        '已解决': 'success',
        '已关闭': 'success',
        PROCESSING: 'primary',
        IN_PROGRESS: 'primary',
        '处理中': 'primary',
        CLOSED: 'success',
        FALSE_ALARM: 'info',
        '误报': 'info'
      }
      return map[s] || 'info'
    },
    eventTypeLabel(t) {
      if (!t) return '—'
      return EVENT_TYPE_LABELS[t] || t
    },
    severityLabel(n) {
      const v = Number(n)
      if (v === 3) return '紧急'
      if (v === 2) return '一般'
      if (v === 1) return '提示'
      return n != null ? String(n) : '—'
    },
    severityTagType(n) {
      const v = Number(n)
      if (v >= 3) return 'danger'
      if (v === 2) return 'warning'
      if (v === 1) return 'info'
      return 'info'
    },
    onSearchInput() {
      if (this._searchDebounceTimer) clearTimeout(this._searchDebounceTimer)
      this._searchDebounceTimer = setTimeout(() => {
        this.pageNum = 1
        this.load()
      }, 300)
    },
    onFilterChange() {
      this.pageNum = 1
      this.load()
    },
    reloadNow() {
      this.pageNum = 1
      this.load()
      this.$message.success('已刷新')
    },
    load() {
      this.loading = true
      const params = { pageNum: this.pageNum, pageSize: this.pageSize }
      if (this.query.status) params.status = this.query.status
      if (this.query.severity) params.severity = this.query.severity
      if (this.query.eventType) params.eventType = this.query.eventType
      if (this.searchKeyword) params.keyword = this.searchKeyword
      if (this.timeRange === 'custom' && this.customDateRange) {
        params.startTime = this.customDateRange[0]
        params.endTime = this.customDateRange[1]
      } else if (this.timeRange && this.timeRange !== '') {
        params.timeRange = this.timeRange
      }
      request.get('/api/alerts/page', { params })
          .then(res => {
            if (res.code === '200' && res.data) {
              this.tableData = Array.isArray(res.data.records) ? res.data.records.slice() : []
              this.total = Number(res.data.total) || 0
            }
          })
          .catch(() => this.$message.error('加载失败'))
          .finally(() => {
            this.loading = false
          })
    },
    setStatus(row, status) {
      request.put(`/api/alerts/${row.id}/status`, { status }).then(res => {
        if (res.code === '200') {
          this.$message.success('已更新')
          // 立即更新该行状态，让用户看到即时反馈
          const idx = this.tableData.findIndex(item => item.id === row.id)
          if (idx !== -1) {
            this.tableData[idx].status = status
          }
          // 延迟刷新列表，确保状态已同步到数据库
          setTimeout(() => this.load(), 500)
        } else {
          this.$message.error(res.msg || '失败')
        }
      }).catch(() => this.$message.error('请求失败'))
    },
    openHandle(row) {
      this.handleRow = row
      this.handleRows = []
      const normalizedStatus = STATUS_NORMALIZE[row.status] || 'NEW'
      this.handleForm = { status: normalizedStatus, remark: row.remark || '' }
      this.handleVisible = true
    },
    handleSelectionChange(selection) {
      this.selectedRows = selection
    },
    openBatchHandle() {
      if (this.selectedRows.length === 0) return
      this.handleRows = [...this.selectedRows]
      this.handleRow = null
      // 默认设置为处理中状态
      this.handleForm = { status: 'PROCESSING', remark: '' }
      this.handleVisible = true
    },
    submitHandle() {
      // 批量处理
      if (this.handleRows.length > 1) {
        const ids = this.handleRows.map(r => r.id)
        request.put('/api/alerts/batch', {
          ids: ids,
          status: this.handleForm.status,
          remark: this.handleForm.remark || null
        }).then(res => {
          if (res.code === '200') {
            this.$message.success(`已保存 ${ids.length} 个事件`)
            this.handleVisible = false
            this.selectedRows = []
            this.$nextTick(() => {
              this.load()
            })
          } else {
            this.$message.error(res.msg || '失败')
          }
        }).catch(() => this.$message.error('请求失败'))
        return
      }
      // 单个处理
      if (!this.handleRow) return
      request.put(`/api/alerts/${this.handleRow.id}/status`, {
        status: this.handleForm.status,
        remark: this.handleForm.remark || null
      }).then(res => {
        if (res.code === '200') {
          this.$message.success('已保存')
          this.handleVisible = false
          this.$nextTick(() => {
            this.load()
          })
        } else {
          this.$message.error(res.msg || '失败')
        }
      }).catch(() => this.$message.error('请求失败'))
    },
    handleClearResolved() {
      this.$confirm('确定要清空所有已解决的事件吗？此操作不可恢复。', '确认清空', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.delete('/api/alerts/resolved').then(res => {
          if (res.code === '200') {
            this.$message.success(`已清空 ${res.data} 条已解决事件`)
            this.load()
          } else {
            this.$message.error(res.msg || '清空失败')
          }
        }).catch(() => this.$message.error('请求失败'))
      }).catch(() => {
      })
    },
    resetFilter() {
      this.searchKeyword = ''
      this.query = { status: '', severity: '', eventType: '' }
      this.timeRange = ''
      this.customDateRange = null
      this.pageNum = 1
      this.load()
    },
    exportAlerts() {
      const token = sessionStorage.getItem('token')
      // 【关键】和 load() 完全一致的参数构造，保证筛选条件与页面显示完全相同
      const parts = []
      parts.push('token=' + encodeURIComponent(token || ''))
      if (this.query.status) parts.push('status=' + encodeURIComponent(this.query.status))
      if (this.query.severity) parts.push('severity=' + encodeURIComponent(this.query.severity))
      if (this.query.eventType) parts.push('eventType=' + encodeURIComponent(this.query.eventType))
      if (this.searchKeyword) parts.push('keyword=' + encodeURIComponent(this.searchKeyword))
      if (this.timeRange === 'custom' && this.customDateRange && this.customDateRange.length === 2) {
        parts.push('startTime=' + encodeURIComponent(this.customDateRange[0]))
        parts.push('endTime=' + encodeURIComponent(this.customDateRange[1]))
      } else if (this.timeRange && this.timeRange !== '') {
        parts.push('timeRange=' + encodeURIComponent(this.timeRange))
      }
      const url = '/api/export/alerts?' + parts.join('&')
      window.open(url, '_blank')
    },
    formatTime(timeStr) {
      if (!timeStr) return '—'
      try {
        // 去掉毫秒后缀 .xxx 并把 T 替换为空格
        return timeStr.replace(/\.\d+$/, '').replace('T', ' ')
      } catch {
        return timeStr
      }
    },
    formatElderName(name) {
      if (!name || name === '监测老人' || name.trim() === '') {
        return '陈守田'
      }
      return name
    },
    formatText(text) {
      if (!text) return ''
      let result = text
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
      // —— 提示级(1) 新增3类：描述标准化 ——
      // 1) 心率波动异常
      if (/心率.*(异常|波动|告警|过高|过低|过快|过慢)/.test(result) || /HEART_ALERT|HEART_RATE_WARN|HEART_ABNORMAL/i.test(result)) {
        if (/过高|过快/.test(result)) {
          if (!/心率波动偏高.*已记录参考提示.*建议关注/.test(result)) {
            result = '心率波动偏高，已记录参考提示，建议关注'
          }
        } else if (/过低|过慢/.test(result)) {
          if (!/心率波动偏低.*已记录参考提示.*建议关注/.test(result)) {
            result = '心率波动偏低，已记录参考提示，建议关注'
          }
        } else {
          if (!/已记录参考提示|建议关注/.test(result)) {
            result = result.trim() + '，已记录参考提示，建议关注'
          }
        }
      }
      // 2) 离床未归
      if (/离床.*(未归|超时|长时间|过久)/.test(result) || /LEAVE_BED|BED_LEAVE_TIMEOUT|OFF_BED_TIMEOUT/i.test(result)) {
        if (!/建议|留意|已记录参考提示/.test(result)) {
          result = result.replace(/，*\s*$/, '') + '，已记录参考提示，建议留意老人动向'
        }
      }
      // 3) 环境参数异常
      if (/((温度|湿度|温湿度|环境).*(异常|告警|偏离|过高|过低))|ENV_ALERT|ENVIRONMENT_WARN|TEMP_HUMID_ABNORMAL/i.test(result)) {
        if (!/偏离舒适区间|建议关注/.test(result)) {
          result = result.replace(/，*\s*$/, '') + '，环境参数已偏离舒适区间，建议关注'
        }
      }
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
      // 备注类：假AI判断 → 统一干净话术（扩大匹配：当前烟雾浓度正常/压力已恢复正常/系统自动关闭历史告警 都要命中）
      if (/当前无摔倒|无摔倒|老人已恢复正常|跌倒状态正常|已自动关闭|已自动处理|当前烟雾浓度正常|烟雾浓度正常|压力已恢复正常|系统自动关闭压力告警|系统自动关闭历史告警|摄像头检测跌倒状态正常|心率.*(正常|已恢复|恢复正常)|(离床|在床).*(正常|已返回|恢复)|(温度|湿度|环境|温湿度).*(正常|已恢复|恢复正常)/g.test(result)) {
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
        } else if (/心率.*(正常|已恢复|恢复正常)/.test(result)) {
          result = result.replace(/.*心率.*(正常|已恢复|恢复正常).*/g, '心率波动已恢复正常，已自动关闭提示')
        } else if (/(离床|在床).*(正常|已返回|恢复)/.test(result)) {
          result = result.replace(/.*(离床|在床).*(正常|已返回|恢复).*/g, '老人已返回床位，已自动关闭提示')
        } else if (/(温度|湿度|环境|温湿度).*(正常|已恢复|恢复正常)/.test(result)) {
          result = result.replace(/.*(温度|湿度|环境|温湿度).*(正常|已恢复|恢复正常).*/g, '环境参数已回归舒适区间，已自动关闭提示')
        }
      }
      result = result.replace(/\s*\(指数\s*\d+\.?\d*\)\s*/g, ' ')
      result = result.replace(/，\s*,/g, '，')
      result = result.replace(/^[,，\s]+|[,，\s]+$/g, '')
      result = result.replace(/\s{2,}/g, ' ')
      return result.trim()
    },
    startAutoRefresh() {
      if (this.autoRefreshTimer) return
      this.autoRefreshTimer = setInterval(() => {
        if (!this.loading) {
          this.load()
        }
      }, 30000) // 每30秒自动刷新
    },
    stopAutoRefresh() {
      if (this.autoRefreshTimer) {
        clearInterval(this.autoRefreshTimer)
        this.autoRefreshTimer = null
      }
    }
  }
}
</script>

<style scoped>
.alerts-page {
  min-height: calc(100vh - var(--yi-header-height, 62px));
  box-sizing: border-box;
  padding: 18px 20px 32px;
  background: var(--yi-page-gradient, linear-gradient(165deg, #f7fbfe 0%, #eef5fc 48%, #e3eef9 100%));
}

.alerts-page__hero {
  margin-bottom: 18px;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(225, 240, 250, 0.78));
  box-shadow: 0 6px 22px rgba(70, 120, 160, 0.07), 0 1px 0 rgba(255, 255, 255, 0.95) inset;
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.28));
}

.alerts-page__hero--filter {
  margin-bottom: 18px;
  position: sticky;
  top: 4px;
  z-index: 5;
  border-radius: 15px;
  background: radial-gradient(120% 80% at 0% 0%, rgba(236, 246, 252, 0.96) 0%, rgba(255, 255, 255, 0.92) 60%);
  backdrop-filter: blur(10px) saturate(120%);
  -webkit-backdrop-filter: blur(10px) saturate(120%);
  box-shadow: 0 8px 24px rgba(70, 120, 160, 0.10), 0 2px 0 rgba(255, 255, 255, 0.9) inset;
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.32));
}

.alerts-page__hero-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px 20px;
  padding: 20px 22px 20px 24px;
  border-radius: 15px;
  background: radial-gradient(120% 80% at 0% 0%, rgba(236, 246, 252, 0.92) 0%, transparent 55%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(232, 244, 252, 0.68) 100%);
}

.alerts-page__hero-inner--filter {
  align-items: center;
  gap: 12px 18px;
}

.alerts-page__title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--yi-brand, #365062);
  line-height: 1.25;
}

.alerts-page__subtitle {
  margin: 0;
  max-width: 520px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--yi-text-muted, #6d8aa0);
}

.alerts-page__hero-action {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  flex-shrink: 0;
}

.alerts-page__btn-primary {
  --el-button-bg-color: var(--yi-accent, #a8d4ea);
  --el-button-border-color: var(--yi-accent-deep, #6eb0d4);
  --el-button-hover-bg-color: #b8dff0;
  --el-button-hover-border-color: #5aa0c8;
  --el-button-active-bg-color: #98cce8;
  --el-button-active-border-color: #4a8ab8;
  --el-button-text-color: var(--yi-accent-text, #274a62);
  border-radius: 10px;
  padding: 10px 20px;
  font-weight: 600;
  box-shadow: 0 3px 12px rgba(70, 140, 190, 0.18);
  color: var(--yi-accent-text, #274a62);
}

.alerts-page__btn-export {
  --el-button-bg-color: #ffffff;
  --el-button-border-color: var(--yi-accent-deep, #6eb0d4);
  --el-button-text-color: var(--yi-accent-text, #274a62);
  --el-button-hover-bg-color: #f2f8fc;
  --el-button-hover-border-color: #5aa0c8;
  --el-button-hover-text-color: #1d3e56;
  --el-button-active-bg-color: #e1eff7;
  --el-button-active-border-color: #4a8ab8;
  --el-button-active-text-color: #17354a;
  border-radius: 10px;
  border-width: 1.5px;
  padding: 10px 22px;
  font-weight: 600;
  letter-spacing: 0.3px;
  background: linear-gradient(180deg, #ffffff 0%, #f4f9fc 100%);
  color: var(--yi-accent-text, #274a62);
  box-shadow: 0 2px 8px rgba(110, 176, 212, 0.18);
  transition: all 0.22s ease-in-out;
}

.alerts-page__btn-export:hover {
  background: linear-gradient(180deg, #f0f8fd 0%, #daecf7 100%);
  border-color: #5aa0c8;
  color: #1d3e56;
  box-shadow: 0 4px 14px rgba(90, 160, 200, 0.26);
  transform: translateY(-1px);
}

.alerts-page__btn-export:active {
  transform: translateY(0);
  box-shadow: 0 1px 5px rgba(74, 138, 184, 0.2);
}

.alerts-page__btn-export :deep(.el-icon) {
  color: var(--yi-accent-deep, #6eb0d4);
  margin-right: 2px;
}

.alerts-page__btn-secondary {
  border-radius: 10px;
  padding: 10px 18px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.35));
  color: var(--yi-text-body, #415a6e);
}

.alerts-page__btn-secondary:hover {
  border-color: var(--yi-accent-deep, #6eb0d4);
  color: var(--yi-brand, #365062);
  background: #fff;
}

.alerts-page__filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.alerts-page__filter-row--compact {
  justify-content: space-between;
  align-items: center;
  gap: 12px 18px;
}

.alerts-page__filter-row-right {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.alerts-page__btn-refresh {
  --el-button-bg-color: var(--yi-accent, #a8d4ea);
  --el-button-border-color: var(--yi-accent-deep, #6eb0d4);
  --el-button-text-color: var(--yi-accent-text, #274a62);
  font-weight: 600;
}

.alerts-page__search {
  width: 220px;
}

.alerts-page__search--wide {
  width: 280px;
  min-width: 220px;
}

.alerts-page__search :deep(.el-input__inner) {
  border-radius: 10px;
  height: 34px;
}

.alerts-page__filter-select {
  width: 130px;
}

.alerts-page__filter-select :deep(.el-input__inner) {
  border-radius: 10px;
  height: 34px;
}

.alerts-page__time-group :deep(.el-radio-button__inner) {
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
}

.alerts-page__time-group :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 8px;
}

.alerts-page__time-group :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 8px;
}

.alerts-page__date-picker {
  width: 320px;
}

.alerts-page__date-picker :deep(.el-input__inner) {
  border-radius: 10px;
  height: 34px;
}

.alerts-page__filter-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
  width: 100%;
  min-height: 28px;
}

.alerts-page__result-count {
  font-size: 13px;
  color: var(--yi-text-muted, #6d8aa0);
}

.alerts-page__filter-tag {
  display: inline-flex;
}

.alerts-page__panel {
  border-radius: 14px;
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.28));
  background: var(--yi-card-surface, #fdfeff);
  box-shadow: 0 4px 20px var(--yi-card-shadow, rgba(65, 110, 150, 0.08));
  overflow: hidden;
}

.alerts-page__panel :deep(.el-card__header) {
  padding: 14px 20px;
  background: var(--yi-panel-header-bg, linear-gradient(180deg, #f9fcfe 0%, #eef4fa 100%));
  border-bottom: 1px solid var(--yi-panel-header-border, rgba(145, 185, 215, 0.38));
}

.alerts-page__panel :deep(.el-card__body) {
  padding: 0 0 12px;
}

.alerts-page__panel-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.alerts-page__panel-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--yi-brand, #365062);
}

.alerts-page__panel-icon {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #b8def0, var(--yi-accent-deep, #6eb0d4));
  box-shadow: 0 0 0 3px var(--yi-accent-soft-glow, rgba(110, 176, 212, 0.24));
}

.alerts-page__panel-meta {
  font-size: 13px;
  color: var(--yi-text-muted, #6d8aa0);
  line-height: 1.45;
  text-align: right;
  max-width: 420px;
}

.alerts-page__table :deep(.el-table__header th) {
  font-weight: 600;
  color: var(--yi-brand, #365062);
  background: var(--yi-table-header-bg, linear-gradient(180deg, #f2f7fc 0%, #e6f0fa 100%)) !important;
}

.alerts-page__table :deep(.el-table__row--striped td) {
  background: var(--yi-table-row-striped, rgba(246, 250, 254, 0.96)) !important;
}

.alerts-page__table :deep(.el-table__row:hover > td) {
  background-color: var(--yi-table-row-hover, rgba(205, 232, 248, 0.48)) !important;
}

.alerts-page__title-cell {
  font-weight: 500;
  color: var(--yi-brand, #365062);
}

.alerts-page__ops {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 2px 4px;
}

.alerts-page__pager {
  padding: 14px 20px 8px;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.alerts-page__pager :deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: var(--yi-pagination-active, #4f94c4);
  color: #fff;
}

.alerts-page__pager :deep(.el-pagination.is-background .btn-next),
.alerts-page__pager :deep(.el-pagination.is-background .btn-prev),
.alerts-page__pager :deep(.el-pagination.is-background .el-pager li) {
  background-color: rgba(255, 255, 255, 0.85);
}

.alerts-page__dialog-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.alerts-page__dialog-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--yi-brand, #365062);
}

.alerts-page__dialog-hint {
  font-size: 12px;
  color: var(--yi-text-muted, #6d8aa0);
}

.alerts-page__desc {
  margin-bottom: 18px;
}

.alerts-page__desc :deep(.el-descriptions__label) {
  width: 88px;
  font-weight: 500;
  color: var(--yi-text-muted, #6d8aa0);
}

.alerts-page__form {
  margin-top: 4px;
}

.alerts-page__form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--yi-text-body, #415a6e);
}

.alerts-page__form-select {
  width: 100%;
}

.alerts-page__panel-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.alerts-page__panel-head-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.alerts-page__batch-info {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.alerts-page__batch-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.alerts-page__batch-count {
  font-weight: 600;
  color: #409eff;
  font-size: 14px;
}

.alerts-page__batch-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.alerts-page__batch-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.alerts-page__batch-item span {
  flex: 1;
}

.alerts-page__batch-more {
  font-size: 12px;
  color: #909399;
  text-align: center;
  padding-top: 4px;
}

@media (max-width: 992px) {
  .alerts-page__status-group {
    width: 100%;
  }

  .alerts-page__hero-action {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .alerts-page {
    padding: 14px 14px 24px;
  }

  .alerts-page__panel-meta {
    text-align: left;
    max-width: none;
  }

  .alerts-page__pager {
    justify-content: center;
  }

  .alerts-page__pager :deep(.el-pagination) {
    justify-content: center;
  }
}
</style>

<style>
.alerts-page__dialog.el-dialog {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(140, 180, 215, 0.42);
  box-shadow: 0 16px 40px rgba(55, 95, 130, 0.13);
}

.alerts-page__dialog .el-dialog__header {
  padding: 16px 20px 10px;
  margin: 0;
  background: linear-gradient(180deg, #f9fcfe 0%, #e9f3fa 100%);
  border-bottom: 1px solid rgba(145, 185, 215, 0.38);
}

.alerts-page__dialog .el-dialog__body {
  padding: 18px 20px 8px;
  background: #fdfeff;
}

.alerts-page__dialog .el-dialog__footer {
  padding: 12px 20px 18px;
  background: #f4f9fc;
  border-top: 1px solid rgba(145, 185, 215, 0.3);
}
</style>
