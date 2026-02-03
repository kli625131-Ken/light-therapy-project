<script setup>
defineProps({
  className: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <div :class="['bg-white rounded-xl shadow-sm border border-gray-100', className]">
    <slot />
  </div>
</template>
