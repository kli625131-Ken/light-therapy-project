<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary'
  },
  size: {
    type: String,
    default: 'md'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  className: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  }
})

defineEmits(['click'])
</script>

<template>
  <button
    @click="$emit('click')"
    :disabled="disabled"
    :title="title"
    :class="[
      'flex items-center justify-center rounded-lg font-medium transition-all duration-200 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed',
      sizes[size],
      variants[variant],
      className
    ]"
  >
    <slot name="icon" />
    <slot />
  </button>
</template>

<script>
const sizes = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2',
  lg: 'px-6 py-3 text-lg'
}

const variants = {
  primary: 'bg-blue-600 hover:bg-blue-700 text-white shadow-md',
  secondary: 'bg-white hover:bg-gray-50 text-gray-700 border border-gray-300 shadow-sm',
  danger: 'bg-red-50 hover:bg-red-100 text-red-600 border border-red-200',
  ghost: 'bg-transparent hover:bg-gray-100 text-gray-600'
}
</script>
