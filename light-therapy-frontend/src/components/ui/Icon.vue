<template>
  <component :is="iconComponent" :class="$attrs.class" />
</template>

<script setup>
import { computed } from 'vue'

// 全量引入lucide-vue-next图标
import * as LucideIcons from 'lucide-vue-next'

const props = defineProps({
  name: {
    type: String,
    required: true,
    description: '图标名称，对应lucide-vue-next图标库中的图标名称'
  }
})

// 根据name属性动态获取对应的图标组件
const iconComponent = computed(() => {
  const iconName = props.name.charAt(0).toUpperCase() + props.name.slice(1)
  return LucideIcons[iconName] || LucideIcons.AlertCircle
})
</script>