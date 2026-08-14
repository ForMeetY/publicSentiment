<script setup>
import TableByPage from "@/component/userinfo/tableByPage.vue"
import pageList from "@/component/userinfo/pageList.vue"
import { getUserInfoPage } from "@/api/userInfo"
import { onMounted, ref} from 'vue'
import { useRouter } from 'vue-router'
import Footer from "@/component/footer.vue"
const router = useRouter()

// 父组件维护所有状态
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tableData = ref([]) // 表格数据

// 统一请求方法
const loadData = async () => {
  const res = await getUserInfoPage(
    currentPage.value,
    pageSize.value
  )

  // 赋值给表格 + 总条数
  tableData.value = res.rows
  total.value = res.total
  // 表格数据
  console.log(tableData.value)
}

// 监听子组件的页码变化，自动刷新
const onPageChange = (val) => {
  currentPage.value = val
  loadData()
}

// 监听子组件的每页条数变化
const onPageSizeChange = (val) => {
  currentPage.value = 1 // 切换每页条数，回到第一页
  pageSize.value = val
  loadData()
}

// 监听子组件的查询事件
const handleQuery = async (val) => {
  console.log(val)
  console.log("父组件触发1")
  // 处理查询逻辑 val 就是点击的行用户ID
  // 携带ID跳转到详情页，显示详细的用户画像
  router.push({
    path: '/userProfileDetail',
    query: { id: val }
  })
  //跳转到详情页
}

onMounted(() => {
  loadData()
  // 表格数据
  console.log(tableData.value)
})
</script>

<template>
  <div class="user-info-wrapper">
    <TableByPage :table-data="tableData"
      @query="handleQuery"
    />

    <!-- 父组件传值给子组件，子组件抛事件给父组件 -->
    <pageList
      :currentPage="currentPage"
      :pageSize="pageSize"
      :total="total"
      @update:page="onPageChange"
      @update:pageSize="onPageSizeChange"
    />
  </div>
  <Footer></Footer>
</template>

<style scoped>
.user-info-wrapper {

  margin: 20px auto;
  margin-bottom: 140px;
}

Footer {
  margin-top: 220px;
  margin-bottom: 0px;
}
</style>
