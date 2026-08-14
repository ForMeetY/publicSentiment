<script setup>
import { ref } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const locale = ref(zhCn)

import { onMounted, provide } from 'vue'
/* 请求项目列表 */
import { getAll } from '@/api/ProjectMemApi.js'
const projectList = ref([])
/* 获取项目列表 */
const useProjectList = async () => {
  const res = await getAll()
  projectList.value = res.data

  console.log(res)
}
/* 提供列表传递给子组件 */
provide('projectList', projectList)
console.log(projectList.value)
onMounted(() => {
   useProjectList()
})
</script>

<template>
  <el-config-provider :locale="locale">
    <RouterView></RouterView>
  </el-config-provider>

</template>

<style scoped>

</style>



