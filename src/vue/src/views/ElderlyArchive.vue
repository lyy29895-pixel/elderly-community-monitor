<template>
  <div class="elderly-archive">
    <div class="elderly-archive__hero">
      <div class="elderly-archive__hero-inner">
        <div class="elderly-archive__hero-text">
          <h1 class="elderly-archive__page-title">老人档案</h1>
          <p class="elderly-archive__subtitle">维护辖区老人基础信息，便于监测与紧急联系人绑定</p>
        </div>
        <div class="elderly-archive__hero-action">
          <el-button type="primary" class="elderly-archive__btn-primary" :icon="Plus" @click="openCreate">
            新增老人档案
          </el-button>
        </div>
      </div>
    </div>

    <div class="elderly-archive__hero elderly-archive__hero--filter">
      <div class="elderly-archive__hero-inner elderly-archive__hero-inner--filter">
        <div class="elderly-archive__filter-row">
          <el-input
              v-model="searchKeyword"
              clearable
              placeholder="输入老人姓名或编号检索"
              class="elderly-archive__search"
              :prefix-icon="Search"
              @input="applyFilters"
          />
          <el-select
              v-model="filterNursingLevel"
              placeholder="护理等级"
              clearable
              class="elderly-archive__filter-select"
              @change="applyFilters"
          >
            <el-option value="LEVEL_1" label="特别护理"/>
            <el-option value="LEVEL_2" label="重点护理"/>
            <el-option value="LEVEL_3" label="一般护理"/>
            <el-option value="LEVEL_4" label="自理"/>
          </el-select>
          <el-select
              v-model="filterBuilding"
              placeholder="楼栋"
              clearable
              class="elderly-archive__filter-select"
              @change="applyFilters"
          >
            <el-option v-for="b in buildings" :key="b" :value="b" :label="b"/>
          </el-select>
          <el-button class="elderly-archive__btn-secondary" :icon="Refresh" @click="resetAndLoad">刷新</el-button>
        </div>

        <div class="elderly-archive__filter-meta">
          <span class="elderly-archive__result-count">共 {{ filteredData.length }} 条</span>
          <span v-if="searchKeyword" class="elderly-archive__filter-tag">
            <el-tag closable size="small" @close="searchKeyword='';applyFilters()">关键词：{{ searchKeyword }}</el-tag>
          </span>
          <span v-if="filterNursingLevel" class="elderly-archive__filter-tag">
            <el-tag closable size="small" type="primary" @close="filterNursingLevel='';applyFilters()">{{ nursingLevelLabel(filterNursingLevel) }}</el-tag>
          </span>
          <span v-if="filterBuilding" class="elderly-archive__filter-tag">
            <el-tag closable size="small" type="success" @close="filterBuilding='';applyFilters()">{{ filterBuilding }}</el-tag>
          </span>
          <el-button v-if="hasActiveFilters" size="small" text type="primary" @click="resetAndLoad">清除全部</el-button>
        </div>
      </div>
    </div>

    <el-card class="elderly-archive__panel" shadow="never">
      <template #header>
        <div class="elderly-archive__panel-head">
          <span class="elderly-archive__panel-title">
            <span class="elderly-archive__panel-icon" aria-hidden="true"/>
            档案列表
          </span>
          <span class="elderly-archive__panel-meta">共 {{ tableData.length }} 条</span>
        </div>
      </template>
      <el-table
          :data="pagedArchiveData"
          class="elderly-archive__table"
          stripe
          empty-text="暂无档案，请先新增"
          style="width: 100%"
          :header-cell-style="{ padding: '10px 0', background: 'var(--yi-table-header-bg, linear-gradient(180deg, #f2f7fc 0%, #e6f0fa 100%))' }"
      >
        <el-table-column prop="id" label="ID" width="50" align="center"/>
        <el-table-column prop="realName" label="姓名" width="65">
          <template #default="{ row }">
            <span class="elderly-archive__name">{{ row.realName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="55" align="center">
          <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="60" align="center"/>
        <el-table-column label="护理等级" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="nursingLevelTagType(row.nursingLevel)" size="small">
              {{ nursingLevelLabel(row.nursingLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="电话" min-width="95" show-overflow-tooltip>
          <template #default="{ row }">{{ displayDash(row.phone) }}</template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="100" show-overflow-tooltip/>
        <el-table-column prop="room" label="房间号" min-width="130" align="center" show-overflow-tooltip/>
        <el-table-column label="紧急联系人" min-width="80" show-overflow-tooltip>
          <template #default="{ row }">{{ displayDash(row.emergencyContact) }}</template>
        </el-table-column>
        <el-table-column label="紧急电话" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ displayDash(row.emergencyPhone) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="elderly-archive__pagination">
        <el-pagination
            v-if="filteredData.length > 0"
            v-model:page-size="archivePageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="filteredData.length"
            v-model:current-page="archiveCurrentPage"
        />
      </div>
    </el-card>

    <el-dialog
        v-model="dlgVisible"
        width="520px"
        destroy-on-close
        class="elderly-archive__dialog"
        align-center
    >
      <template #header>
        <div class="elderly-archive__dialog-head">
          <span class="elderly-archive__dialog-title">{{ dlgTitle }}</span>
          <span class="elderly-archive__dialog-hint">姓名必填；紧急联系人用于异常事件通知</span>
        </div>
      </template>
      <el-form :model="form" class="elderly-archive__dialog-form" label-position="top">
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" placeholder="请输入姓名"/>
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" placeholder="可选" clearable class="elderly-archive__form-full">
            <el-option :value="0" label="未知"/>
            <el-option :value="1" label="男"/>
            <el-option :value="2" label="女"/>
          </el-select>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input v-model.number="form.age" type="number" placeholder="请输入年龄" min="0" max="150"/>
        </el-form-item>
        <el-form-item label="护理等级">
          <el-select v-model="form.nursingLevel" placeholder="请选择护理等级" style="width: 100%">
            <el-option value="LEVEL_1" label="特别护理"/>
            <el-option value="LEVEL_2" label="重点护理"/>
            <el-option value="LEVEL_3" label="一般护理"/>
            <el-option value="LEVEL_4" label="自理"/>
          </el-select>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="联系电话"/>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="居住地址"/>
        </el-form-item>
        <el-form-item label="房间号">
          <el-input v-model="form.room" placeholder="如 1号楼1单元101"/>
        </el-form-item>
        <el-form-item label="紧急联系人">
          <el-input v-model="form.emergencyContact" placeholder="姓名"/>
        </el-form-item>
        <el-form-item label="紧急电话">
          <el-input v-model="form.emergencyPhone" placeholder="电话"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="elderly-archive__btn-secondary" @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" class="elderly-archive__btn-primary" @click="submitElderly">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { Plus, Refresh, Search, Loading } from '@element-plus/icons-vue'

export default {
  name: 'ElderlyArchive',
  components: { Plus, Refresh, Search, Loading },
  data() {
    return {
      tableData: [],
      filteredData: [],
      archiveCurrentPage: 1,
      archivePageSize: 10,
      dlgVisible: false,
      dlgTitle: '',
      form: {},
      searchKeyword: '',
      filterNursingLevel: '',
      filterBuilding: '',
      buildings: [],
      lookupHint: ''
    }
  },
  computed: {
    hasActiveFilters() {
      return !!(this.searchKeyword || this.filterNursingLevel || this.filterBuilding)
    },
    pagedArchiveData() {
      const start = (this.archiveCurrentPage - 1) * this.archivePageSize
      return this.filteredData.slice(start, start + this.archivePageSize)
    }
  },
  watch: {
    searchKeyword() { this.applyFilters() },
    filterNursingLevel() { this.applyFilters() },
    filterBuilding() { this.applyFilters() }
  },
  created() {
    this.load()
  },
  methods: {
    genderLabel(g) {
      if (g == null || g === '' || g === 'null' || g === 'undefined') return '—'
      if (g === 1 || g === '1' || g === '男' || g === 'M' || g === 'm') return '男'
      if (g === 2 || g === '2' || g === '女' || g === 'F' || g === 'f') return '女'
      if (g === 0 || g === '0' || g === '未知' || g === 'U' || g === 'u') return '未知'
      const s = String(g).trim()
      if (s.includes('男')) return '男'
      if (s.includes('女')) return '女'
      return '—'
    },
    getStatusType(status) {
      if (status === 0 || status === '0') return 'success'
      if (status === 1 || status === '1') return 'danger'
      return 'info'
    },
    getStatusLabel(status) {
      if (status === 0 || status === '0') return '正常'
      if (status === 1 || status === '1') return '异常'
      return '—'
    },
    nursingLevelLabel(level) {
      const map = {
        'LEVEL_1': '特别护理',
        'LEVEL_2': '重点护理',
        'LEVEL_3': '一般护理',
        'LEVEL_4': '自理',
        '特别护理': '特别护理',
        '重点护理': '重点护理',
        '一般护理': '一般护理',
        '自理': '自理'
      }
      return map[level] || level || '—'
    },
    nursingLevelTagType(level) {
      const map = {
        'LEVEL_1': 'danger',
        'LEVEL_2': 'warning',
        'LEVEL_3': 'primary',
        'LEVEL_4': 'success',
        '特别护理': 'danger',
        '重点护理': 'warning',
        '一般护理': 'primary',
        '自理': 'success'
      }
      return map[level] || 'info'
    },
    displayDash(v) {
      if (v == null) return '—'
      const s = String(v).trim()
      return s || '—'
    },
    load() {
      request.get('/api/elderly/list').then(res => {
        if (res.code === '200') {
          const data = res.data || []
          this.tableData = data
          this.buildings = this.extractBuildings(this.tableData)
          this.applyFilters()
        }
      }).catch(() => this.$message.error('加载失败'))
    },
    extractBuildings(data) {
      const set = new Set()
      data.forEach(e => {
        const room = (e.room || '').trim()
        if (room) {
          // 新格式：1号楼1单元101 → 提取为 "1号楼"
          const newMatch = room.match(/^(\d+号楼)/)
          if (newMatch) {
            set.add(newMatch[1])
            return
          }
          // 旧格式：A-201 → 提取为 "A栋"
          const oldMatch = room.match(/^([A-Za-z])-/)
          if (oldMatch) {
            set.add(oldMatch[1] + '栋')
            return
          }
          // 其他：用第一个字符判断
          const floorMatch = room.match(/^(\d)/)
          if (floorMatch) {
            set.add(floorMatch[1] + '号楼')
          } else {
            set.add('其他')
          }
        }
      })
      return [...set].sort()
    },
    getElderBuilding(elder) {
      const room = (elder.room || '').trim()
      if (!room) return '其他'
      // 新格式：1号楼1单元101 → 返回 "1号楼"
      const newMatch = room.match(/^(\d+号楼)/)
      if (newMatch) return newMatch[1]
      // 旧格式：A-201 → 返回 "A栋"
      const oldMatch = room.match(/^([A-Za-z])-/)
      if (oldMatch) return oldMatch[1] + '栋'
      const floorMatch = room.match(/^(\d)/)
      if (floorMatch) return floorMatch[1] + '号楼'
      return '其他'
    },
    applyFilters() {
      let result = [...this.tableData]

      if (this.searchKeyword) {
        const q = this.searchKeyword.toLowerCase()
        result = result.filter(e => {
          const idMatch = String(e.id).includes(q)
          const nameMatch = (e.realName || '').toLowerCase().includes(q)
          return idMatch || nameMatch
        })
      }

      if (this.filterNursingLevel) {
        // 兼容历史数据：库中可能存在 LEVEL_X 编码或中文值，统一归一为编码后比较
        const normalize = v => {
          const m = { '特别护理': 'LEVEL_1', '重点护理': 'LEVEL_2', '一般护理': 'LEVEL_3', '自理': 'LEVEL_4' }
          return m[v] || v || ''
        }
        result = result.filter(e => normalize(e.nursingLevel) === this.filterNursingLevel)
      }

      if (this.filterBuilding) {
        result = result.filter(e => this.getElderBuilding(e) === this.filterBuilding)
      }

      this.archiveCurrentPage = 1
      this.filteredData = result
    },
    resetAndLoad() {
      this.searchKeyword = ''
      this.filterNursingLevel = ''
      this.filterBuilding = ''
      this.lookupHint = ''
      this.load()
    },
    openCreate() {
      this.dlgTitle = '新增老人档案'
      this.form = { gender: null }
      this.dlgVisible = true
    },
    openEdit(row) {
      this.dlgTitle = '编辑老人档案'
      this.form = JSON.parse(JSON.stringify(row))
      this.dlgVisible = true
    },
    submitElderly() {
      if (!this.form.realName) {
        this.$message.warning('请填写姓名')
        return
      }
      const submitData = { ...this.form }
      const api = submitData.id ? request.put('/api/elderly', submitData) : request.post('/api/elderly', submitData)
      api.then(res => {
        if (res.code === '200') {
          this.$message.success('保存成功')
          this.dlgVisible = false
          this.load()
        } else {
          this.$message.error(res.msg || '失败')
        }
      }).catch((err) => {
        const st = err.response && err.response.status
        const d = err.response && err.response.data
        let msg = (typeof d === 'object' && d && d.msg) ? d.msg : null
        if (!msg && typeof d === 'string' && d) {
          try {
            const o = JSON.parse(d)
            if (o && o.msg) msg = o.msg
          } catch (e) { /* ignore */ }
        }
        if (st === 401) {
          msg = '未登录或登录已失效，请重新登录'
        }
        if (!msg && !err.response) {
          msg = '无法连接后端（请确认 Spring Boot 已启动，且开发环境通过 npm 代理访问）'
        }
        this.$message.error(msg || (st ? `请求失败（HTTP ${st}）` : '请求失败'))
      })
    },
    handleDelete(row) {
      const name = row.realName || '该老人'
      this.$confirm(`确定要删除老人「${name}」的档案吗？删除后将无法恢复。`, '确认删除', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.delete(`/api/elderly/${row.id}`).then(res => {
          if (res.code === '200') {
            this.$message.success('删除成功')
            this.load()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        }).catch((err) => {
          const st = err.response && err.response.status
          const d = err.response && err.response.data
          let msg = (typeof d === 'object' && d && d.msg) ? d.msg : null
          if (!msg && typeof d === 'string' && d) {
            try {
              const o = JSON.parse(d)
              if (o && o.msg) msg = o.msg
            } catch (e) { /* ignore */ }
          }
          if (st === 401) {
            msg = '未登录或登录已失效，请重新登录'
          }
          if (st === 403) {
            msg = '无权限删除该老人档案'
          }
          if (!msg && !err.response) {
            msg = '无法连接后端（请确认 Spring Boot 已启动）'
          }
          this.$message.error(msg || (st ? `请求失败（HTTP ${st}）` : '请求失败'))
        })
      }).catch(() => {
      })
    }
  }
}
</script>

<style scoped>
.elderly-archive {
  min-height: calc(100vh - var(--yi-header-height, 62px));
  box-sizing: border-box;
  padding: 18px 20px 32px;
  background: var(--yi-page-gradient, linear-gradient(165deg, #f7fbfe 0%, #eef5fc 48%, #e3eef9 100%));
}

.elderly-archive__hero {
  margin-bottom: 18px;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(225, 240, 250, 0.78));
  box-shadow: 0 6px 22px rgba(70, 120, 160, 0.07), 0 1px 0 rgba(255, 255, 255, 0.95) inset;
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.28));
}

.elderly-archive__hero--filter {
  margin-bottom: 18px;
}

.elderly-archive__hero-inner {
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

.elderly-archive__hero-inner--filter {
  flex-direction: column;
  align-items: stretch;
  gap: 0;
  padding-bottom: 16px;
}

.elderly-archive__page-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--yi-brand, #365062);
  line-height: 1.25;
}

.elderly-archive__subtitle {
  margin: 0;
  max-width: 560px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--yi-text-muted, #6d8aa0);
}

.elderly-archive__hero-action {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  flex-shrink: 0;
}

.elderly-archive__btn-primary {
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

.elderly-archive__btn-secondary {
  border-radius: 10px;
  padding: 10px 18px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.35));
  color: var(--yi-text-body, #415a6e);
}

.elderly-archive__btn-secondary:hover {
  border-color: var(--yi-accent-deep, #6eb0d4);
  color: var(--yi-brand, #365062);
  background: #fff;
}

.elderly-archive__filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.elderly-archive__search {
  width: 220px;
  max-width: 100%;
  flex: 1;
  min-width: 160px;
}

.elderly-archive__panel {
  border-radius: 14px;
  border: 1px solid var(--yi-card-border, rgba(130, 170, 205, 0.28));
  background: var(--yi-card-surface, #fdfeff);
  box-shadow: 0 4px 20px var(--yi-card-shadow, rgba(65, 110, 150, 0.08));
  overflow: hidden;
}

.elderly-archive__panel :deep(.el-card__header) {
  padding: 14px 20px;
  background: var(--yi-panel-header-bg, linear-gradient(180deg, #f9fcfe 0%, #eef4fa 100%));
  border-bottom: 1px solid var(--yi-panel-header-border, rgba(145, 185, 215, 0.38));
}

.elderly-archive__panel :deep(.el-card__body) {
  padding: 0 0 12px;
  width: 100%;
  box-sizing: border-box;
}

.el-table {
  --el-table-row-height: 42px;
  font-size: 13px;
  width: 100%;
}

.el-table :deep(.el-table__header-wrapper) {
  background: linear-gradient(180deg, #f8fafc 0%, #f0f5f9 100%);
}

.el-table :deep(.el-table__header tr) {
  height: 44px;
}

.el-table :deep(.el-table__header th) {
  padding: 8px 6px;
  font-weight: 600;
  color: #3d5265;
  font-size: 13px;
  background: transparent;
  border-bottom: 2px solid #e1e9f0;
}

.el-table :deep(.el-table__body td) {
  padding: 8px 6px;
  color: #4a5f73;
  border-bottom: 1px solid #f0f4f8;
}

.el-table :deep(.el-table__row:hover) {
  background: rgba(168, 212, 234, 0.12);
}

.el-table :deep(.el-table__row--striped) {
  background: rgba(245, 249, 252, 0.6);
}

.elderly-archive__panel-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.elderly-archive__panel-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--yi-brand, #365062);
}

.elderly-archive__panel-icon {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #b8def0, var(--yi-accent-deep, #6eb0d4));
  box-shadow: 0 0 0 3px var(--yi-accent-soft-glow, rgba(110, 176, 212, 0.24));
}

.elderly-archive__panel-meta {
  font-size: 13px;
  color: var(--yi-text-muted, #6d8aa0);
}

.elderly-archive__table :deep(.el-table__header th) {
  font-weight: 600;
  font-size: 13px;
  color: var(--yi-brand, #365062);
  background: var(--yi-table-header-bg, linear-gradient(180deg, #f2f7fc 0%, #e6f0fa 100%)) !important;
}

.elderly-archive__table :deep(.el-table__body td) {
  padding: 8px 0;
}

.elderly-archive__table :deep(.el-table__header .cell),
.elderly-archive__table :deep(.el-table__body .cell) {
  white-space: nowrap;
  padding-left: 10px;
  padding-right: 10px;
}

.elderly-archive__table :deep(.el-table__row--striped td) {
  background: var(--yi-table-row-striped, rgba(246, 250, 254, 0.96)) !important;
}

.elderly-archive__table :deep(.el-table__row:hover > td) {
  background-color: var(--yi-table-row-hover, rgba(205, 232, 248, 0.48)) !important;
}

.elderly-archive__name {
  font-weight: 500;
  color: var(--yi-brand, #365062);
}

.elderly-archive__dialog-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.elderly-archive__dialog-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--yi-brand, #365062);
}

.elderly-archive__dialog-hint {
  font-size: 12px;
  color: var(--yi-text-muted, #6d8aa0);
}

.elderly-archive__dialog-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--yi-text-body, #415a6e);
}

.elderly-archive__form-full {
  width: 100%;
}

.elderly-archive__filter-select {
  width: 160px;
}

.elderly-archive__pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 4px;
}

.elderly-archive__filter-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  padding-left: 2px;
}

.elderly-archive__result-count {
  font-size: 13px;
  color: #64748b;
}

.elderly-archive__filter-tag {
  font-size: 13px;
}

@media (max-width: 768px) {
  .elderly-archive {
    padding: 14px 14px 24px;
  }

  .elderly-archive__hero-action {
    width: 100%;
    justify-content: flex-start;
  }

  .elderly-archive__filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .elderly-archive__search {
    width: 100%;
  }

  .elderly-archive__panel-meta {
    width: 100%;
    text-align: left;
  }
}
</style>

<style>
.elderly-archive__dialog.el-dialog {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(140, 180, 215, 0.42);
  box-shadow: 0 16px 40px rgba(55, 95, 130, 0.13);
}

.elderly-archive__dialog .el-dialog__header {
  padding: 16px 20px 10px;
  margin: 0;
  background: linear-gradient(180deg, #f9fcfe 0%, #e9f3fa 100%);
  border-bottom: 1px solid rgba(145, 185, 215, 0.38);
}

.elderly-archive__dialog .el-dialog__body {
  padding: 18px 20px 8px;
  background: #fdfeff;
}

.elderly-archive__dialog .el-dialog__footer {
  padding: 12px 20px 18px;
  background: #f4f9fc;
  border-top: 1px solid rgba(145, 185, 215, 0.3);
}
</style>
