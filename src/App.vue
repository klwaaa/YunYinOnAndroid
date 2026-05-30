<template>
  <Home class="light" v-if="isTokenLoaded"></Home>
</template>

<script setup lang="ts">
  import {useGetTokenStore} from "./store/token.ts";
  import {onBeforeMount, watch, ref} from "vue";
  import {storeToRefs} from "pinia";
  import Home from "./components/Home.vue";
  import {useGetAudio} from "./store/audio.ts";
  // import {
  //   isPermissionGranted,
  //   requestPermission,
  //   sendNotification,
  //   registerActionTypes,
  //   onAction,
  // } from '@tauri-apps/plugin-notification';
  //
  // // 1. 先注册交互类型
  //  registerActionTypes([
  //   {
  //     id: '1',
  //     actions: [
  //       {
  //         id: 'simple',
  //         title: 'Click me',
  //         requiresAuthentication: true,
  //         input:true,
  //         inputButtonTitle: 'Click me',
  //         inputPlaceholder: 'Click me',
  //       },
  //     ],
  //   },
  // ]).then(() => {
  //    sendNotification({
  //      title: 'Test',
  //      body: 'Check button',
  //      actionTypeId: '1',
  //      ongoing: true,
  //
  //    });
  // });
  //
  //
  //
  // // 3. 发送通知的函数
  // async function showNotification() {
  //   // 检查并请求权限
  //   let permissionGranted = await isPermissionGranted();
  //   if (!permissionGranted) {
  //     const permission = await requestPermission();
  //     permissionGranted = permission === 'granted';
  //   }
  //   if (permissionGranted) {
  //     // sendNotification({
  //     //   title: 'New message',
  //     //   body: 'You have a new message',
  //     //   // actionTypeId: 'messages',
  //     //   // 这里不直接指定 actions，系统会根据注册的 'messages' 动作类型显示按钮
  //     // });
  //   }
  // }
  //
  // showNotification();
  
  
  const {isPlaying} = storeToRefs(useGetAudio());
  const isTokenLoaded = ref(false);
  
  // 通过code获得token
  onBeforeMount(async () => {
    
    const tokenStore = useGetTokenStore();
    
    let {refresh_token} = storeToRefs(tokenStore);
    if (!localStorage.getItem("token")) {
      localStorage.setItem("token", JSON.stringify({
        access_token: "null",
        refresh_token: "null"
      }));
    }
    // 检测是不是通过code获取了token，如果有就通过refresh_token每2小时获取一次token
    watch(refresh_token, (_, oldVal) => {
      if (oldVal === "null") {
        setInterval(tokenStore.useRefreshTokenGetToken, 6480000);
      }
    });
    // 检测是不是有token，如果有就通过refresh_token每2小时获取一次token
    if (localStorage.getItem("token") !== null
      && JSON.parse(<string>localStorage.getItem("token")).refresh_token !== "null") {
      await tokenStore.useRefreshTokenGetToken();
      isTokenLoaded.value = true;
      setInterval(tokenStore.useRefreshTokenGetToken, 6480000);
    } else {
      isTokenLoaded.value = true;
    }
    
    // 页面刷新 / 浏览器关闭时停止播放
    window.addEventListener("beforeunload", () => {
      isPlaying.value = false;
      
    });
    document.addEventListener('contextmenu', function (event) {
      event.preventDefault(); // 阻止右键菜单的弹出
    });
  });
  if (!localStorage.getItem("audio")) {
    localStorage.setItem("audio", JSON.stringify([]));
  }
</script>

<style scoped>
  .light {
    padding-top: 5vh;
  }
</style>