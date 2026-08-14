<!-- 这个组件用于主页展示
具体的项目内容和 描述查询数据库得到 而不是固定值
这个数据库需要包括
1、项目id
2、项目名称
3、项目描述
4、项目图片
5、项目链接
-->
<script setup>
import { inject } from 'vue'
defineOptions({
  name: 'LayoutComponent',
})
/* 从父组件注入项目列表 */
const projectList = inject('projectList')
console.log(projectList.value)
console.log(projectList.value[0].img)
import { useRouter } from 'vue-router'
const router = useRouter()
const navigateTo = (path) => {
  router.push(path)
}
</script>

 <template>
  <div class="container">
      <div class="project"
      v-for="item in projectList"
      :key="item.id"
      :style="{ backgroundImage: `url('${item.img}')`
        , backgroundSize: 'cover'
        , backgroundPosition: 'center'
        , backgroundRepeat: 'no-repeat'
      }"
      >
      <div class="project-content">
        <h2 style="color: #fff; font-size: 30px;font-style: SimHei;">{{ item.title }}</h2>
        <p style="color: #fff; font-size: 20px; font-style: italic;">{{ item.description }}</p>
        <el-button class="btn"
        @click="navigateTo(item.path)">
        查看详情
        <el-icon>
          <ArrowRight />
        </el-icon>
      </el-button>
      </div>

      </div>
    </div>
</template>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0 35px;
}

.project {
  display: flex;
  flex-direction: column;
  justify-content: end;
  background-color: black;
  height: 1000px; /* 项目高度 */
  /* background-color: #514848; */
}

.project-content {
  /* background-color: pink ; */
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 20px;
  margin-bottom: 150px;
  width: 45%;
  /* 为背景加模糊 */
  /* backdrop-filter: blur(5px); */
}

/* 调整详细按钮样式 */
.btn {
  margin-top: 20px;
  width:100px;
  height: 40px;
  background-color: transparent;
  /* border:0px; */
  /* 背景模糊 */
  /* padding:5px 3px; */
  padding-right: 6px;
  backdrop-filter: blur(25px);
}
</style>
