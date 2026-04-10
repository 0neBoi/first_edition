<template>
  <!-- 装饰性像素插图：随父级 color / 主题色 -->
  <svg
    class="pixel-hero-bits"
    :class="`pixel-hero-bits--${variant}`"
    viewBox="0 0 96 56"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <g fill="currentColor" shape-rendering="crispEdges" opacity="0.9">
      <template v-if="variant === 'monitor'">
        <rect x="8" y="6" width="52" height="36" />
        <rect x="12" y="10" width="44" height="24" class="ph-inner" />
        <rect x="28" y="44" width="12" height="4" />
        <rect x="24" y="48" width="20" height="4" />
        <rect x="66" y="12" width="4" height="4" />
        <rect x="72" y="18" width="6" height="6" />
        <rect x="78" y="8" width="10" height="14" />
      </template>
      <template v-else-if="variant === 'books'">
        <rect x="6" y="18" width="14" height="30" />
        <rect x="22" y="14" width="14" height="34" />
        <rect x="38" y="20" width="14" height="28" />
        <rect x="54" y="16" width="12" height="32" />
        <rect x="70" y="22" width="10" height="26" />
        <rect x="10" y="12" width="6" height="4" />
        <rect x="26" y="8" width="6" height="4" />
        <rect x="82" y="10" width="8" height="8" />
      </template>
      <template v-else-if="variant === 'settings'">
        <rect x="40" y="4" width="16" height="16" />
        <rect x="44" y="8" width="8" height="8" class="ph-inner" />
        <rect x="46" y="22" width="4" height="28" />
        <rect x="32" y="30" width="32" height="4" />
        <rect x="8" y="12" width="6" height="6" />
        <rect x="8" y="22" width="6" height="6" />
        <rect x="82" y="14" width="8" height="8" />
        <rect x="82" y="26" width="8" height="8" />
      </template>
      <template v-else>
        <rect x="12" y="8" width="56" height="32" />
        <rect x="16" y="12" width="48" height="22" class="ph-inner" />
        <rect x="36" y="42" width="8" height="6" />
        <rect x="72" y="16" width="16" height="20" />
        <rect x="76" y="20" width="8" height="4" />
      </template>
    </g>
  </svg>
</template>

<script setup>
defineProps({
  /** monitor | books | settings | campus */
  variant: { type: String, default: 'campus' },
})
</script>

<style scoped>
.ph-inner {
  fill: color-mix(in srgb, var(--neon-bg-deep) 92%, transparent);
}
</style>
