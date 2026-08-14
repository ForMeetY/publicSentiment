<script setup>
import { ref } from 'vue'
const activeIndex = ref('1')
const handleSelect = (key, keyPath) => {
  console.log(key, keyPath)
}
import { useRouter } from 'vue-router'
import { inject } from 'vue'

const projectList = inject('projectList')
const router = useRouter()
const navigateTo = (path) => {
  router.push(path)
}
const name = inject('name')
</script>

<template>
  <el-tag class="tag" style="background-color: black;" type="primary">
    {{ name }}
  </el-tag>

  <el-menu
    :default-active="activeIndex"
    class="el-menu-demo"
    mode="horizontal"
    :ellipsis="false"
    @select="handleSelect"
  >
    <el-sub-menu index="1">
      <template #title>
        <span style="font-size: 20px; color: aliceblue">
          项目
        </span>
      </template>

      <el-menu-item
        index="1-1"
        v-for="item in projectList"
        :key="item.path"
        @click="navigateTo(item.path)"
      >
        {{ item.title }}
      </el-menu-item>
    </el-sub-menu>
  </el-menu>
</template>

<style scoped>
/* 主菜单 */
.el-menu {
  background-color: black !important;
  border-bottom: 0 !important;
}

/* ---------------------
  1. 主菜单标题（项目）
--------------------- */
:deep(.el-sub-menu__title) {
  color: aliceblue !important;
  font-size: 20px !important;
  background-color: black !important;
}
:deep(.el-sub-menu__title:hover) {
  color: aliceblue !important;
  background-color: black !important;
}

/* ---------------------
  2. 下拉弹层整体（最关键！）
--------------------- */
:deep(.el-popper) {
  background-color: black !important;
  border: none !important;
}

/* ---------------------
  3. 下拉菜单项
--------------------- */
:deep(.el-menu-item) {
  color: aliceblue !important;
  background-color: black !important;
  font-size: 18px !important;
}

/* 鼠标悬浮完全不变色 */
:deep(.el-menu-item:hover) {
  color: aliceblue !important;
  background-color: black !important;
}

/* 选中状态不变色 */
:deep(.el-menu-item.is-active) {
  color: aliceblue !important;
  background-color: black !important;
}

/* tag 样式 */
.tag {
  font-size: 18px;
  border: 0;
}
</style>
