<template>
  <div class="popup" v-if="showPopup">
    <div class="popup-content">
      <!-- 这里添加弹窗的内容 -->
      <p>这是一个弹窗</p>
      <button @click="closePopup">关闭</button>
    </div>
  </div>
</template>

<script>
export default {
  props: ['touser', 'touuid'],
  data () {
    return {
      showPopup: false
    }
  },
  methods: {
    openPopup () {
      this.showPopup = true
    },
    closePopup () {
      this.showPopup = false
      window.open('/index?_uuid=' + this.touuid + '&_user=' + this.touser)
    }
  }
}
</script>

<style scoped>
.popup {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.popup-content {
  background-color: white;
  padding: 20px;
  border-radius: 5px;
  text-align: center;
}
</style>
