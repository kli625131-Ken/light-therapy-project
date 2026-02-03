<template>
  <div v-if="isOpen" class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in duration-200">
    <div :class="['bg-white rounded-xl shadow-2xl w-full overflow-hidden flex flex-col max-h-[90vh]', {
      'max-w-lg': size === 'md',
      'max-w-4xl': size === 'lg'
    }]">
      <div class="flex justify-between items-center p-4 border-b border-gray-100 bg-gray-50">
        <h3 class="font-bold text-gray-800">{{ title }}</h3>
        <button v-if="!preventClose" @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <X :size="20" />
        </button>
      </div>
      <div class="p-6 overflow-y-auto">
        <slot />
      </div>
    </div>
  </div>
</template>

<script>
import { X } from 'lucide-vue-next'

export default {
  components: {
    X
  },
  props: {
    isOpen: {
      type: Boolean,
      required: true
    },
    title: {
      type: String,
      default: ''
    },
    preventClose: {
      type: Boolean,
      default: false
    },
    size: {
      type: String,
      default: 'md'
    }
  },
  emits: ['close']
}
</script>

<style scoped>
.max-w-lg {
  max-width: 32rem;
}
.max-w-4xl {
  max-width: 56rem;
}
</style>
