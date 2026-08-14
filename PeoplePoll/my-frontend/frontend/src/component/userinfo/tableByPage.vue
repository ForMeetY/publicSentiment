<script setup>
import { defineProps, defineEmits } from 'vue'
const emit = defineEmits(['query'])
const props = defineProps({
  tableData: {
    type: Array,
    default: () => []
  }
})

// 点击后将用户Id传递给父组件
const handleQuery = (row) => {
  emit('query', row.userId)
  console.log("当前行ID：", row.userId)
}

// 辅助函数：将小数值转换为亮眼的百分比文本
const toPercent = (val) => {
  if (val === undefined || val === null) return '0.0%'
  return `${(Number(val) * 100).toFixed(1)}%`
}
</script>

<template>
  <div class="table-wrapper dashboard-theme">
    <el-table
      :data="props.tableData"
      align="center"
      :fit="true"
      style="width:100%"
      class="custom-dark-table"
    >
      <el-table-column prop="userId" label="用户ID" width="130" />
      <el-table-column prop="screenName" label="用户昵称" width="160" class-name="highlight-name" />

      <el-table-column prop="clusterId" label="所属圈层" width="110">
        <template #default="scope">
          <el-tag
            :type="scope.row.clusterId === 3 ? 'danger' : scope.row.clusterId === 2 ? 'warning' : 'success'"
            effect="dark"
            size="small"
            :class="['neon-tag', scope.row.clusterId === 1 ? 'cluster-purple' : '']"
          >
            圈层 {{ scope.row.clusterId }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="prob0" label="生活号概率" width="110">
        <template #default="scope">
          <span class="prob-text cyan">{{ toPercent(scope.row.prob0) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="prob1" label="夜猫几率" width="110">
        <template #default="scope">
          <span class="prob-text purple">{{ toPercent(scope.row.prob1) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="prob2" label="大V权重" width="110">
        <template #default="scope">
          <span class="prob-text orange">{{ toPercent(scope.row.prob2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="prob3" label="宣泄概率" width="110">
        <template #default="scope">
          <span class="prob-text neon-red">{{ toPercent(scope.row.prob3) }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="updateTime" label="观测时间" min-width="180" />

      <el-table-column label="操作" width="100" fixed="right">
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            plain
            class="action-btn-neon"
            @click="handleQuery(scope.row)"
          >
            画像研判
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.table-wrapper {
  width: 95%;
  margin: 0 auto;
  background-color: transparent;
}

/* 表格全局容器与底层穿透 */
:deep(.custom-dark-table) {
  background-color: transparent !important;
  --el-table-border-color: #1e293b !important;
  border: 1px solid #1e293b !important;
  border-radius: 8px;
  overflow: hidden;
}

/* 隐藏底层多余的空白背景 */
:deep(.el-table__inner-wrapper)::before {
  display: none !important;
}

:deep(.el-table__header th) {
  text-align: center !important;
  background: linear-gradient(180deg, #161f30 0%, #101726 100%) !important;
  color: #f1f5f9 !important;
  font-weight: 600;
  border-bottom: 1px solid #2d3748 !important;
  letter-spacing: 1px;
}

/* 数据行：剔除原粗暴色块，改用极细微的虚化分割线 */
:deep(.el-table__body td) {
  text-align: center !important;
  background-color: #0f1422 !important; /* 带有蓝墨色底调，完美适配大屏 */
  color: #94a3b8 !important; /* 调整为冷色调灰色 */
  border-bottom: 1px solid #1e293b !important;
  padding: 10px 0;
  transition: all 0.2s ease;
}

/* 斑马线样式覆盖（隔行换色） */
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #141b2d !important;
}

/* 悬浮行（Hover效果）：大屏最炫的流光感效果 */
:deep(.el-table__body tr:hover > td) {
  background-color: #1e293b !important;
  color: #ffffff !important;
  box-shadow: inset 0 0 10px rgba(0, 191, 255, 0.1);
}

/* 昵称高亮 */
:deep(.highlight-name) {
  color: #fff !important;
  font-weight: bold;
}


.prob-text {
  font-family: 'Courier New', Courier, monospace;
  font-weight: bold;
}
.prob-text.cyan { color: #00ffcc; }
.prob-text.purple { color: #b975ff; }
.prob-text.orange { color: #ff9f43; }
.prob-text.neon-red { color: #ff4d4d; }


.neon-tag {
  border: 1px solid transparent;
}
.el-tag--success { background-color: rgba(0, 255, 204, 0.15) !important; border-color: #00ffcc !important; color: #00ffcc !important; }
.el-tag--warning { background-color: rgba(255, 159, 67, 0.15) !important; border-color: #ff9f43 !important; color: #ff9f43 !important; }
.el-tag--danger { background-color: rgba(255, 77, 77, 0.15) !important; border-color: #ff4d4d !important; color: #ff4d4d !important; }


.el-tag--purple,
:deep(.cluster-purple) {
  background-color: rgba(185, 117, 255, 0.15) !important;
  border-color: #b975ff !important;
  color: #b975ff !important;
}


.action-btn-neon {
  background: rgba(0, 191, 255, 0.05) !important;
  border: 1px solid #00bfff !important;
  color: #00bfff !important;
  text-shadow: 0 0 4px rgba(0, 191, 255, 0.4);
  box-shadow: 0 0 8px rgba(0, 191, 255, 0.1);
  transition: all 0.3s;
}
.action-btn-neon:hover {
  background: #00bfff !important;
  color: #fff !important;
  box-shadow: 0 0 15px rgba(0, 191, 255, 0.5);
  transform: translateY(-1px);
}

/* 隐藏原生横向滚动条不美观问题 */
:deep(.el-scrollbar__wrap) {
  background-color: transparent !important;
}
</style>
