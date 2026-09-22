<script>

import Aside from "@/components/Aside.vue";
import Header from "@/components/Header.vue";

export default{
  name:"Layout",
  components:{
    Header,
    Aside
  }
}

</script>

<template>
  <div class="app-layout">
    <Header />
    <div class="app-layout__body">
      <Aside />
      <main class="app-layout__main">
        <router-view/>
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--yi-shell-bg, #e5eff7);
  overflow: hidden;
}

.app-layout__body {
  display: flex;
  align-items: stretch;
  flex: 1;
  min-height: 0;
}

.app-layout__main {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow-y: auto;
  background: var(--yi-main-surface, #f3f8fc);
}
</style>