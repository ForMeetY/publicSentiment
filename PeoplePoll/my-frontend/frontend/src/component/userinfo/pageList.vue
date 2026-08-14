<template>
  <div class="demo-pagination-block dashboard-pagination">
    <el-pagination
      :current-page="props.currentPage"
      :page-size="props.pageSize"
      :page-sizes="[10, 20, 30, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="props.total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      background
    />
  </div>
</template>

<script setup>
const props = defineProps({
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  total: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:page', 'update:pageSize'])

const handleSizeChange = (val) => {
  emit('update:pageSize', val)
}

const handleCurrentChange = (val) => {
  emit('update:page', val)
}
</script>

<style scoped>
.demo-pagination-block {
  margin-top: 25px;
  margin-bottom: 15px;


  --el-pagination-text-color: #94a3b8 !important;       /* 总条数、前往等常规文本 */
  --el-pagination-button-color: #94a3b8 !important;     /* 翻页按钮箭头默认色 */
  --el-pagination-hover-color: #00ffcc !important;      /* 悬浮聚焦荧光绿 */

  /* 输入框 / 下拉框相关的文本颜色 */
  --el-input-text-color: #ffffff !important;           /* 输入框内文字颜色 */
  --el-text-color-placeholder: #64748b !important;     /* 占位符颜色 */
  --el-text-color-regular: #94a3b8 !important;         /* 常规文本颜色 */
}

/* 保持你的基本布局框架 */
:deep(.el-pager li) {
  margin: 0 4px;
  border-radius: 4px;
}

:deep(.el-pagination){
  max-width: calc(100% - 40px);
  width: fit-content;
  margin: 0 auto;
  margin-left: 5%;
}


/*  未激活的页码、普通按钮背景与精致暗蓝边框 */
:deep(.el-pagination .el-pager li),
:deep(.el-pagination button) {
  background-color: #101726 !important; /* 与表格行色调呼应 */
  border: 1px solid #1e293b !important; /* 细腻的暗蓝灰色边框 */
  color: #94a3b8 !important;
  transition: all 0.2s ease;
}

/*  悬浮效果（Hover）：边缘发出淡淡的冰蓝科技微光 */
:deep(.el-pagination .el-pager li:not(.is-active):hover),
:deep(.el-pagination button:not(:disabled):hover) {
  border-color: #00bfff !important;
  color: #00bfff !important;
  box-shadow: 0 0 8px rgba(0, 191, 255, 0.2);
}

/* 激活状态页码：无缝对接大屏的荧光绿/青流光效果 */
:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #00bfff 0%, #00ffcc 100%) !important;
  color: #050508 !important; /* 暗色文字，形成强烈对比反差，极具现代感 */
  font-weight: bold;
  border-color: transparent !important;
  box-shadow: 0 0 10px rgba(0, 255, 204, 0.4);
}

/* 输入框、下拉框外壳（玻璃拟态微光） */
:deep(.el-input__wrapper) {
  background-color: #101726 !important;
  box-shadow: none !important;
  border: 1px solid #1e293b !important;
  transition: all 0.3s;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: #00ffcc !important;
  box-shadow: 0 0 8px rgba(0, 255, 204, 0.2) !important;
}

/* 下拉菜单及输入框内部文本 */
:deep(.el-pagination .el-input__inner) {
  background: transparent !important;
  color: #ffffff !important;
}

/*  解决原本文字变黑看不见的问题 */
:deep(.el-select__selected-item) {
  color: black !important; /* 强制选中的每页条数（如 10条/页）文字为白色 */
}

/*  强制下拉小箭头和跳转文本内的文字颜色 */
:deep(.el-input__suffix .el-input__icon),
:deep(.el-pagination__jump) {
  color: #94a3b8 !important;
}
</style>
