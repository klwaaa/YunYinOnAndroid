<template>
  <div class="app md-theme-dark">

    <div class="loginPopup scrim" v-show="isShow">
      <div class="dialog-card surface-container-high">
        <div class="dialog-header">
          <h3>需要授权</h3>
        </div>
        <p class="dialog-text">请先登录阿里云盘以获取完整体验</p>

        <div class="login-action">
          <a v-if="!isLoggedIn" :href="loginUrl" class="md-text-button">
            前往网页登录
          </a>
        </div>

        <div class="input-field-container">
          <input type="text" placeholder=" " ref="codeInput" class="md-input">
          <label class="floating-label">请输入授权码</label>
        </div>

        <div class="dialog-actions">
          <div @click="()=>{isShow=false}" class="md-text-button">取消</div>
          <div @click="getToken" class="md-filled-button">确定</div>
        </div>
      </div>
    </div>

    <header class="app-header surface">
      <nav class="nav-items">
        <div class="nav-chip login-chip">
          <a v-if="!isLoggedIn" :href="loginUrl" class="md-chip">登录</a>
          <span v-else @click="logOut" class="md-chip">退出</span>
        </div>

        <div class="nav-chip playListButton">
          <router-link to="/PlayList" class="md-chip">歌单</router-link>
        </div>
        <div class="nav-chip musicLibraryButton">
          <router-link to="/MusicLibrary" class="md-chip">曲库</router-link>
        </div>
        <div class="nav-chip synchronousButton">
          <router-link to="/DataSync" class="md-chip">同步</router-link>
        </div>
        <div class="nav-chip settingButton">
          <router-link to="/Setting" class="md-chip">设置</router-link>
        </div>
      </nav>
    </header>

    <main class="main-content surface-background">
      <suspense>
        <template v-slot:default>
          <router-view class="router-view" v-if="route.name === 'PlayList'" v-model:count="count"/>
          <router-view class="router-view" v-else-if="route.name === 'AudioView'" :key="controlAudioKey+count"/>
          <router-view class="router-view" v-else/>
        </template>
      </suspense>
      <div class="bottom-spacer"></div>
    </main>

    <div class="audioControl surface-container">
      <Suspense>
        <template v-slot:default>
          <AudioControl :key="controlAudioKey+count"></AudioControl>
        </template>
        <template v-slot:fallback>
          <div class="loading-text">加载组件中...</div>
        </template>
      </Suspense>
    </div>
  </div>
</template>

<script setup lang="ts">
import AudioControl from "./AudioControl.vue";
import {useRoute} from 'vue-router';
// 假设这些 store 已定义在您的项目中
import {useGetTokenStore} from "../store/token.ts";
import {useGetAudio} from "../store/audio.ts";
import {storeToRefs} from "pinia";
import {RouterView, RouterLink} from "vue-router";
import {ref, watch} from "vue";
// 假设这是 Tauri API
import {invoke} from "@tauri-apps/api/core";

const isShow = ref(false);
const codeInput: any = ref();
const count = ref(0);
const route = useRoute();

const tokenStore = useGetTokenStore();
const code = ref("undefined");
localStorage.setItem("code", code.value);
if (tokenStore.refresh_token === "null" && localStorage.getItem("code") === "undefined") {
  isShow.value = true;
}

function getToken() {
  localStorage.setItem("code", codeInput.value.value);
  if (codeInput.value.value !== "" && tokenStore.access_token === "null") {
    tokenStore.useCodeGetToken().then(async () => {
      const responseText: string = await invoke('get_drive_id', {
        token: JSON.parse(<string>localStorage.getItem("token")).access_token
      });
      localStorage.setItem("drive_id", JSON.parse(responseText).backup_drive_id);
      isLoggedIn.value = true;
      isShow.value = false;
    });
  }
  if (tokenStore.refresh_token === "null" && localStorage.getItem("code") === "undefined") {
    isShow.value = true;
  }
}

const {controlAudioKey} = storeToRefs(useGetAudio());

const loginUrl =
    'https://openapi.alipan.com/oauth/authorize' +
    '?client_id=f3bc86ad8618424d99beb9da421d5526' +
    '&redirect_uri=oob' +
    '&scope=user:base,file:all:read,file:all:write';

const isLoggedIn = ref(false);

// 避免在初始化时 JSON.parse(null) 导致错误
const storedToken = localStorage.getItem("token");
const initialToken = storedToken ? JSON.parse(storedToken) : {access_token: "null"};
const token = ref(initialToken);

function logOut() {
  token.value.access_token = null;
  localStorage.setItem("token", JSON.stringify({access_token: "null"}));
}

watch(token, () => {
  setTimeout(() => {
    const storedToken = localStorage.getItem("token");
    const parsed = storedToken ? JSON.parse(storedToken) : {access_token: "null"};
    isLoggedIn.value = parsed.access_token !== "null";
  }, 300);
}, {immediate: true, deep: true});
</script>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  height: 95vh;
  width: 100vw;
  background-color: var(--md-sys-color-background);
  color: var(--md-sys-color-on-background);
  font-family: 'Roboto', sans-serif;
  overflow: hidden;
}

/* =============================================
   3. 弹窗样式 (Dialog Style) - MD3 规范，圆角直接赋值
   ============================================= */
.scrim {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(0.125rem);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-card {
  width: 85vw;
  max-width: 24rem;
  min-width: 17.5rem;
  background-color: var(--md-sys-color-surface-container-high);

  /* MD3 Dialog 标准圆角 (28dp -> 1.75rem) */
  border-radius: 1.75rem;
  overflow: hidden;

  box-shadow: 0 0.5rem 1rem 0.2rem rgba(0, 0, 0, 0.4);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}


.dialog-header h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 400;
  color: var(--md-sys-color-on-surface);
}

.dialog-text {
  margin: 0;
  font-size: 0.875rem;
  color: var(--md-sys-color-on-surface-variant);
  text-align: center;
  line-height: 1.5;
}

/* 输入框 (MD3 Filled Text Field 风格) */
.input-field-container {
  position: relative;
  margin-top: 0.5rem;
  background-color: rgba(208, 188, 255, 0.08);
  /* Input field top corner radius (8dp -> 0.5rem) */
  border-radius: 0.5rem 0.5rem 0 0;
  border-bottom: 0.0625rem solid var(--md-sys-color-outline);
}

.md-input {
  width: 100%;
  border: none;
  border-bottom: 0.125rem solid transparent;
  background: transparent;
  padding: 1.5rem 1rem 0.5rem;
  font-size: 1rem;
  color: var(--md-sys-color-on-surface);
  outline: none;
  transition: border 0.2s;
}

.md-input:focus {
  border-bottom: 0.125rem solid var(--md-sys-color-primary);
}

.floating-label {
  position: absolute;
  left: 1rem;
  top: 0.5rem;
  font-size: 0.75rem;
  color: var(--md-sys-color-outline);
  pointer-events: none;
  transition: all 0.2s;
}

.md-input:focus + .floating-label,
.md-input:not(:placeholder-shown) + .floating-label {
  color: var(--md-sys-color-primary);
  top: 0.25rem;
}

/* 弹窗按钮组 */
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1.5rem;
}

/* M3 Text Button 样式 */
.md-text-button {
  padding: 0.625rem 1.5rem;
  /* Pill shape */
  border-radius: 9999rem;
  color: var(--md-sys-color-primary);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  transition: background-color 0.2s;
}

.md-text-button:hover {
  background-color: rgba(208, 188, 255, 0.08);
}

/* M3 Filled Button 样式 */
.md-filled-button {
  padding: 0.625rem 1.5rem;
  background-color: var(--md-sys-color-primary);
  color: var(--md-sys-color-on-primary);
  /* Pill shape */
  border-radius: 9999rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 0.0625rem 0.125rem rgba(0, 0, 0, 0.3);
  transition: opacity 0.2s, box-shadow 0.2s;
}

.md-filled-button:active {
  opacity: 0.9;
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.4);
}

/* =============================================
   4. Header & Nav (Top App Bar & Chips)
   ============================================= */
.app-header {
  height: var(--header-height);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 4vw;
  gap: 1rem;
  background-color: var(--md-sys-color-surface);
  border-bottom: 0.0625rem solid rgba(255, 255, 255, 0.05);
  z-index: 10;
}

.nav-items {
  flex: 1;
  display: flex;
  overflow-x: auto;
  gap: 0.5rem;
  scrollbar-width: none;
  justify-content: space-around;
}

.nav-items::-webkit-scrollbar {
  display: none;
}

/* M3 Chip 基础样式 */
.md-chip {
  display: inline-flex;
  align-items: center;
  height: 2rem;
  padding: 0 1rem;
  /* Pill shape */
  border-radius: 9999rem;
  border: 0.0625rem solid var(--md-sys-color-outline);
  background-color: transparent;
  color: var(--md-sys-color-on-surface-variant);
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  white-space: nowrap;
  transition: background-color 0.2s, border-color 0.2s;
  cursor: pointer;
}

/* 激活状态 */
.nav-chip a.router-link-active.md-chip {
  background-color: var(--md-sys-color-secondary-container);
  color: var(--md-sys-color-on-secondary-container);
  border: none;
}

/* 登录/退出 Chip 强调色 */
.login-chip .md-chip {
  border-color: var(--md-sys-color-primary);
  color: var(--md-sys-color-primary);
}

/* =============================================
   5. Main Content & Player
   ============================================= */
.main-content {
  flex: 1;
  overflow-y: auto;
  position: relative;
  padding: 1rem;
  background-color: var(--md-sys-color-background);
}

/* 底部间距 */
.bottom-spacer {
  height: calc(var(--bottom-player-height) + 1rem);
  width: 100%;
}

/* 音频控制栏 */
.audioControl {
  position: fixed;
  left: 0;
  bottom: 0;
  width: 100%;
}

.loading-text {
  color: var(--md-sys-color-on-surface-variant);
  font-size: 0.875rem;
}
</style>