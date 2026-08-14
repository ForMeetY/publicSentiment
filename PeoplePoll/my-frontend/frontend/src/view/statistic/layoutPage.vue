<script setup>
import { useRoute } from 'vue-router'
import { getUserInfoDetail,getUserInfoAnalysis } from "@/api/userInfo"
import { getRecommendByUserID } from "@/api/recommend"
import { onMounted, ref, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import deepseekIcon from '../../assets/img/deepseek (2).svg'
import Footer from '@/component/footer.vue'
import recommendListPage from '@/component/recommend/recommendList.vue'
const route = useRoute()
const id = route.query.id
const user = ref(null)

const probChartRef = ref(null)
const radarChartRef = ref(null)

// 动态映射圈层名称字典
const clusterMap = {
  0: '生活号',
  1: '夜猫子',
  2: '大V核心',
  3: '宣泄群体'
}
const aiResult = ref(null)
const loadingAi = ref(false)
const recommendList = ref([])




// AI 分析处理方法
const handleAiAnalysis = async () => {
  loadingAi.value = true
  try {
    const res = await getUserInfoAnalysis(id)
    // 根据你提供的 JSON 结构，res.data 就是包含那三个字段的对象
    aiResult.value = res.data || res
    ElMessage.success("AI 研判分析已完成")
  } catch (error) {
    ElMessage.error("AI 分析服务暂时不可用",error)
  } finally {
    loadingAi.value = false
  }
}

//动态计算当前匹配圈层的概率最大值
const currentClusterProb = computed(() => {
  if (!user.value) return '0.00%'
  const probKey = `prob${user.value.clusterId}`
  return `${(Number(user.value[probKey] || 0) * 100).toFixed(2)}%`
})

const load = async () => {
  const res = await getUserInfoDetail(id)
  user.value = res.data || res
  console.log("用户画像详细", user.value)

  nextTick(() => {
    initCharts()
  })

  // 加载用户推荐
  const recommendRes = await getRecommendByUserID(
    id
  )
  recommendList.value = recommendRes.data || recommendRes || []
  console.log(recommendRes)
}

const initCharts = () => {
  if (!user.value) return

  // 1. 圈层概率分布图
  if (probChartRef.value) {
    const probChart = echarts.init(probChartRef.value, 'dark', { renderer: 'canvas' })
    probChart.setOption({
      backgroundColor: 'transparent',
      grid: { left: '3%', right: '15%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'value',
        max: 1,
        name: '概率',
        splitLine: { show: true, lineStyle: { color: '#2c3e50' } },
        axisLabel: { color: '#a0aec0' }
      },
      yAxis: {
        type: 'category',
        data: ['生活号 (0)', '夜猫子 (1)', '大V核心 (2)', '宣泄群 (3)'],
        inverse: true,
        axisLabel: { color: '#a0aec0' }
      },
      series: [{
        name: '归属概率',
        type: 'bar',
        data: [user.value.prob0, user.value.prob1, user.value.prob2, user.value.prob3],
        label: {
          show: true,
          position: 'right',
          color: '#fff',
          formatter: (params) => `${(params.value * 100).toFixed(2)}%`
        },
        itemStyle: {
          color: (params) => params.dataIndex === user.value.clusterId ? '#00ffcc' : '#00bfff',
          borderRadius: [0, 4, 4, 0]
        }
      }]
    })
  }

  // 2. 行为特征雷达图
  if (radarChartRef.value) {
    const radarChart = echarts.init(radarChartRef.value, 'dark', { renderer: 'canvas' })
    radarChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'item' },
      radar: {
        indicator: [
          { name: '情感波动度', max: 1 },
          { name: '夜间活跃度', max: 1 },
          { name: 'IP地域多样性', max: 1 },
          { name: '舆论煽动/杠杆力', max: 1 }
        ],
        radius: '65%',
        axisName: { color: '#a0aec0' },
        splitArea: { show: false },
        splitLine: { lineStyle: { color: '#2c3e50' } },
        axisLine: { lineStyle: { color: '#2c3e50' } }
      },
      series: [{
        name: '特征象限',
        type: 'radar',
        data: [{
          value: [
            user.value.hourlySentimentStddev,
            user.value.nightPostRatio,
            user.value.regionDiversityScore,
            user.value.sentimentLeverage
          ],
          name: '行为指标',
          areaStyle: { color: 'rgba(0, 191, 255, 0.2)' },
          lineStyle: { color: '#00bfff', width: 2 },
          itemStyle: { color: '#00bfff' }
        }]
      }]
    })
  }
}

onMounted(() => {
  if (id) {
    load()
  }
})
</script>

<template>
  <div class="user-info-wrapper dark-theme">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/public-opinion/character' }">用户列表</el-breadcrumb-item>
      <el-breadcrumb-item>用户深度分析画像</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="user" class="content-body">

      <el-card class="info-card bg-dark-gradient">
        <div class="user-main-info">
          <div class="avatar-box">
            <el-avatar :size="64" class="dark-avatar">{{ user.screenName?.substring(0,1) }}</el-avatar>
            <div class="name-meta">
              <h2>{{ user.screenName }}</h2>
              <p>用户ID: {{ user.userId }}</p>
            </div>
          </div>
          <div class="cluster-tag-box">
            <span class="meta-label">最终研判分类：</span>
            <el-tag
              :type="user.clusterId === 3 ? 'danger' : user.clusterId === 2 ? 'warning' : 'success'"
              effect="dark"
              size="large"
              :class="['neon-tag', user.clusterId === 1 ? 'cluster-purple' : '']"
            >
              {{ clusterMap[user.clusterId] || '未知圈层' }} (圈层 {{ user.clusterId }})
            </el-tag>
          </div>
        </div>
        <div class="update-time">数据最后观测时间：{{ user.updateTime }}</div>
      </el-card>

      <el-row :gutter="20" class="matrix-row">

        <el-col :span="8">
          <el-card class="full-height-card dark-panel" header=" 网民行为深度挖掘报告">

            <div class="insight-item">
              <div class="insight-title">作息偏好与活跃时段：</div>
              <p class="insight-desc">
                该网民在【{{ clusterMap[user.clusterId] }}】模型的归属概率达
                <strong class="highlight-cyan">{{ currentClusterProb }}</strong>。
                <span v-if="user.clusterId === 1">
                  该用户深夜发帖及网络浏览行为极度活跃，模型特征精准归类为典型<strong>“夜猫子”</strong>作息，习惯于午夜至凌晨时分在平台进行核心交互。
                </span>
                <span v-else-if="user.clusterId === 0">
                  生活作息极端规律，社交网络活跃时间紧密聚集于白昼时段，属于健康的常规高频用户。
                </span>
                <span v-else>
                  作息呈现综合多峰分布特征，全天各时段均匀分布或具备特定舆情事件触发的临时活跃倾向。
                </span>
              </p>
            </div>

            <div class="insight-item">
              <div class="insight-title">地域跨度与视野：</div>
              <p class="insight-desc">
                地域多样性特征分为 <strong class="highlight-cyan">{{ user.regionDiversityScore }}</strong>。
                <span v-if="user.regionDiversityScore >= 0.8 || user.regionDiversityScore === 1">
                  得分极高，表明该网民发言具有强烈的多地域特征，存在频繁跨区域移动行为或其视野高度跨越地域茧房，广泛关注全国多地区公共舆情。
                </span>
                <span v-else-if="user.regionDiversityScore >= 0.4">
                  地域关注度呈中等聚集，主要围绕在特定数个关联省市或重点地理节点。
                </span>
                <span v-else>
                  发言地域极其单一，关注焦点高度局限于本地或单一IP所属区域，表现出极强的本地下沉特征。
                </span>
              </p>
            </div>

            <div class="insight-item">
              <div class="insight-title">情绪稳定性与心理抗压：</div>
              <p class="insight-desc">
                时段情感标准差为 <strong class="text-white">{{ user.hourlySentimentStddev }}</strong>，主要活跃地域平均情感为
                <strong :class="user.topRegionAvgScore < 0 ? 'text-neon-red' : 'text-neon-green'">{{ user.topRegionAvgScore }}</strong>。
                <span v-if="user.clusterId === 3">
                  <strong>核心预警：</strong>该网民被模型划归为【宣泄群体】，历史发言频繁带有强烈的负向情绪与倾诉欲，心理防线或处于高压状态。
                </span>
                <span v-else-if="user.hourlySentimentStddev === 0 && user.topRegionAvgScore === 0">
                  情绪特征表现为冷淡、极其理智或疑似存在自动化发布脚本，发言几乎不携带任何极端情绪词汇。
                </span>
                <span v-else>
                  发言情绪起伏在标准阈值内平缓交错，日常网络言论基调整体偏向理性或温和。
                </span>
              </p>
            </div>

            <div class="insight-item">
              <div class="insight-title">舆论杠杆与煽动性：</div>
              <p class="insight-desc">
                情感杠杆值（模型影响力）为 <strong class="text-white">{{ user.sentimentLeverage }}</strong>。
                <span v-if="user.sentimentLeverage >= 0.8 || user.clusterId === 2">
                  具备极强的<strong>舆论倒灌与扩散煽动能力</strong>。其言论极易引发多级转发与大范围圈层共鸣，属于高价值舆情导向控制节点。
                </span>
                <span v-else-if="user.sentimentLeverage >= 0.4">
                  具备一定的小圈层传染力与社群共鸣性，能够在特定核心圈子内传导情绪。
                </span>
                <span v-else>
                  杠杆值微弱，属于网络情绪的“非易感”大众，在社交平台不具备主动引发核心舆论倒灌的意愿与倾向。
                </span>
              </p>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card class="full-height-card dark-panel" header="GMM 圈层归属概率分布">
            <div class="chart-tip-dark">
              模型研判依据：有 <strong>{{ currentClusterProb }}</strong> 的把握将其归为【<strong>{{ clusterMap[user.clusterId] }}</strong>】
            </div>
            <div ref="probChartRef" class="echarts-box"></div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card class="full-height-card dark-panel" header="核心特征象限（雷达图）">
            <div ref="radarChartRef" class="echarts-box"></div>
          </el-card>
        </el-col>

      </el-row>
      <el-row class="matrix-row">
  <el-col :span="24">
    <el-card class="dark-panel">
      <template #header>
        <div class="card-header-flex">
          <span>
            <img :src="deepseekIcon" alt="DeepSeek" class="deepseek-icon" />
            DeepSeek-V4 AI 深度研判报告</span>
          <el-button
            type="primary"
            size="small"
            :loading="loadingAi"
            @click="handleAiAnalysis"
            class="neon-btn"
          >
            获取 AI 智能分析
          </el-button>
        </div>
      </template>

      <div v-if="aiResult" class="ai-report-content">
        <el-descriptions :column="1" border class="dark-descriptions">
          <el-descriptions-item label="用户标签" label-class-name="ai-label">
            <el-tag effect="plain" type="info">{{ aiResult.userTag }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="行为深度拆解" label-class-name="ai-label">
            <p>{{ aiResult.behaviorAnalysis }}</p>
          </el-descriptions-item>
          <el-descriptions-item label="运营优化建议" label-class-name="ai-label">
            <p class="highlight-cyan">{{ aiResult.operationSuggestion }}</p>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <el-empty v-else description="暂无 AI 分析报告，请点击上方按钮生成" />
    </el-card>
  </el-col>
</el-row>

    </div>

    <el-skeleton v-else :rows="10" animated class="dark-skeleton" />
  </div>
  <!-- 推荐内容列表 -->

<recommendListPage
:list="recommendList"
v-if="user"
:userId="user.userId"
></recommendListPage>

<Footer></Footer>
</template>

<style scoped>
.user-info-wrapper.dark-theme {
  padding: 24px;
  background-color: #0b0f19;
  min-height: calc(100vh - 40px);
  color: #e2e8f0;
}
:deep(.el-breadcrumb) {
  margin-bottom: 16px;
}
:deep(.el-breadcrumb__inner) {
  color: #a0aec0 !important;
}
:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #fff !important;
}
:deep(.el-card) {
  background-color: #131a26 !important;
  border: 1px solid #1e293b !important;
  color: #e2e8f0 !important;
}
:deep(.el-card__header) {
  border-bottom: 1px solid #1e293b !important;
  color: #fff !important;
  font-weight: bold;
}
.bg-dark-gradient {
  background: linear-gradient(135deg, #141c2c 0%, #0d1527 100%) !important;
}
.user-main-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.avatar-box {
  display: flex;
  align-items: center;
  gap: 16px;
}
.dark-avatar {
  background-color: #1e293b;
  color: #00bfff;
  border: 1px solid #00bfff;
  font-weight: bold;
}
.name-meta h2 {
  margin: 0 0 4px 0;
  color: #ffffff;
}
.name-meta p {
  margin: 0;
  color: #a0aec0;
  font-size: 14px;
}
.meta-label {
  color: #a0aec0;
}
.neon-tag {
  background-color: rgba(0, 255, 204, 0.1) !important;
  border-color: #00ffcc !important;
  color: #00ffcc !important;
}

:deep(.cluster-purple) {
  background-color: rgba(185, 117, 255, 0.1) !important;
  border-color: #b975ff !important;
  color: #b975ff !important;
}
.update-time {
  margin-top: 16px;
  font-size: 13px;
  color: #64748b;
  border-top: 1px solid #1e293b;
  padding-top: 8px;
}
.matrix-row {
  margin-top: 20px;
}
.full-height-card {
  height: 520px;
  overflow-y: auto;
}
.insight-item {
  margin-bottom: 18px;
  font-size: 14px;
  line-height: 1.6;
}
.insight-title {
  font-weight: bold;
  color: #00bfff;
  margin-bottom: 4px;
}
.insight-desc {
  margin: 0;
  color: #cbd5e1;
}
.highlight-cyan {
  color: #00ffcc;
}
.text-white {
  color: #ffffff;
}
.text-neon-red {
  color: #ff4d4d;
}
.text-neon-green {
  color: #00ffcc;
}
.chart-tip-dark {
  font-size: 13px;
  color: #e6a23c;
  margin-bottom: 10px;
  background: rgba(230, 162, 60, 0.1);
  padding: 8px;
  border-left: 4px solid #e6a23c;
  border-radius: 4px;
}
.echarts-box {
  width: 100%;
  height: 380px;
}
.dark-skeleton :deep(.el-skeleton__item) {
  background: #1e293b !important;
}

.card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.neon-btn {
  background: #00bfff !important;
  border: none !important;
  color: #000 !important;
  font-weight: bold;
}
:deep(.ai-label) {
  width: 120px;
  color: #00bfff !important;
  font-weight: bold;
}
.dark-descriptions :deep(.el-descriptions__cell) {
  background: #1a2233 !important;
  color: #e2e8f0 !important;
}

/* 确保加载遮罩层能正确覆盖在 card 上 */
:deep(.el-card) {
  position: relative !important;
}

/* AI 报告区域的特殊容器 */
.ai-report-container {
  min-height: 200px;
  position: relative;
}

/* 优化 DeepSeek 图标样式 */
.deepseek-icon {
  width: 20px;
  height: 20px;
  vertical-align: middle;
  margin-right: 8px;
  transition: transform 0.5s ease;
}

/* 旋转动画 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 当 loading 时，图标自动旋转 */
.is-loading .deepseek-icon {
  animation: spin 1.5s linear infinite;
}



.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.recommend-item {
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  border-left: 3px solid #00bfff;
}
.item-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.screen-name {
  color: #00bfff;
  font-weight: bold;
}
.item-text {
  font-size: 14px;
  color: #e2e8f0;
  margin: 0 0 8px 0;
}
.item-footer {
  font-size: 12px;
  color: #64748b;
  display: flex;
  gap: 20px;
}


</style>
