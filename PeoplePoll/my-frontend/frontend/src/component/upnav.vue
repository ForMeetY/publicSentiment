<script setup>
defineOptions({
  name: 'UpNavComponent',
})
import dropdownList from './dropdownList.vue'
import { inject } from 'vue'
// 从父组件注入项目名称
const headName = inject('headName')



</script>

<template>
  <!-- 只改这里：前面加冒号 :show-back="false" -->
  <el-page-header :show-back="false">
    <template #content>
      <div class="flex items-center">
        <el-avatar
          class="logo"
          :size="60"
          src="/image/logo(2).jpg"
        />
        <span  class="text-large"> {{ headName }} </span>
      </div>
    </template>

    <template #extra>
      <div class="flex items-center">
        <dropdownList></dropdownList>
      </div>
    </template>

  </el-page-header>
</template>

<style scoped>
/* 导航栏样式调整 */
:deep(.el-page-header__header) {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 0 35px;
  height: 80px;
}

.logo {
  margin-right: 10px;
  border-radius: 0;
  height: 60px;
  width: 100px;
}

/* 隐藏返回按钮和分隔线 */
:deep(.el-page-header__back){
  display: none;
}
/* 隐藏分隔线 */
:deep(.el-divider) {
  display: none;
}

/* logo居中 */
:deep(.items-center) {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* logo字体大小 */
.text-large {
  font-family: Georgia,serif;
  font-size: 24px;
  color:aliceblue;
}

</style>
