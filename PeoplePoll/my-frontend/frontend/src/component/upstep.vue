<script setup>
import { ArrowRight } from '@element-plus/icons-vue'
import { defineProps } from 'vue'

import { defineOptions } from 'vue'
defineOptions({
  name: 'UpStep',
})
const props = defineProps({
  upstepList: {
    type: Array,
    default: () => [],
  },
})


</script>

<template>
  <el-breadcrumb class="custom-bread" :separator-icon="ArrowRight">
    <el-breadcrumb-item :to="{ path: '/' }">主页</el-breadcrumb-item>
    <el-breadcrumb-item v-for="item in props.upstepList" :key="item.path"
    :to="{ path: item.path }">
      {{ item.name }}</el-breadcrumb-item>
  </el-breadcrumb>
</template>

<style scoped>



.custom-bread {
  display: flex;
  align-items: center;
  color: #fff;
  background: rgba(20, 25, 40, 0.7);
  height: 60px;
  padding-left:50px ;
  font-size: 18px;
  font-weight: bold;
}

:deep(.el-breadcrumb__item) {
  color: #fff;
}

:deep(.el-breadcrumb__inner.is-link) {
  cursor: pointer !important;
  font-family: 'Courier New', Courier, monospace;
  color: #ffffff !important;
}

:deep(.el-breadcrumb__inner.is-link:hover) {
  color: #409eff !important;
  transition: color 0.3s ease;
}





</style>

