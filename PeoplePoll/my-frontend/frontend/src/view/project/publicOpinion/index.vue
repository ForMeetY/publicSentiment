<script setup>
// import UpNavComponent from '@/component/upnav.vue'
import upstep from '@/component/upstep.vue'
import {RouterView }from 'vue-router'
defineOptions({
  name: 'PublicOpinion',
})
import { provide } from 'vue'
// 提供项目名称
const name = '舆论预测'
provide('name', name)
const headName = '政务舆论预测'
provide('headName', headName)
// 提供一个列表给upstep组件
const upstepList = [
  {
    path: '/public-opinion/analysis',
    name: '舆论统计',
  },
  {
    path: '/public-opinion/forecast',
    name: '情感分析',
  },
  {
    path: '/public-opinion/character',
    name: '用户画像'
  }
]
</script>

<template>
  <div>
    <!-- <UpNavComponent/> -->
    <upstep :upstepList="upstepList"></upstep>
    <RouterView></RouterView>
  </div>
</template>

<style scoped>

</style>
