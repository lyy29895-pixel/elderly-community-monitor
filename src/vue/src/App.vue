<template>
  <div>
   <router-view />
   <AlertNotification />
  </div>

</template>

<style>

</style>

<script>
import AlertNotification from '@/components/AlertNotification.vue'

export default{
  name:"App",
  components: { AlertNotification }

}
</script>