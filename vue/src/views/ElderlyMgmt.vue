<template>
  <div class="elderly-mgmt">
    <div class="elderly-mgmt__page-header">
      <div class="elderly-mgmt__page-title">老人监测</div>
    </div>

    <!-- ====== 统计卡片 ====== -->
    <div v-if="overview" class="elderly-mgmt__stats">
      <div class="stats-section-label">基础概况</div>
      <el-row :gutter="14" class="stats-row">
        <el-col :xs="12" :sm="8" :md="6">
          <div class="stat-card stat-card--brand">
            <div class="stat-card__value">{{ overview.elderlyTotal }}</div>
            <div class="stat-card__label">总老人数</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="6">
          <div class="stat-card stat-card--warn">
            <div class="stat-card__value">{{ overview.pendingAlertTotal }}</div>
            <div class="stat-card__label">待处理事件</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="6">
          <div class="stat-card stat-card--danger">
            <div class="stat-card__value">{{ overview.criticalPendingTotal }}</div>
            <div class="stat-card__label">紧急未关闭</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="6">
          <div class="stat-card stat-card--neutral">
            <div class="stat-card__value">{{ overview.newAlertsLast7Days }}</div>
            <div class="stat-card__label">近7日新增事件</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 搜索筛选区域 -->
    <div class="elderly-mgmt__search-panel">
      <div class="elderly-mgmt__search-row">
        <div class="elderly-mgmt__search-field">
          <el-input
              v-model="searchKeyword"
              placeholder="输入老人编号或姓名检索"
              clearable
              :prefix-icon="Search"
              class="elderly-mgmt__search-input"
              @keyup.enter="handleSearch"
          />
        </div>
        <div class="elderly-mgmt__search-field">
          <el-select
              v-model="selectedBuilding"
              placeholder="选择楼栋筛选"
              clearable
              class="elderly-mgmt__building-select"
              @change="handleBuildingChange"
          >
            <el-option v-for="b in buildings" :key="b" :label="b" :value="b"/>
          </el-select>
        </div>
        <el-button type="primary" :icon="Search" class="elderly-mgmt__search-btn" @click="handleSearch">搜索</el-button>
        <el-button class="elderly-mgmt__reset-btn" :icon="RefreshLeft" @click="handleReset">重置</el-button>
        <el-button v-if="selectedBuilding" type="warning" :icon="DataAnalysis" class="elderly-mgmt__report-btn" @click="showBuildingReport">楼栋报表</el-button>
      </div>
      <div class="elderly-mgmt__risk-filter">
        <span class="elderly-mgmt__risk-filter-label">风险等级:</span>
        <el-radio-group v-model="selectedRiskLevel" @change="handleRiskLevelChange" size="default">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="HIGH">
            <span class="elderly-mgmt__risk-dot elderly-mgmt__risk-dot--high"></span>
            高危
          </el-radio-button>
          <el-radio-button label="MEDIUM">
            <span class="elderly-mgmt__risk-dot elderly-mgmt__risk-dot--medium"></span>
            中危
          </el-radio-button>
          <el-radio-button label="LOW">
            <span class="elderly-mgmt__risk-dot elderly-mgmt__risk-dot--low"></span>
            低危
          </el-radio-button>
        </el-radio-group>
      </div>
      <div class="elderly-mgmt__search-meta">
        <span class="elderly-mgmt__result-count">共 {{ filteredElders.length }} 位老人</span>
        <span v-if="selectedBuilding" class="elderly-mgmt__filter-tag">
          <el-tag type="primary" size="small" effect="light">{{ selectedBuilding }}</el-tag>
        </span>
        <el-button
            :loading="refreshing"
            size="small"
            @click="handleRefresh"
            class="elderly-mgmt__refresh-btn-inline"
        >
          <el-icon><Refresh/></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button
            type="success"
            size="small"
            :icon="DataAnalysis"
            :loading="assessLoading"
            @click="runAssessAll"
        >
          一键评估
        </el-button>
      </div>
    </div>

    <!-- 楼栋报表面板 -->
    <el-dialog
        v-model="reportVisible"
        width="1200px"
        class="elderly-mgmt__report-dialog"
        destroy-on-close
        top="3vh"
    >
      <template #header>
        <div class="elderly-mgmt__report-header">
          <span>{{ selectedBuilding + ' 楼栋报表' }}</span>
        </div>
      </template>
      <div class="elderly-mgmt__report-search">
        <el-input
            v-model="reportSearchKeyword"
            placeholder="搜索姓名或房间号"
            clearable
            :prefix-icon="Search"
            class="elderly-mgmt__report-search-input"
        />
        <el-button class="elderly-mgmt__btn-export" :icon="Download" @click="exportBuildingReport">导出报表</el-button>
        <span class="elderly-mgmt__report-search-hint">
          筛选后：{{ filteredHighRisk.length + filteredMediumRisk.length + filteredLowRisk.length }} 人
        </span>
      </div>

      <div class="elderly-mgmt__report-summary">
        <el-row :gutter="16">
          <el-col :span="4">
            <div class="elderly-mgmt__report-stat">
              <div class="elderly-mgmt__report-stat-value">{{ buildingStats.total }}</div>
              <div class="elderly-mgmt__report-stat-label">老人总数</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="elderly-mgmt__report-stat elderly-mgmt__report-stat--danger">
              <div class="elderly-mgmt__report-stat-value">{{ buildingStats.highRisk }}</div>
              <div class="elderly-mgmt__report-stat-label">高危老人</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="elderly-mgmt__report-stat elderly-mgmt__report-stat--warning">
              <div class="elderly-mgmt__report-stat-value">{{ buildingStats.mediumRisk }}</div>
              <div class="elderly-mgmt__report-stat-label">中危老人</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="elderly-mgmt__report-stat elderly-mgmt__report-stat--success">
              <div class="elderly-mgmt__report-stat-value">{{ buildingStats.lowRisk }}</div>
              <div class="elderly-mgmt__report-stat-label">低危老人</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="elderly-mgmt__report-stat elderly-mgmt__report-stat--info">
              <div class="elderly-mgmt__report-stat-value">{{ buildingStats.online }}</div>
              <div class="elderly-mgmt__report-stat-label">设备在线</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="elderly-mgmt__report-stat">
              <div class="elderly-mgmt__report-stat-value">{{ buildingStats.offline }}</div>
              <div class="elderly-mgmt__report-stat-label">设备离线</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <div class="elderly-mgmt__report-sections">
        <div class="elderly-mgmt__report-section">
          <div class="elderly-mgmt__report-section-header">
            <span class="elderly-mgmt__report-section-title">
              <el-tag type="danger" effect="light" round>高危老人</el-tag>
              <span class="elderly-mgmt__report-section-count">{{ highRiskElders.length }} 人</span>
            </span>
            <el-button v-if="highRiskElders.length > 0" size="small" type="danger" @click="batchHandleHighRisk">批量处理</el-button>
          </div>
          <el-table :data="filteredHighRisk" size="small" stripe empty-text="暂无高危老人">
            <el-table-column prop="id" label="ID" width="60" align="center"/>
            <el-table-column prop="realName" label="姓名" width="100"/>
            <el-table-column prop="room" label="房间" min-width="140" show-overflow-tooltip/>
            <el-table-column label="护理等级" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getNursingTagType(row.nursingLevel)" size="small" effect="plain">{{ getNursingLabel(row.nursingLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险因素" min-width="150">
              <template #default="{ row }">
                <span class="elderly-mgmt__risk-factor">{{ row.riskFactors || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="心率" width="80" align="center">
              <template #default="{ row }">{{ formatInt(row.heartRate) }}</template>
            </el-table-column>
            <el-table-column label="呼吸" width="80" align="center">
              <template #default="{ row }">{{ formatInt(row.breathingRate) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="openElderDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="elderly-mgmt__report-section">
          <div class="elderly-mgmt__report-section-header">
            <span class="elderly-mgmt__report-section-title">
              <el-tag type="warning" effect="light" round>中危老人</el-tag>
              <span class="elderly-mgmt__report-section-count">{{ mediumRiskElders.length }} 人</span>
            </span>
          </div>
          <el-table :data="filteredMediumRisk" size="small" stripe empty-text="暂无中危老人">
            <el-table-column prop="id" label="ID" width="60" align="center"/>
            <el-table-column prop="realName" label="姓名" width="100"/>
            <el-table-column prop="room" label="房间" min-width="140" show-overflow-tooltip/>
            <el-table-column label="护理等级" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getNursingTagType(row.nursingLevel)" size="small" effect="plain">{{ getNursingLabel(row.nursingLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险因素" min-width="150">
              <template #default="{ row }">
                <span class="elderly-mgmt__risk-factor">{{ row.riskFactors || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="心率" width="80" align="center">
              <template #default="{ row }">{{ formatInt(row.heartRate) }}</template>
            </el-table-column>
            <el-table-column label="呼吸" width="80" align="center">
              <template #default="{ row }">{{ formatInt(row.breathingRate) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="openElderDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="elderly-mgmt__report-section">
          <div class="elderly-mgmt__report-section-header">
            <span class="elderly-mgmt__report-section-title">
              <el-tag type="success" effect="light" round>低危老人</el-tag>
              <span class="elderly-mgmt__report-section-count">{{ lowRiskElders.length }} 人</span>
            </span>
          </div>
          <el-table :data="filteredLowRisk" size="small" stripe empty-text="暂无低危老人">
            <el-table-column prop="id" label="ID" width="60" align="center"/>
            <el-table-column prop="realName" label="姓名" width="100"/>
            <el-table-column prop="room" label="房间" min-width="140" show-overflow-tooltip/>
            <el-table-column label="护理等级" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getNursingTagType(row.nursingLevel)" size="small" effect="plain">{{ getNursingLabel(row.nursingLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险因素" min-width="150">
              <template #default="{ row }">
                <span class="elderly-mgmt__risk-factor">{{ row.riskFactors || '健康状态良好' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="心率" width="80" align="center">
              <template #default="{ row }">{{ formatInt(row.heartRate) }}</template>
            </el-table-column>
            <el-table-column label="呼吸" width="80" align="center">
              <template #default="{ row }">{{ formatInt(row.breathingRate) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="openElderDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- 老人卡片列表 -->
    <div class="elderly-mgmt__cards-container" v-loading="loading">
      <div v-if="filteredElders.length === 0 && !loading" class="elderly-mgmt__empty">
        <el-empty description="暂无符合条件的老人数据"/>
      </div>
      <div v-else class="elderly-mgmt__cards-grid">
        <div
            v-for="elder in pagedElders"
            :key="elder.id"
            class="elderly-mgmt__elder-card"
        >
          <div class="elderly-mgmt__card-header">
            <div class="elderly-mgmt__card-avatar">
              <span class="elderly-mgmt__avatar-text">{{ elder.realName?.charAt(0) || '老' }}</span>
            </div>
            <div class="elderly-mgmt__card-info">
              <div class="elderly-mgmt__card-name">{{ elder.realName || elder.name || '未知' }}</div>
              <div class="elderly-mgmt__card-id">ID: {{ elder.id }}</div>
            </div>
            <div class="elderly-mgmt__card-status">
              <el-tag :type="getRiskTagType(elder.riskLevel)" size="default" effect="dark" round>
                {{ getRiskLabel(elder.riskLevel) }}
              </el-tag>
            </div>
          </div>
          <div class="elderly-mgmt__card-body">
            <div class="elderly-mgmt__card-row">
              <span class="elderly-mgmt__card-label">护理等级：</span>
              <span class="elderly-mgmt__card-value">
                <el-tag :type="getNursingTagType(elder.nursingLevel)" size="small" effect="plain">
                  {{ getNursingLabel(elder.nursingLevel) }}
                </el-tag>
              </span>
            </div>
            <div class="elderly-mgmt__card-row">
              <span class="elderly-mgmt__card-label">健康评分：</span>
              <span class="elderly-mgmt__card-value">
                <span :class="getScoreClass(elder.healthScore)">{{ elder.healthScore != null ? elder.healthScore + '分' : '—' }}</span>
              </span>
            </div>
            <div class="elderly-mgmt__card-row">
              <span class="elderly-mgmt__card-label">房间：</span>
              <span class="elderly-mgmt__card-value">{{ elder.room || elder.address || '—' }}</span>
            </div>
            <div class="elderly-mgmt__card-row">
              <span class="elderly-mgmt__card-label">睡眠状态：</span>
              <span class="elderly-mgmt__card-value">{{ getSleepStatusLabel(elder.sleepStatus) }}</span>
            </div>
            <div class="elderly-mgmt__card-row">
              <span class="elderly-mgmt__card-label">在床状态：</span>
              <span class="elderly-mgmt__card-value">
                <el-tag :type="getOnBedStatusTagType(elder.onBedStatus)" size="small" effect="plain">
                  {{ getOnBedStatusLabel(elder.onBedStatus) }}
                </el-tag>
              </span>
            </div>
            <div v-if="elder.highPriority" class="elderly-mgmt__card-row elderly-mgmt__card-row--priority">
              <el-tag type="danger" size="small" effect="dark">⚠️ 高监护优先级</el-tag>
            </div>
          </div>
          <div class="elderly-mgmt__card-stats">
            <div class="elderly-mgmt__stat-item">
              <span class="elderly-mgmt__stat-icon">❤️</span>
              <span class="elderly-mgmt__stat-value">{{ formatInt(elder.heartRate) }}</span>
            </div>
            <div class="elderly-mgmt__stat-item">
              <span class="elderly-mgmt__stat-icon">💨</span>
              <span class="elderly-mgmt__stat-value">{{ formatInt(elder.breathingRate) }}</span>
            </div>
            <div class="elderly-mgmt__stat-item">
              <span class="elderly-mgmt__stat-icon">😴</span>
              <span class="elderly-mgmt__stat-value">{{ formatInt(elder.sleepScore) }}</span>
            </div>
          </div>
          <div class="elderly-mgmt__card-footer">
            <el-button type="primary" size="small" link @click.stop="openElderDetail(elder)">查看详情</el-button>
          </div>
        </div>
      </div>
      <div class="elderly-mgmt__pagination">
        <el-pagination
            v-if="filteredElders.length > 0"
            v-model:page-size="pageSize"
            :page-sizes="[15, 30, 45]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="filteredElders.length"
            v-model:current-page="currentPage"
        />
      </div>
    </div>

    <!-- 老人详情弹窗 -->
    <el-dialog
        v-model="detailVisible"
        :title="currentElder?.realName + ' - 监测详情'"
        width="800px"
        class="elderly-mgmt__detail-dialog"
        destroy-on-close
        @opened="onDetailDialogOpened"
        @closed="onDetailDialogClosed"
    >
      <template v-if="currentElder">
        <div class="elderly-mgmt__detail-header">
          <div class="elderly-mgmt__detail-avatar">
            <span>{{ currentElder.realName?.charAt(0) || '老' }}</span>
          </div>
          <div class="elderly-mgmt__detail-info">
            <div class="elderly-mgmt__detail-name">{{ currentElder.realName }}</div>
            <div class="elderly-mgmt__detail-meta">
              <span>ID: {{ currentElder.id }}</span>
              <span>{{ currentElder.gender }} · {{ currentElder.age }}岁</span>
              <span>{{ currentElder.room || currentElder.address }}</span>
            </div>
          </div>
        </div>

        <el-row :gutter="12" class="elderly-mgmt__detail-stats">
          <el-col :span="6">
            <div class="elderly-mgmt__detail-stat elderly-mgmt__detail-stat--temp">
              <div class="elderly-mgmt__detail-stat-label">温度</div>
              <div class="elderly-mgmt__detail-stat-value">{{ latestEnv.temperature != null ? latestEnv.temperature.toFixed(1) + '℃' : '—' }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="elderly-mgmt__detail-stat elderly-mgmt__detail-stat--hum">
              <div class="elderly-mgmt__detail-stat-label">湿度</div>
              <div class="elderly-mgmt__detail-stat-value">{{ latestEnv.humidity != null ? latestEnv.humidity.toFixed(0) + '%' : '—' }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="elderly-mgmt__detail-stat elderly-mgmt__detail-stat--heart">
              <div class="elderly-mgmt__detail-stat-label">心率</div>
              <div class="elderly-mgmt__detail-stat-value">{{ formatInt(latestHealth.heartRate) }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="elderly-mgmt__detail-stat elderly-mgmt__detail-stat--breath">
              <div class="elderly-mgmt__detail-stat-label">呼吸</div>
              <div class="elderly-mgmt__detail-stat-value">{{ formatInt(latestHealth.breathingRate) }}</div>
            </div>
          </el-col>
        </el-row>

        <el-card class="elderly-mgmt__detail-panel" shadow="never">
          <template #header>
            <div class="elderly-mgmt__panel-head">
              <div class="elderly-mgmt__panel-head-left">
                <span class="elderly-mgmt__panel-title">异常情况统计</span>
                <span class="elderly-mgmt__stats-badge">共 {{ alertStats.total }} 条 · {{ alertStats.unresolved }} 条待处理</span>
              </div>
              <el-radio-group v-model="alertStatsRange" size="small" @change="loadAlertStats">
                <el-radio-button value="1">1天</el-radio-button>
                <el-radio-button value="7">1周</el-radio-button>
                <el-radio-button value="30">1月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div v-if="alertStatsTable.length === 0" class="elderly-mgmt__stats-empty">暂无异常事件</div>
          <div v-else class="elderly-mgmt__alert-stats">
            <div v-for="item in alertStatsTable" :key="item.type" class="elderly-mgmt__alert-stat-item" @click="goToAlerts(item.type)">
              <div class="elderly-mgmt__alert-stat-icon">
                <span>{{ item.icon }}</span>
              </div>
              <div class="elderly-mgmt__alert-stat-info">
                <span class="elderly-mgmt__alert-stat-type">{{ item.label }}</span>
                <span class="elderly-mgmt__alert-stat-count">{{ item.count }} 次</span>
              </div>
              <el-icon class="elderly-mgmt__alert-stat-arrow"><ArrowRight/></el-icon>
            </div>
          </div>
        </el-card>

        <el-card class="elderly-mgmt__detail-panel" shadow="never">
          <template #header>
            <div class="elderly-mgmt__panel-head">
              <span class="elderly-mgmt__panel-title">温湿度曲线</span>
              <span style="font-size:12px;color:#94a3b8;margin-left:8px">最近24小时</span>
            </div>
          </template>
          <div ref="envChart" class="elderly-mgmt__detail-chart"></div>
        </el-card>

        <el-card class="elderly-mgmt__detail-panel" shadow="never">
          <template #header>
            <div class="elderly-mgmt__panel-head">
              <span class="elderly-mgmt__panel-title">健康数据（最近30条）</span>
            </div>
          </template>
          <el-table :data="healthRows" size="small" max-height="280" stripe empty-text="暂无健康数据">
            <el-table-column prop="metricType" label="类型" width="100">
              <template #default="{ row }">{{ metricLabel(row.metricType) }}</template>
            </el-table-column>
            <el-table-column label="数值" min-width="80">
              <template #default="{ row }">
                {{ formatMetricValue(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="unit" label="单位" width="60" align="center"/>
            <el-table-column prop="recordTime" label="采集时间" width="160">
              <template #default="{ row }">{{ formatTime(row.recordTime) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </el-dialog>

    <!-- 一键评估结果 -->
    <el-dialog v-model="assessVisible" title="一键评估结果" width="960px" destroy-on-close>
      <div v-if="assessResult" class="elderly-mgmt__assess-summary">
        <el-tag type="info" size="large" effect="light">共评估 {{ assessResult.total }} 人</el-tag>
        <el-tag type="danger" size="large" effect="light">高危 {{ assessResult.highCount }} 人</el-tag>
        <el-tag type="warning" size="large" effect="light">中危 {{ assessResult.mediumCount }} 人</el-tag>
        <el-tag type="success" size="large" effect="light">低危 {{ assessResult.lowCount }} 人</el-tag>
        <span class="elderly-mgmt__assess-time">评估时间：{{ formatTime(assessResult.evaluatedAt) }}</span>
      </div>
      <el-table :data="assessResult ? assessResult.items : []" size="small" max-height="440" stripe style="margin-top:12px">
        <el-table-column type="index" label="#" width="48" align="center"/>
        <el-table-column prop="name" label="姓名" width="90"/>
        <el-table-column prop="room" label="房间" width="90"/>
        <el-table-column label="风险等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getRiskTagType(row.riskLevel)" size="small" effect="dark">{{ getRiskLabel(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="健康评分" width="90" align="center">
          <template #default="{ row }">
            <span :class="getScoreClass(row.healthScore)">{{ row.healthScore }}分</span>
          </template>
        </el-table-column>
        <el-table-column label="护理等级" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="getAssessNursingTagType(row.nursingLevel)" size="small" effect="plain">
              {{ getAssessNursingLabel(row.nursingLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="heartRate" label="心率" width="70" align="center"/>
        <el-table-column prop="breathingRate" label="呼吸" width="70" align="center"/>
        <el-table-column prop="sleepStatus" label="睡眠状态" min-width="90" align="center"/>
        <el-table-column prop="openAlertCount" label="未处理事件" width="90" align="center"/>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="assessVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import * as echarts from 'echarts'
import { Loading, Refresh, Search, RefreshLeft, DataAnalysis, ArrowRight, Download } from '@element-plus/icons-vue'

const METRIC_LABELS = {
  HEART_RATE: '心率',
  BLOOD_PRESSURE: '血压',
  MOTION_INDEX: '体动次数',
  TEMPERATURE: '体温',
  BREATHING_RATE: '呼吸',
  SLEEP_SCORE: '睡眠评分',
  SLEEP_STATUS: '睡眠状态',
  ON_BED_STATUS: '在床状态'
}

const NURSING_LEVEL_LABELS = {
  LEVEL_1: '特别护理',
  LEVEL_2: '重点护理',
  LEVEL_3: '一般护理',
  LEVEL_4: '自理',
  // 兼容库中历史中文值
  '特别护理': '特别护理',
  '重点护理': '重点护理',
  '一般护理': '一般护理',
  '自理': '自理'
}

const RISK_LEVEL_LABELS = {
  '高危': '高危',
  '中危': '中危',
  '低危': '低危',
  'NONE': '无数据',
  'HIGH': '高危',
  'MEDIUM': '中危',
  'LOW': '低危'
}

export default {
  name: 'ElderlyMgmt',
  components: { Loading, Refresh, Search, RefreshLeft, DataAnalysis, ArrowRight, Download },
  data() {
    return {
      overview: null,
      elderlyList: [],
      refreshing: false,
      elders: [],
      filteredElders: [],
      currentPage: 1,
      pageSize: 15,
      buildings: [],
      searchKeyword: '',
      selectedBuilding: '',
      selectedRiskLevel: '',
      loading: false,
      detailVisible: false,
      reportVisible: false,
      currentElder: null,
      envChart: null,
      healthRows: [],
      latestEnv: { temperature: null, humidity: null, recordTime: null },
      latestHealth: { heartRate: null, breathingRate: null },
      alertStats: { total: 0, unresolved: 0, byType: {} },
      alertStatsRange: '1',
      buildingStats: { total: 0, highRisk: 0, mediumRisk: 0, lowRisk: 0, online: 0, offline: 0 },
      highRiskElders: [],
      mediumRiskElders: [],
      lowRiskElders: [],
      reportSearchKeyword: '',
      assessLoading: false,
      assessVisible: false,
      assessResult: null,
      dataRefreshTimer: null,
      detailRefreshTimer: null
    }
  },
  computed: {
    filteredHighRisk() {
      return this.filterReportList(this.highRiskElders)
    },
    filteredMediumRisk() {
      return this.filterReportList(this.mediumRiskElders)
    },
    filteredLowRisk() {
      return this.filterReportList(this.lowRiskElders)
    },
    alertStatsTable() {
      const byType = this.alertStats.byType || {}
      const categories = [
        { key: 'SMOKE', label: '烟雾报警', icon: '🔥', types: ['SMOKE', 'SMOKE_ALERT', 'SMOKE_DETECTED', 'FIRE', 'GAS', '烟雾报警', '燃气报警', '火灾报警'] },
        { key: 'PRESSURE', label: '情绪低落', icon: '💓', types: ['PRESSURE', 'PRESSURE_ALERT', 'BED_PRESSURE', 'PRESSURE_WARN', '压力告警', '床压告警', '情绪低落'] },
        { key: 'EMERGENCY', label: '一键求助', icon: '🔔', types: ['EMERGENCY', 'SOS', 'SOS_CALL', 'CALL_HELP', 'BUTTON_PRESS', '一键求助', '紧急按钮', '紧急求助'] },
        { key: 'FALL', label: '摔倒报警', icon: '🆘', types: ['FALL', 'FALL_DETECTED', 'SOS_FALL', 'FALL_DETECT', 'FALL_ALERT', '跌倒', '跌倒告警', '摔倒报警', '摔倒检测', '跌倒检测'] }
      ]
      return categories.map(cat => {
        let count = 0
        cat.types.forEach(t => {
          if (byType[t]) count += byType[t]
        })
        return { type: cat.key, label: cat.label, icon: cat.icon, count }
      })
    },
    pagedElders() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.filteredElders.slice(start, start + this.pageSize)
    },
    riskClassifiedElders() {
      const high = []
      const medium = []
      const low = []
      
      this.filteredElders.forEach(elder => {
        const riskLevel = (elder.riskLevel || 'LOW').toLowerCase()
        if (riskLevel === 'high') {
          high.push(elder)
        } else if (riskLevel === 'medium') {
          medium.push(elder)
        } else {
          low.push(elder)
        }
      })

      return { high, medium, low }
    }
  },
  beforeUnmount() {
    if (this.envChart) {
      this.envChart.dispose()
      this.envChart = null
    }
  },
  created() {
    this.loadOverview()
    this.loadElderlyList()
  },
  methods: {
    getScoreClass(score) {
      if (score == null) return ''
      if (score >= 80) return 'score--good'
      if (score >= 60) return 'score--medium'
      return 'score--bad'
    },
    loadOverview() {
      return request.get('/api/community/overview').then(res => {
        if (res.code === '200') this.overview = res.data || null
      }).catch(() => this.$message.error('概览加载失败'))
    },
    loadElderlyList() {
      this.loading = true
      // 并行请求：老人列表 + 批量概览数据（一次请求返回所有老人的环境+健康+设备状态）
      return Promise.all([
        request.get('/api/elderly/list'),
        request.get('/api/elderly/batch-overview')
      ]).then(([listRes, overviewRes]) => {
        if (listRes.code === '200') {
          const newElders = listRes.data || []

          // 提取楼栋
          const buildingSet = new Set()
          newElders.forEach(e => {
            const room = e.room || e.address || ''
            const newMatch = room.match(/^(\d+号楼)/)
            if (newMatch) {
              buildingSet.add(newMatch[1])
              return
            }
            const oldMatch = room.match(/^([A-Z])-/)
            if (oldMatch) {
              buildingSet.add(oldMatch[1] + '栋')
            }
          })
          this.buildings = Array.from(buildingSet).sort()

          // 组装批量概览数据
          const overviewMap = new Map()
          if (overviewRes.code === '200' && overviewRes.data) {
            overviewRes.data.forEach(row => {
              overviewMap.set(row.elderlyId, row)
            })
          }

          // 为每个老人填充环境/健康/在线数据
          newElders.forEach(elder => {
            const originalHealthScore = elder.healthScore
            const ov = overviewMap.get(elder.id)
            if (ov) {
              elder.temperature = ov.temperature ?? null
              elder.humidity = ov.humidity ?? null
              elder.recordTime = ov.recordTime ?? null
              elder.heartRate = ov.heartRate ?? null
              elder.breathingRate = ov.breathingRate ?? null
              elder.sleepScore = ov.sleepScore ?? null
              elder.sleepStatus = ov.sleepStatus ?? null
              elder.onBedStatus = ov.onBedStatus ?? null
              elder.online = !!ov.online
            } else {
              elder.temperature = null
              elder.humidity = null
              elder.recordTime = null
              elder.heartRate = null
              elder.breathingRate = null
              elder.sleepScore = null
              elder.sleepStatus = null
              elder.onBedStatus = null
              elder.online = false
            }
            if (originalHealthScore != null) {
              elder.healthScore = originalHealthScore
            }
            if (elder.online && elder.riskFactors) {
              elder.riskFactors = elder.riskFactors
                .split(/\s*,\s*/)
                .filter(f => f !== '设备离线')
                .join(', ') || '健康状态良好'
            } else {
              // 离线或无风险因素的老人，按统一规则现场计算，保证报表风险因素列不为空
              elder.riskFactors = this.calculateRiskLevel(elder).factors
            }
          })

          this.elderlyList = newElders
          this.elders = newElders
          this.sortByRisk(newElders)
          this.filteredElders = [...newElders]
        }
      }).catch(() => this.$message.error('加载老人列表失败'))
      .finally(() => {
        this.loading = false
      })
    },
    handleRefresh() {
      this.refreshing = true
      Promise.all([
        this.loadOverview(),
        this.loadElderlyList()
      ]).finally(() => {
        this.refreshing = false
      })
    },
    runAssessAll() {
      this.$confirm('将基于最新监测数据对全部老人重新评估风险等级与健康评分，结果会保存到老人档案，是否继续？', '一键评估', {
        confirmButtonText: '开始评估',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.assessLoading = true
        request.post('/api/community/assess-all').then(res => {
          if (res.code === '200' && res.data) {
            this.assessResult = res.data
            this.assessVisible = true
            this.$message.success(`已完成 ${res.data.total} 位老人的评估`)
            // 评估结果已写入档案，刷新概览统计与老人卡片
            this.handleRefresh()
          } else {
            this.$message.error(res.msg || '评估失败')
          }
        }).catch(() => this.$message.error('评估请求失败')).finally(() => {
          this.assessLoading = false
        })
      }).catch(() => {})
    },
    getAssessNursingLabel(level) {
      const map = {
        LEVEL_1: '一级护理（特护）',
        LEVEL_2: '二级护理（重点护理）',
        LEVEL_3: '三级护理（一般护理）',
        LEVEL_4: '四级护理（自理）'
      }
      return map[level] || level || '未定级'
    },
    getAssessNursingTagType(level) {
      const map = {
        LEVEL_1: 'danger',
        LEVEL_2: 'warning',
        LEVEL_3: 'primary',
        LEVEL_4: 'success'
      }
      return map[level] || 'info'
    },
    metricLabel(t) {
      return METRIC_LABELS[t] || t || '—'
    },
    formatMetricValue(row) {
      if (!row.metricValue || row.metricValue === '0' || row.metricValue === 0) return '—'
      if (row.metricType === 'SLEEP_STATUS') return this.getSleepStatusLabel(row.metricValue)
      return row.metricValue
    },
    formatTime(timeStr) {
      if (!timeStr) return '—'
      try {
        return timeStr.replace('T', ' ').substring(0, 19)
      } catch {
        return timeStr
      }
    },
    getNursingLabel(level) {
      return NURSING_LEVEL_LABELS[level] || level || '未定级'
    },
    getNursingTagType(level) {
      const map = {
        LEVEL_1: 'danger',
        LEVEL_2: 'warning',
        LEVEL_3: 'primary',
        LEVEL_4: 'success',
        // 兼容库中历史中文值
        '特别护理': 'danger',
        '重点护理': 'warning',
        '一般护理': 'primary',
        '自理': 'success'
      }
      return map[level] || 'info'
    },
    formatInt(val) {
      if (val == null || val === '' || val === '0') return '—'
      const n = Number(val)
      if (isNaN(n)) return '—'
      return Math.round(n)
    },
    getSleepStatusLabel(status) {
      if (status == null || status === '') return '—'
      // 提取中文部分，去掉英文括号内容，如 "清醒 (WAKE)" -> "清醒"
      const chineseMatch = String(status).match(/[\u4e00-\u9fa5]+/)
      if (chineseMatch) return chineseMatch[0]
      // 纯英文映射
      const map = {
        'WAKE': '清醒',
        'AWAKE': '清醒',
        'DEEP': '深睡',
        'DEEP_SLEEP': '深睡',
        'LIGHT': '浅睡',
        'LIGHT_SLEEP': '浅睡',
        'REM': '快速眼动',
        'FALL_ASLEEP': '入睡',
        'SLEEP': '睡眠中',
        'ON_BED': '在床',
        'OFF_BED': '离床'
      }
      return map[String(status).toUpperCase()] || String(status)
    },
    getOnBedStatusLabel(status) {
      if (status == null || status === '') return '—'
      const map = {
        'ON_BED': '在床',
        'OFF_BED': '离床',
        '在床': '在床',
        '离床': '离床',
        '1': '在床',
        '0': '离床'
      }
      return map[String(status)] || status
    },
    getOnBedStatusTagType(status) {
      if (status == null || status === '') return 'info'
      const map = {
        'ON_BED': 'success',
        'OFF_BED': 'warning',
        '在床': 'success',
        '离床': 'warning',
        '1': 'success',
        '0': 'warning'
      }
      return map[String(status)] || 'info'
    },
    getRiskLabel(level) {
      const upperLevel = (level || '').toUpperCase()
      return RISK_LEVEL_LABELS[upperLevel] || level || '未评估'
    },
    getRiskTagType(level) {
      const map = {
        '高危': 'danger',
        '中危': 'warning',
        '低危': 'success',
        '无数据': 'info',
        'HIGH': 'danger',
        'MEDIUM': 'warning',
        'LOW': 'success',
        'NONE': 'info'
      }
      const upperLevel = (level || '').toUpperCase()
      return map[upperLevel] || map[level] || 'info'
    },
    handleSearch() {
      this.filterElders()
    },
    handleBuildingChange() {
      this.filterElders()
    },
    handleReset() {
      this.searchKeyword = ''
      this.selectedBuilding = ''
      this.selectedRiskLevel = ''
      this.filterElders()
    },
    filterElders() {
      let result = [...this.elders]

      if (this.searchKeyword) {
        const keyword = this.searchKeyword.toLowerCase()
        result = result.filter(e => {
          const idMatch = String(e.id).includes(keyword)
          const nameMatch = (e.realName || e.name || '').toLowerCase().includes(keyword)
          return idMatch || nameMatch
        })
      }

      if (this.selectedBuilding) {
        result = result.filter(e => {
          const room = e.room || e.address || ''
          // 新格式：selectedBuilding = "1号楼"，判断是否以 "1号楼" 开头
          if (this.selectedBuilding.includes('号楼')) {
            return room.startsWith(this.selectedBuilding)
          }
          // 旧格式：selectedBuilding = "A栋"，判断是否以 "A-" 开头
          const buildingPrefix = this.selectedBuilding.replace('栋', '')
          return room.startsWith(buildingPrefix + '-')
        })
      }

      if (this.selectedRiskLevel) {
        result = result.filter(e => {
          return (e.riskLevel || 'LOW').toLowerCase() === this.selectedRiskLevel.toLowerCase()
        })
      }

      this.currentPage = 1
      // 按风险等级排序：HIGH > MEDIUM > LOW
      this.sortByRisk(result)
      this.filteredElders = result
    },
    /** 统一的风险排序：高危在前，同级按老人ID升序，保证顺序稳定 */
    sortByRisk(list) {
      const riskOrder = { HIGH: 0, MEDIUM: 1, LOW: 2 }
      list.sort((a, b) => {
        const orderA = riskOrder[(a.riskLevel || 'LOW').toUpperCase()] ?? 2
        const orderB = riskOrder[(b.riskLevel || 'LOW').toUpperCase()] ?? 2
        if (orderA !== orderB) return orderA - orderB
        return a.id - b.id
      })
    },
    handleRiskLevelChange() {
      this.filterElders()
    },
    calculateRiskLevel(elder) {
      const factors = []
      let level = 'low'
      
      const heartRate = elder.heartRate
      if (heartRate != null) {
        if (heartRate < 50 || heartRate > 100) {
          factors.push(`心率异常(${heartRate}次/分)`)
          level = 'high'
        } else if (heartRate < 55 || heartRate > 90) {
          factors.push(`心率偏高/低(${heartRate}次/分)`)
          if (level !== 'high') level = 'medium'
        }
      }
      
      if (!elder.online) {
        factors.push('设备离线')
        if (level !== 'high') level = 'medium'
      }
      
      if (elder.age >= 80) {
        factors.push('高龄老人')
        if (level !== 'high') level = 'medium'
      }
      
      return { level, factors: factors.join(', ') || '健康状态良好' }
    },
    exportBuildingReport() {
      const token = sessionStorage.getItem('token')
      const parts = []
      parts.push('token=' + encodeURIComponent(token || ''))
      if (this.selectedBuilding) parts.push('building=' + encodeURIComponent(this.selectedBuilding))
      if (this.reportSearchKeyword) parts.push('keyword=' + encodeURIComponent(this.reportSearchKeyword))
      const url = '/api/export/building-report?' + parts.join('&')
      window.open(url, '_blank')
    },
    filterReportList(list) {
      if (!this.reportSearchKeyword) return list
      const keyword = this.reportSearchKeyword.toLowerCase()
      return list.filter(e => {
        const name = (e.realName || e.name || '').toLowerCase()
        const room = (e.room || '').toLowerCase()
        const id = String(e.id || '')
        return name.includes(keyword) || room.includes(keyword) || id.includes(keyword)
      })
    },
    showBuildingReport() {
      if (!this.selectedBuilding) return
      
      const classified = this.riskClassifiedElders
      this.highRiskElders = classified.high
      this.mediumRiskElders = classified.medium
      this.lowRiskElders = classified.low
      this.reportSearchKeyword = ''
      
      this.buildingStats = {
        total: this.filteredElders.length,
        highRisk: classified.high.length,
        mediumRisk: classified.medium.length,
        lowRisk: classified.low.length,
        online: this.filteredElders.filter(e => e.online).length,
        offline: this.filteredElders.filter(e => !e.online).length
      }
      
      this.reportVisible = true
    },
    batchHandleHighRisk() {
      if (this.highRiskElders.length === 0) {
        this.$message.warning('暂无高危老人需要处理')
        return
      }
      
      this.$confirm(`确定要对 ${this.highRiskElders.length} 位高危老人进行批量处理吗？`, '批量处理确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$router.push('/alerts')
        this.$message.success('请前往事件处理页面进行批量处理')
      }).catch(() => {})
    },
    openElderDetail(elder) {
      this.currentElder = elder
      this.detailVisible = true
      this.latestEnv = { temperature: elder.temperature, humidity: elder.humidity, recordTime: elder.recordTime }
      this.latestHealth = { heartRate: elder.heartRate, breathingRate: elder.breathingRate }
    },
    onDetailDialogOpened() {
      this.$nextTick(() => {
        this.loadEnvChart()
        this.loadHealthData()
        this.loadAlertStats()
        // 启动详情独立定时刷新（1.5秒一次，温湿度图表+体征表格+告警统计都实时刷新）
        this.stopDetailRefreshTimer()
        this.detailRefreshTimer = setInterval(() => {
          this.refreshDetailContent()
        }, 1500)
      })
    },
    refreshDetailContent() {
      if (!this.currentElder) return
      Promise.all([
        this.loadEnvChart(),
        this.loadHealthData(),
        this.loadAlertStats()
      ]).catch(() => {})
    },
    stopDetailRefreshTimer() {
      if (this.detailRefreshTimer) {
        clearInterval(this.detailRefreshTimer)
        this.detailRefreshTimer = null
      }
    },
    onDetailDialogClosed() {
      this.stopDetailRefreshTimer()
      if (this.envChart) {
        this.envChart.dispose()
        this.envChart = null
      }
    },
    loadEnvChart() {
      if (!this.currentElder) return
      const end = new Date()
      const start = new Date(end.getTime() - 24 * 60 * 60 * 1000)
      const fmt = d => {
        const pad = n => String(n).padStart(2, '0')
        return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
      }
      request.get(`/api/elderly/${this.currentElder.id}/environment/page`, {
        params: { pageNum: 1, pageSize: 500, start: fmt(start), end: fmt(end) }
      }).then(res => {
        if (res.code !== '200' || !res.data) {
          this.latestEnv = { temperature: null, humidity: null, recordTime: null }
          return
        }
        const records = res.data.records || []
        if (records.length) {
          const last = records[0]
          this.latestEnv = {
            temperature: last.temperature,
            humidity: last.humidity,
            recordTime: last.recordTime
          }
        } else {
          // 没有数据时清除旧数据，显示 —
          this.latestEnv = { temperature: null, humidity: null, recordTime: null }
        }
        let sorted = [...records].sort((a, b) => new Date(a.recordTime) - new Date(b.recordTime))
        const MAX_POINTS = 200
        if (sorted.length > MAX_POINTS) {
          const step = Math.ceil(sorted.length / MAX_POINTS)
          sorted = sorted.filter((_, i) => i % step === 0)
        }
        const times = sorted.map(r => r.recordTime)
        const temp = sorted.map(r => r.temperature)
        const hum = sorted.map(r => r.humidity)
        if (times.length === 0) {
          // 无数据时清空图表
          if (this.envChart) {
            this.envChart.clear()
          }
        } else {
          this.renderEnvChart(times, temp, hum)
        }
      }).catch(() => this.$message.error('加载环境数据失败'))
    },
    loadHealthData() {
      if (!this.currentElder) return
      request.get(`/api/elderly/${this.currentElder.id}/health/page`, {
        params: { pageNum: 1, pageSize: 30 }
      }).then(res => {
        if (res.code === '200' && res.data) {
          this.healthRows = res.data.records || []
        }
      }).catch(() => this.$message.error('加载健康数据失败'))
    },
    loadAlertStats() {
      if (!this.currentElder) return
      const days = this.alertStatsRange || '30'
      request.get(`/api/alerts/stats/${this.currentElder.id}`, {
        params: { days }
      }).then(res => {
        if (res.code === '200' && res.data) {
          this.alertStats = res.data
        }
      }).catch(() => {})
    },
    goToAlerts(type) {
      this.$router.push('/alerts')
    },
    renderEnvChart(times, temp, hum) {
      if (!this.$refs.envChart) return
      if (!this.envChart) {
        this.envChart = echarts.init(this.$refs.envChart)
      }

      // 动态计算温度、湿度的合理Y轴范围（上下留margin，避免曲线挤在边缘）
      const validTemp = (temp || []).filter(v => v != null && !isNaN(parseFloat(v))).map(v => parseFloat(v))
      const validHum = (hum || []).filter(v => v != null && !isNaN(parseFloat(v))).map(v => parseFloat(v))
      const minT = validTemp.length ? Math.min.apply(null, validTemp) : 20
      const maxT = validTemp.length ? Math.max.apply(null, validTemp) : 30
      const minH = validHum.length ? Math.min.apply(null, validHum) : 40
      const maxH = validHum.length ? Math.max.apply(null, validHum) : 70
      const tempMin = Math.max(-10, Math.floor(minT - 3))
      const tempMax = Math.min(60, Math.ceil(maxT + 3))
      const humMin = Math.max(0, Math.floor(minH - 5))
      const humMax = Math.min(100, Math.ceil(maxH + 5))
      // 极端兜底：范围太小（比如所有值一样）时，强制给个合理区间
      const yTempMin = (tempMax - tempMin) < 4 ? (tempMin - 3) : tempMin
      const yTempMax = (tempMax - tempMin) < 4 ? (tempMax + 3) : tempMax
      const yHumMin = (humMax - humMin) < 10 ? (humMin - 5) : humMin
      const yHumMax = (humMax - humMin) < 10 ? (humMax + 5) : humMax

      // 点数过多时不显示每个数据点的圆圈，避免密密麻麻
      const totalPoints = (times || []).length
      const showSymbol = totalPoints <= 40
      const symbolSz = showSymbol ? (totalPoints <= 15 ? 6 : 4) : 0

      this.envChart.setOption({
        color: ['#2563eb', '#0d9488'],
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255,255,255,0.96)',
          borderColor: 'rgba(130, 170, 205, 0.35)',
          textStyle: { color: '#415a6e', fontSize: 12 },
          valueFormatter: function (val, seriesName) {
            if (val == null) return '—'
            if ((seriesName || '').indexOf('温度') >= 0) return parseFloat(val).toFixed(1) + '℃'
            if ((seriesName || '').indexOf('湿度') >= 0) return parseFloat(val).toFixed(1) + '%'
            return val
          }
        },
        legend: {
          data: ['温度℃', '湿度%'],
          top: 4,
          textStyle: { color: '#6d8aa0', fontSize: 12 }
        },
        grid: { left: 52, right: 60, bottom: 52, top: 40 },
        xAxis: {
          type: 'category',
          data: times,
          axisLabel: {
            rotate: 26,
            color: '#6d8aa0',
            fontSize: 11,
            interval: Math.max(0, Math.floor(times.length / 10) - 1),
            formatter: function (val) {
              return val.replace(/^\d{4}-(\d{2}-\d{2})T(\d{2}:\d{2}).*$/, '$1 $2')
            }
          },
          axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.25)' } }
        },
        yAxis: [
          {
            type: 'value',
            name: '温度℃',
            min: yTempMin,
            max: yTempMax,
            position: 'left',
            nameTextStyle: { color: '#2563eb', fontSize: 11 },
            axisLabel: { color: '#2563eb', fontSize: 11, formatter: '{value}' },
            splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.25)', type: 'dashed' } }
          },
          {
            type: 'value',
            name: '湿度%',
            min: yHumMin,
            max: yHumMax,
            position: 'right',
            nameTextStyle: { color: '#0d9488', fontSize: 11 },
            axisLabel: { color: '#0d9488', fontSize: 11, formatter: '{value}' },
            splitLine: { show: false }
          }
        ],
        series: [
          {
            name: '温度℃',
            type: 'line',
            smooth: true,
            yAxisIndex: 0,
            data: temp,
            showSymbol: showSymbol,
            symbolSize: symbolSz,
            lineStyle: { width: 2.5 },
            areaStyle: { color: 'rgba(37, 99, 235, 0.08)' }
          },
          {
            name: '湿度%',
            type: 'line',
            smooth: true,
            yAxisIndex: 1,
            data: hum,
            showSymbol: showSymbol,
            symbolSize: symbolSz,
            lineStyle: { width: 2.5 },
            areaStyle: { color: 'rgba(13, 148, 136, 0.08)' }
          }
        ]
      })
    },
    mounted() {
      this.startDataRefreshTimer()
    },
    beforeDestroy() {
      this.stopDataRefreshTimer()
      this.stopDetailRefreshTimer()
    },
    startDataRefreshTimer() {
      this.stopDataRefreshTimer()
      this.dataRefreshTimer = setInterval(() => {
        Promise.all([
          this.loadOverview(),
          this.loadElderlyList()
        ]).then(() => {
          // 楼栋报表弹窗打开时同步刷新分组与统计
          if (this.reportVisible) {
            const classified = this.riskClassifiedElders
            this.highRiskElders = classified.high
            this.mediumRiskElders = classified.medium
            this.lowRiskElders = classified.low
            this.buildingStats = {
              total: this.filteredElders.length,
              highRisk: classified.high.length,
              mediumRisk: classified.medium.length,
              lowRisk: classified.low.length,
              online: this.filteredElders.filter(e => e.online).length,
              offline: this.filteredElders.filter(e => !e.online).length
            }
          }
        }).catch(() => {})
      }, 2000)
    },
    stopDataRefreshTimer() {
      if (this.dataRefreshTimer) {
        clearInterval(this.dataRefreshTimer)
        this.dataRefreshTimer = null
      }
    }
  }
}
</script>

<style scoped>
.elderly-mgmt {
  padding: 16px 20px 0;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  background: var(--yi-page-gradient, linear-gradient(165deg, #f7fbfe 0%, #eef5fc 48%, #e3eef9 100%));
  overflow: hidden;
}

.elderly-mgmt__page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.elderly-mgmt__page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--yi-brand, #365062);
  letter-spacing: 0.02em;
  margin: 0;
}

.stats-section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--yi-brand, #365062);
  margin-bottom: 8px;
  margin-top: 6px;
  padding-left: 2px;
}

.stats-row {
  margin-bottom: 12px;
}

.elderly-mgmt__stats {
  margin-bottom: 18px;
  flex-shrink: 0;
}

.stat-card {
  border-radius: 12px;
  padding: 16px 18px;
  min-height: 88px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border: 1px solid transparent;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px var(--yi-card-shadow, rgba(65, 110, 150, 0.08));
}

.stat-card--brand {
  background: linear-gradient(145deg, #fff 0%, rgba(168, 212, 234, 0.22) 100%);
  border-color: var(--yi-card-border, rgba(130, 170, 205, 0.28));
}

.stat-card--warn {
  background: linear-gradient(145deg, #fffbf5 0%, rgba(230, 162, 60, 0.12) 100%);
  border-color: rgba(230, 162, 60, 0.25);
}

.stat-card--danger {
  background: linear-gradient(145deg, #fff8f8 0%, rgba(245, 108, 108, 0.12) 100%);
  border-color: rgba(245, 108, 108, 0.25);
}

.stat-card--neutral {
  background: linear-gradient(145deg, #fafcfe 0%, #e8f2f8 100%);
  border-color: rgba(145, 175, 200, 0.28);
}

.stat-card__value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--yi-brand, #365062);
  letter-spacing: -0.02em;
}

.stat-card--warn .stat-card__value {
  color: #c77e0a;
}

.stat-card--danger .stat-card__value {
  color: #dc2626;
}

.stat-card--neutral .stat-card__value {
  color: #507088;
}

.stat-card__label {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
  color: var(--yi-text-muted, #6d8aa0);
  letter-spacing: 0.02em;
}

/* 搜索面板 */
.elderly-mgmt__search-panel {
  margin-bottom: 18px;
  padding: 16px 20px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid rgba(130, 170, 205, 0.28);
  box-shadow: 0 4px 20px rgba(65, 110, 150, 0.08);
  flex-shrink: 0;
}

.elderly-mgmt__search-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.elderly-mgmt__search-field {
  flex: 1;
  min-width: 200px;
}

.elderly-mgmt__search-input {
  width: 100%;
}

.elderly-mgmt__building-select {
  width: 160px;
}

.elderly-mgmt__search-btn {
  border-radius: 8px;
  padding: 8px 20px;
}

.elderly-mgmt__reset-btn {
  border-radius: 8px;
  padding: 8px 16px;
}

.elderly-mgmt__report-btn {
  border-radius: 8px;
  padding: 8px 16px;
}

.elderly-mgmt__search-meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.elderly-mgmt__assess-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.elderly-mgmt__assess-time {
  margin-left: auto;
  font-size: 12px;
  color: #8aa0b3;
}

.elderly-mgmt__risk-filter {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.elderly-mgmt__risk-filter-label {
  font-size: 13px;
  color: #6d8aa0;
  font-weight: 500;
}

.elderly-mgmt__risk-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}

.elderly-mgmt__risk-dot--high { background: #f56c6c; }
.elderly-mgmt__risk-dot--medium { background: #e6a23c; }
.elderly-mgmt__risk-dot--low { background: #67c23a; }

.elderly-mgmt__result-count {
  font-size: 13px;
  color: #6d8aa0;
}

.elderly-mgmt__refresh-btn-inline {
  margin-left: auto;
}

/* 楼栋报表样式 */
.elderly-mgmt__report-dialog :deep(.el-dialog__body) {
  padding: 16px 20px;
  max-height: 82vh;
  overflow-y: auto;
}

.elderly-mgmt__report-search {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.elderly-mgmt__report-search-input {
  width: 280px;
}

.elderly-mgmt__btn-export {
  margin-left: auto;
}

.elderly-mgmt__report-search-hint {
  font-size: 15px;
  color: #6d8aa0;
}

.elderly-mgmt__report-summary {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
}

.elderly-mgmt__report-stat {
  text-align: center;
  padding: 16px 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid rgba(130, 170, 205, 0.28);
}

.elderly-mgmt__report-stat--danger {
  background: linear-gradient(145deg, #fff 0%, rgba(239, 68, 68, 0.08) 100%);
  border-color: rgba(239, 68, 68, 0.2);
}

.elderly-mgmt__report-stat--warning {
  background: linear-gradient(145deg, #fff 0%, rgba(230, 162, 60, 0.08) 100%);
  border-color: rgba(230, 162, 60, 0.2);
}

.elderly-mgmt__report-stat--success {
  background: linear-gradient(145deg, #fff 0%, rgba(103, 194, 58, 0.08) 100%);
  border-color: rgba(103, 194, 58, 0.2);
}

.elderly-mgmt__report-stat--info {
  background: linear-gradient(145deg, #fff 0%, rgba(64, 158, 255, 0.08) 100%);
  border-color: rgba(64, 158, 255, 0.2);
}

.elderly-mgmt__report-stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #365062;
  line-height: 1.2;
}

.elderly-mgmt__report-stat--danger .elderly-mgmt__report-stat-value {
  color: #ef4444;
}

.elderly-mgmt__report-stat--warning .elderly-mgmt__report-stat-value {
  color: #e6a23c;
}

.elderly-mgmt__report-stat--success .elderly-mgmt__report-stat-value {
  color: #67c23a;
}

.elderly-mgmt__report-stat--info .elderly-mgmt__report-stat-value {
  color: #409eff;
}

.elderly-mgmt__report-stat-label {
  font-size: 16px;
  color: #6d8aa0;
  margin-top: 6px;
}

.elderly-mgmt__report-sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.elderly-mgmt__report-section {
  border-radius: 10px;
  background: #fff;
  border: 1px solid rgba(130, 170, 205, 0.28);
  overflow: hidden;
}

.elderly-mgmt__report-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid rgba(130, 170, 205, 0.28);
}

.elderly-mgmt__report-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.elderly-mgmt__report-section-title :deep(.el-tag) {
  font-size: 18px !important;
  padding: 6px 14px !important;
  height: auto !important;
}

.elderly-mgmt__report-section-count {
  font-size: 18px;
  color: #6d8aa0;
}

.elderly-mgmt__risk-factor {
  font-size: 16px;
  color: #606266;
}

.elderly-mgmt__report-dialog :deep(.el-table) {
  font-size: 26px !important;
}

.elderly-mgmt__report-dialog :deep(.el-table th) {
  font-size: 38px !important;
  font-weight: 600;
}

.elderly-mgmt__report-dialog :deep(.el-table td) {
  font-size: 26px !important;
}

.elderly-mgmt__report-dialog :deep(.el-table__cell) {
  padding: 20px 24px !important;
}

.elderly-mgmt__report-dialog :deep(.el-button--small) {
  font-size: 24px !important;
  padding: 8px 16px !important;
}

.elderly-mgmt__report-dialog :deep(.el-tag--small) {
  font-size: 22px !important;
  padding: 4px 12px !important;
  height: auto !important;
}

.elderly-mgmt__report-dialog :deep(.el-table .cell) {
  line-height: 1.4 !important;
}

.elderly-mgmt__report-section-count {
  font-size: 16px;
  color: #6d8aa0;
}

/* 卡片网格 */
.elderly-mgmt__cards-container {
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding-right: 4px;
  margin: 0 -4px 12px;
  padding: 8px 4px 4px;
}

.elderly-mgmt__pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px 8px;
  flex-shrink: 0;
}

.elderly-mgmt__empty {
  padding: 40px 0;
}

.elderly-mgmt__cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  flex: 1;
}

/* 老人卡片 */
.elderly-mgmt__elder-card {
  border-radius: 14px;
  background: #fff;
  border: 1px solid rgba(140, 175, 205, 0.22);
  box-shadow: 0 2px 6px rgba(65, 105, 145, 0.04), 0 1px 2px rgba(65, 105, 145, 0.04);
  cursor: pointer;
  overflow: hidden;
  transition: all 0.22s ease;
}

.elderly-mgmt__elder-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(65, 105, 145, 0.09), 0 2px 4px rgba(65, 105, 145, 0.05);
  border-color: rgba(95, 155, 205, 0.4);
}

.elderly-mgmt__card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px 13px;
  background: #f7fafd;
  border-bottom: 1px solid rgba(145, 185, 215, 0.18);
}

.elderly-mgmt__card-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #75b9dd;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.elderly-mgmt__avatar-text {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
}

.elderly-mgmt__card-info {
  flex: 1;
  min-width: 0;
}

.elderly-mgmt__card-name {
  font-size: 16px;
  font-weight: 600;
  color: #2c4254;
}

.elderly-mgmt__card-id {
  font-size: 12px;
  color: #8fa7bb;
  margin-top: 3px;
}

.elderly-mgmt__card-status {
  flex-shrink: 0;
}

.elderly-mgmt__card-status :deep(.el-tag) {
  font-weight: 600 !important;
  font-size: 12px !important;
}

.elderly-mgmt__card-body {
  padding: 14px 18px 10px;
}

.elderly-mgmt__card-row {
  display: flex;
  align-items: center;
  padding: 5px 0;
  font-size: 13px;
  line-height: 1.6;
}

.elderly-mgmt__card-label {
  color: #7d97ab;
  flex-shrink: 0;
  width: 72px;
  font-size: 13px;
  font-weight: 500;
}

.elderly-mgmt__card-value {
  color: #2f4556;
  font-weight: 600;
  flex: 1;
  min-width: 0;
  word-break: break-all;
  text-align: left;
  padding-left: 6px;
  letter-spacing: 1px;
}

.elderly-mgmt__card-value .el-tag {
  font-weight: 500;
  letter-spacing: 1px;
}

.elderly-mgmt__card-row--priority {
  margin-top: 4px;
}

.score--good,
.score--medium,
.score--bad {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
}

.score--good {
  color: #2ea043;
  background: rgba(46, 160, 67, 0.09);
}

.score--medium {
  color: #c58100;
  background: rgba(214, 140, 0, 0.1);
}

.score--bad {
  color: #cf222e;
  background: rgba(207, 34, 46, 0.09);
}

.elderly-mgmt__card-stats {
  display: flex;
  justify-content: space-between;
  padding: 10px 16px;
  background: #f8fbfe;
  border-top: 1px solid rgba(145, 185, 215, 0.15);
  gap: 10px;
}

.elderly-mgmt__stat-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  flex: 1;
  padding: 8px 4px;
  border-radius: 9px;
  background: #fff;
  border: 1px solid rgba(145, 185, 215, 0.12);
}

.elderly-mgmt__stat-item:nth-child(1) {
  background: #fff9f8;
  border-color: rgba(239, 68, 68, 0.12);
}
.elderly-mgmt__stat-item:nth-child(2) {
  background: #f7fbff;
  border-color: rgba(59, 130, 246, 0.12);
}
.elderly-mgmt__stat-item:nth-child(3) {
  background: #f7fdfa;
  border-color: rgba(22, 163, 74, 0.12);
}

.elderly-mgmt__stat-icon {
  font-size: 13px;
  opacity: 0.9;
}
.elderly-mgmt__stat-item:nth-child(1) .elderly-mgmt__stat-icon {
  color: #e55353;
}
.elderly-mgmt__stat-item:nth-child(2) .elderly-mgmt__stat-icon {
  color: #4080ff;
}
.elderly-mgmt__stat-item:nth-child(3) .elderly-mgmt__stat-icon {
  color: #2db36c;
}

.elderly-mgmt__stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #2f4556;
}

.elderly-mgmt__card-footer {
  padding: 9px 16px 13px;
  background: #f8fbfe;
  border-top: 1px solid rgba(145, 185, 215, 0.12);
  text-align: center;
}

.elderly-mgmt__card-footer .el-button {
  font-weight: 500;
  font-size: 13px;
  color: #4a8fbf;
}

/* 详情弹窗 */
.elderly-mgmt__detail-dialog :deep(.el-dialog__body) {
  padding: 16px 20px;
}

.elderly-mgmt__detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid rgba(145, 185, 215, 0.38);
}

.elderly-mgmt__detail-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a8d4ea 0%, #6eb0d4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.elderly-mgmt__detail-avatar span {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.elderly-mgmt__detail-info {
  flex: 1;
}

.elderly-mgmt__detail-name {
  font-size: 18px;
  font-weight: 700;
  color: #365062;
}

.elderly-mgmt__detail-meta {
  font-size: 13px;
  color: #6d8aa0;
  margin-top: 4px;
  display: flex;
  gap: 12px;
}

.elderly-mgmt__detail-stats {
  margin-bottom: 16px;
}

.elderly-mgmt__detail-stat {
  border-radius: 10px;
  padding: 12px;
  text-align: center;
  background: #f5f7fa;
}

.elderly-mgmt__detail-stat--temp {
  background: linear-gradient(145deg, #fff 0%, rgba(37, 99, 235, 0.08) 100%);
}

.elderly-mgmt__detail-stat--hum {
  background: linear-gradient(145deg, #fff 0%, rgba(13, 148, 136, 0.1) 100%);
}

.elderly-mgmt__detail-stat--heart {
  background: linear-gradient(145deg, #fff 0%, rgba(239, 68, 68, 0.08) 100%);
}

.elderly-mgmt__detail-stat--breath {
  background: linear-gradient(145deg, #fff 0%, rgba(147, 51, 234, 0.08) 100%);
}

.elderly-mgmt__detail-stat-label {
  font-size: 12px;
  color: #6d8aa0;
}

.elderly-mgmt__detail-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #365062;
  margin-top: 4px;
}

.elderly-mgmt__detail-panel {
  margin-bottom: 16px;
  border-radius: 12px;
  border: 1px solid rgba(130, 170, 205, 0.28);
}

.elderly-mgmt__panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.elderly-mgmt__panel-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.elderly-mgmt__stats-badge {
  font-size: 12px;
  color: #6d8aa0;
  background: #f0f5fa;
  padding: 2px 10px;
  border-radius: 20px;
  white-space: nowrap;
}

.elderly-mgmt__panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #365062;
}

.elderly-mgmt__detail-chart {
  height: 280px;
}

/* ========== 异常情况统计卡片样式 ========== */

.elderly-mgmt__stats-empty {
  padding: 32px 0;
  text-align: center;
  color: var(--yi-text-muted, #6d8aa0);
  font-size: 14px;
}

.elderly-mgmt__alert-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 6px 0;
}

.elderly-mgmt__alert-stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 160px;
  padding: 12px 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e8edf2;
  cursor: pointer;
  transition: all 0.2s;
}

.elderly-mgmt__alert-stat-item:hover {
  background: #eef5fc;
  border-color: var(--yi-accent-deep, #6eb0d4);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(70, 140, 190, 0.12);
}

.elderly-mgmt__alert-stat-icon {
  font-size: 24px;
  line-height: 1;
  flex-shrink: 0;
}

.elderly-mgmt__alert-stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.elderly-mgmt__alert-stat-type {
  font-size: 14px;
  font-weight: 600;
  color: var(--yi-brand, #365062);
}

.elderly-mgmt__alert-stat-count {
  font-size: 12px;
  color: var(--yi-text-muted, #6d8aa0);
}

.elderly-mgmt__alert-stat-arrow {
  font-size: 14px;
  color: #b0c8da;
  flex-shrink: 0;
}

.unit {
  font-size: 11px;
  color: var(--yi-text-muted, #6d8aa0);
  margin-left: 2px;
}

@media (max-width: 768px) {
  .elderly-mgmt {
    padding: 12px 12px 20px;
  }

  .elderly-mgmt__search-row {
    flex-direction: column;
    align-items: stretch;
  }

  .elderly-mgmt__search-field {
    min-width: 100%;
  }

  .elderly-mgmt__building-select {
    width: 100%;
  }

  .elderly-mgmt__cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
