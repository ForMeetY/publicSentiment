<script setup>
defineOptions({
  name: 'HomePage',
})
import { provide } from 'vue'
import UpNavComponent from '@/component/upnav.vue'
import FooterComponent from '@/component/footer.vue'
import LayoutComponent from '@/component/layout.vue'
const name = 'Home'
const headName = 'FutureShow'
provide('headName', headName)
provide('name', name)
</script>

<template>
  <div>
    <UpNavComponent/>
    <LayoutComponent/>
    <FooterComponent/>
  </div>
</template>

<style scoped>


</style>
