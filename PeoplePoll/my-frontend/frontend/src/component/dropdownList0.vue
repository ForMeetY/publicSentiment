<script setup>
import {
  ArrowDown,
  CirclePlus,
  PriceTag,
  Memo,
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { inject } from 'vue'

/* 从父组件注入项目列表 */
const projectList = inject('projectList')
console.log(projectList.value)
/* 下拉菜单点击事件处理 */
const router = useRouter()
const navigateTo = (path) => {
  router.push(path)
}
/* 从父组件获取传递的名字 */
const name = inject('name')

</script>

<template>
  <el-row class="block-col-2">
    <el-tag class="tag" style="background-color: black;" type="primary">
      {{ name }}</el-tag>
    <el-col :span="8">
      <el-dropdown>
        <!-- 下拉菜单链接 -->
        <span class="el-dropdown-link">
          <el-icon><Memo /></el-icon>
          <el-icon class="el-icon--right"><Arrow-down /></el-icon>
        </span>
        <!-- 下拉菜单内容 -->
        <template #dropdown>
          <el-dropdown-menu>
            <!-- <el-dropdown-item :icon="House"
            @click="navigateTo('/')"
            >
              首页
            </el-dropdown-item> -->
            <el-dropdown-item :icon="PriceTag"
            v-for="item in projectList"
            :key="item.id"
            @click="navigateTo(item.path)"
            >
              {{ item.title }}
          </el-dropdown-item>
            <el-dropdown-item :icon="CirclePlus">期待更多....</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-col>
  </el-row>
</template>

<style scoped>
/* 下拉菜单样式调整 */
.block-col-2 .demonstration {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-bottom: 20px;
}

.block-col-2{
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 20px;
}



.tag{
  font-size: 18px;
  border: 0;
}


/* 下拉菜单链接样式调整 */
.block-col-2 .el-dropdown-link {
  display: flex;
  align-items: center;
  padding-right: 30px;
}
/* 下拉菜单样式调整 */
:deep(.el-dropdown) {
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
}

/* 下拉菜单链接样式调整 */
:deep(.el-dropdown-link) {
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
}
/* 下拉菜单图标大小 */
:deep(.el-icon) {
  font-size: 24px;
}


/* 下拉菜单边框 */
:deep(.el-dropdown__caret) {
  border: none !important;
  outline: none !important;
}
</style>
