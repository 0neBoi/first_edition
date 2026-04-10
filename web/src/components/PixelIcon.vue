<template>
  <el-icon
    v-if="useVector"
    :class="['app-icon', `app-icon--${size}`]"
    :size="iconSizePx"
  >
    <component :is="vectorComponent" />
  </el-icon>
  <svg
    v-else
    class="pixel-icon"
    :class="[`pixel-icon--${size}`]"
    viewBox="0 0 16 16"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
    focusable="false"
  >
    <g fill="currentColor" shape-rendering="crispEdges" v-html="svgInner" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'
import { PIXEL_ICON_SVG } from '../pixel/icons.js'
import { VECTOR_ICON_BY_NAME } from '../pixel/vectorIconMap.js'

const props = defineProps({
  name: { type: String, default: 'spark' },
  /**
   * sm 20px · md 24px · lg 30px · nav 26px（侧栏）· xl 36px
   * 矢量与像素两套尺寸一致，便于全局替换
   */
  size: { type: String, default: 'md' },
  /** 为 true 时强制使用 16×16 像素格 SVG（默认使用矢量图标） */
  pixel: { type: Boolean, default: false },
})

const ICON_PX = { sm: 20, md: 24, lg: 30, nav: 26, xl: 36 }

const iconSizePx = computed(() => ICON_PX[props.size] ?? ICON_PX.md)

const vectorComponent = computed(() => VECTOR_ICON_BY_NAME[props.name] ?? null)

const useVector = computed(() => !props.pixel && vectorComponent.value != null)

const svgInner = computed(() => PIXEL_ICON_SVG[props.name] || PIXEL_ICON_SVG.spark)
</script>
