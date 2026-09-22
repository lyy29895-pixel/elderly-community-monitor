<template>
  <div class="announce-page">
    <div class="announce-page__hero">
      <div class="announce-page__hero-inner">
        <div class="announce-page__hero-text">
          <h1 class="announce-page__title">{{ isCommunity ? '公告发布' : '社区公告' }}</h1>
          <p class="announce-page__subtitle">
            {{ isCommunity ? '把重要提醒送到每一位居民身边，文字请尽量简明、清楚。' : '及时了解社区通知与便民信息。' }}
          </p>
        </div>
        <div v-if="isCommunity" class="announce-page__hero-action">
          <el-button type="primary" class="announce-page__btn-publish" :icon="Plus" @click="openPublish">
            发布公告
          </el-button>
        </div>
      </div>
    </div>

    <el-card class="announce-page__panel" shadow="never">
      <template #header>
        <div class="announce-page__panel-head">
          <span class="announce-page__panel-title">
            <span class="announce-page__panel-icon" aria-hidden="true"/>
            公告列表
          </span>
          <span v-if="total > 0" class="announce-page__panel-meta">共 {{ total }} 条</span>
        </div>
      </template>
      <el-table
          :data="tableData"
          class="announce-page__table"
          stripe
          v-loading="loading"
          empty-text="暂无公告，稍后再来看看～"
      >
        <el-table-column prop="title" label="标题" min-width="240">
          <template #default="{ row }">
            <span class="announce-page__title-cell" :title="row.title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布方式" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.publishType === 1" class="announce-page__tag-scheduled">预约</span>
            <span v-else class="announce-page__tag-immediate">即时</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light" round size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" effect="light" round size="small">
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.publishedAt || row.scheduledAt) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="viewDetail(row)">查看</el-button>
            <el-button v-if="isCommunity && row.status === 1" text type="warning" size="small" @click="handleRecall(row)">撤下</el-button>
            <el-button v-if="isCommunity && (row.status === 0 || row.status === 2 || row.status === 3)" text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="announce-page__pager">
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

    <!-- 发布/编辑 弹窗 -->
    <el-dialog
        v-model="dlg"
        class="announce-page__dialog"
        width="600px"
        destroy-on-close
        align-center
    >
      <template #header>
        <div class="announce-page__dialog-head">
          <span class="announce-page__dialog-title">{{ isEdit ? '编辑公告' : '发布公告' }}</span>
          <span class="announce-page__dialog-hint">支持即时发布和预约发布，可设置到期自动撤下</span>
        </div>
      </template>
      <el-form :model="form" label-position="top" class="announce-page__form">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="例如：本周义诊安排"/>
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input
              v-model="form.content"
              type="textarea"
              :rows="6"
              maxlength="4000"
              show-word-limit
              placeholder="写清时间、地点与联系方式，便于居民查阅。"
          />
        </el-form-item>
        <el-form-item label="发布方式">
          <el-radio-group v-model="form.publishType">
            <el-radio :value="0">即时发布</el-radio>
            <el-radio :value="1">预约发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.publishType === 1" label="预约发布时间" required>
          <el-date-picker
              v-model="form.scheduledAt"
              type="datetime"
              placeholder="选择预约发布时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :disabled-date="disabledScheduledDate"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="到期自动撤下（可选）">
          <el-date-picker
              v-model="form.endTime"
              type="datetime"
              placeholder="到期后自动撤下，为空则不撤下"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :disabled-date="disabledEndDate"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="优先级（数字越大越靠前展示）">
          <el-input-number v-model="form.priority" :min="0" :max="9" controls-position="right"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" class="announce-page__btn-publish" @click="submit">
          {{ isEdit ? '保存' : '发布' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看详情 弹窗 -->
    <el-dialog
        v-model="detailDlg"
        class="announce-page__dialog"
        width="560px"
        align-center
    >
      <template #header>
        <div class="announce-page__dialog-head">
          <span class="announce-page__dialog-title">{{ detail.title }}</span>
        </div>
      </template>
      <div class="announce-page__detail">
        <div class="announce-page__detail-meta">
          <span>状态：<el-tag :type="statusTagType(detail.status)" effect="light" round size="small">{{ statusLabel(detail.status) }}</el-tag></span>
          <span v-if="detail.publishedAt">发布时间：{{ formatDateTime(detail.publishedAt) }}</span>
          <span v-if="detail.endTime">结束时间：{{ formatDateTime(detail.endTime) }}</span>
        </div>
        <div class="announce-page__detail-body">{{ detail.content }}</div>
      </div>
      <template #footer>
        <el-button @click="detailDlg = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { Plus } from '@element-plus/icons-vue'

export default {
  name: 'Announcements',
  components: { Plus },
  data() {
    return {
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      dlg: false,
      detailDlg: false,
      loading: false,
      isEdit: false,
      editId: null,
      detail: {},
      form: this.getEmptyForm()
    }
  },
  computed: {
    isCommunity() {
      try {
        const u = JSON.parse(sessionStorage.getItem('user') || '{}')
        const r = String(u.role || '').toLowerCase()
        return r === 'community' || r === 'admin'
      } catch (e) {
        return false
      }
    }
  },
  created() {
    this.load()
  },
  methods: {
    getEmptyForm() {
      return { title: '', content: '', priority: 0, publishType: 0, scheduledAt: null, endTime: null }
    },
    disabledScheduledDate(time) {
      return time.getTime() < Date.now() - 8.64e7
    },
    disabledEndDate(time) {
      return time.getTime() < Date.now() - 8.64e7
    },
    statusLabel(s) {
      const map = { 0: '待发布', 1: '已发布', 2: '已撤下', 3: '已过期' }
      return map[s] || '未知'
    },
    statusTagType(s) {
      const map = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
      return map[s] || 'info'
    },
    priorityLabel(p) {
      const n = Number(p)
      if (n >= 7) return '重要'
      if (n >= 4) return '较高'
      if (n >= 1) return '一般'
      return '普通'
    },
    priorityTagType(p) {
      const n = Number(p)
      if (n >= 7) return 'danger'
      if (n >= 4) return 'warning'
      return 'success'
    },
    formatDateTime(dateStr) {
      if (!dateStr) return '—'
      const d = new Date(dateStr)
      if (isNaN(d.getTime())) return dateStr
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const h = String(d.getHours()).padStart(2, '0')
      const min = String(d.getMinutes()).padStart(2, '0')
      return `${y}-${m}-${day} ${h}:${min}`
    },
    load() {
      this.loading = true
      request.get('/api/announcements/page', {
        params: { pageNum: this.pageNum, pageSize: this.pageSize }
      })
          .then(res => {
            if (res.code === '200' && res.data) {
              this.tableData = res.data.records || []
              this.total = res.data.total || 0
            }
          })
          .catch(() => this.$message.error('加载失败'))
          .finally(() => {
            this.loading = false
          })
    },
    openPublish() {
      this.isEdit = false
      this.editId = null
      this.form = this.getEmptyForm()
      this.dlg = true
    },
    viewDetail(row) {
      this.detail = row
      this.detailDlg = true
    },
    handleRecall(row) {
      this.$confirm(`确定撤下公告「${row.title}」？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
          .then(() => {
            request.put(`/api/announcements/${row.id}/recall`).then(res => {
              if (res.code === '200') {
                this.$message.success('已撤下')
                this.load()
              } else {
                this.$message.error(res.msg || '撤下失败')
              }
            }).catch(() => this.$message.error('请求失败'))
          })
          .catch(() => {})
    },
    handleDelete(row) {
      this.$confirm(`确定删除公告「${row.title}」？删除后不可恢复。`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
          .then(() => {
            request.delete(`/api/announcements/${row.id}`).then(res => {
              if (res.code === '200') {
                this.$message.success('已删除')
                this.load()
              } else {
                this.$message.error(res.msg || '删除失败')
              }
            }).catch(() => this.$message.error('请求失败'))
          })
          .catch(() => {})
    },
    submit() {
      if (!this.form.title || !this.form.content) {
        this.$message.warning('请填写标题和正文')
        return
      }
      if (this.form.publishType === 1 && !this.form.scheduledAt) {
        this.$message.warning('预约发布请选择发布时间')
        return
      }
      const url = this.isEdit ? `/api/announcements/${this.editId}` : '/api/announcements'
      const method = this.isEdit ? 'put' : 'post'
      request[method](url, this.form).then(res => {
        if (res.code === '200') {
          this.$message.success(this.isEdit ? '保存成功' : '发布成功')
          this.dlg = false
          this.load()
        } else {
          this.$message.error(res.msg || '失败')
        }
      }).catch(() => this.$message.error('请求失败'))
    }
  }
}
</script>

<style scoped>
.announce-page {
  min-height: calc(100vh - var(--yi-header-height, 62px));
  box-sizing: border-box;
  padding: 18px 20px 32px;
  background: var(--yi-page-gradient, linear-gradient(165deg, #f7fbfe 0%, #eef5fc 48%, #e3eef9 100%));
}

.announce-page__hero {
  margin-bottom: 18px;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(225, 240, 250, 0.78));
  box-shadow: 0 6px 22px rgba(70, 120, 160, 0.07), 0 1px 0 rgba(255, 255, 255, 0.95) inset;
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.28));
}

.announce-page__hero-inner {
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

.announce-page__title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--yi-brand, #365062);
  line-height: 1.25;
}

.announce-page__subtitle {
  margin: 0;
  max-width: 520px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--yi-text-muted, #6d8aa0);
}

.announce-page__hero-action {
  flex-shrink: 0;
}

.announce-page__btn-publish {
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

.announce-page__panel {
  border-radius: 14px;
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.28));
  background: var(--yi-card-surface, #fdfeff);
  box-shadow: 0 4px 20px var(--yi-card-shadow, rgba(65, 110, 150, 0.08));
  overflow: hidden;
}

.announce-page__panel :deep(.el-card__header) {
  padding: 14px 20px;
  background: var(--yi-panel-header-bg, linear-gradient(180deg, #f9fcfe 0%, #eef4fa 100%));
  border-bottom: 1px solid var(--yi-panel-header-border, rgba(145, 185, 215, 0.38));
}

.announce-page__panel :deep(.el-card__body) {
  padding: 0 0 12px;
}

.announce-page__panel-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.announce-page__panel-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--yi-brand, #365062);
}

.announce-page__panel-icon {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #b8def0, var(--yi-accent-deep, #6eb0d4));
  box-shadow: 0 0 0 3px var(--yi-accent-soft-glow, rgba(110, 176, 212, 0.24));
}

.announce-page__panel-meta {
  font-size: 13px;
  color: var(--yi-text-muted, #6d8aa0);
}

.announce-page__table :deep(.el-table__header th) {
  font-weight: 600;
  color: var(--yi-brand, #365062);
  background: var(--yi-table-header-bg, linear-gradient(180deg, #f2f7fc 0%, #e6f0fa 100%)) !important;
}

.announce-page__table :deep(.el-table__row--striped td) {
  background: var(--yi-table-row-striped, rgba(246, 250, 254, 0.96)) !important;
}

.announce-page__table :deep(.el-table__row:hover > td) {
  background-color: var(--yi-table-row-hover, rgba(205, 232, 248, 0.48)) !important;
}

.announce-page__title-cell {
  font-weight: 500;
  color: var(--yi-brand, #365062);
}

.announce-page__tag-immediate {
  display: inline-block;
  padding: 0 8px;
  font-size: 12px;
  line-height: 22px;
  border-radius: 4px;
  background: #e8f5e9;
  color: #2e7d32;
}

.announce-page__tag-scheduled {
  display: inline-block;
  padding: 0 8px;
  font-size: 12px;
  line-height: 22px;
  border-radius: 4px;
  background: #fff3e0;
  color: #e65100;
}

.announce-page__pager {
  padding: 14px 20px 8px;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.announce-page__pager :deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: var(--yi-pagination-active, #4f94c4);
  color: #fff;
}

.announce-page__pager :deep(.el-pagination.is-background .btn-next),
.announce-page__pager :deep(.el-pagination.is-background .btn-prev),
.announce-page__pager :deep(.el-pagination.is-background .el-pager li) {
  background-color: rgba(255, 255, 255, 0.85);
}

.announce-page__dialog-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.announce-page__dialog-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--yi-brand, #365062);
}

.announce-page__dialog-hint {
  font-size: 12px;
  color: var(--yi-text-muted, #6d8aa0);
}

.announce-page__form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--yi-text-body, #415a6e);
}

.announce-page__detail {
  padding: 8px 0;
}

.announce-page__detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #6d8aa0;
}

.announce-page__detail-body {
  font-size: 14px;
  line-height: 1.8;
  color: #415a6e;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 768px) {
  .announce-page {
    padding: 14px 14px 24px;
  }

  .announce-page__pager {
    justify-content: center;
  }
}
</style>

<style>
.announce-page__dialog.el-dialog {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(140, 180, 215, 0.42);
  box-shadow: 0 16px 40px rgba(55, 95, 130, 0.13);
}

.announce-page__dialog .el-dialog__header {
  padding: 16px 20px 10px;
  margin: 0;
  background: linear-gradient(180deg, #f9fcfe 0%, #e9f3fa 100%);
  border-bottom: 1px solid rgba(145, 185, 215, 0.38);
}

.announce-page__dialog .el-dialog__body {
  padding: 18px 20px 8px;
  background: #fdfeff;
}

.announce-page__dialog .el-dialog__footer {
  padding: 12px 20px 18px;
  background: #f4f9fc;
  border-top: 1px solid rgba(145, 185, 215, 0.3);
}
</style>
