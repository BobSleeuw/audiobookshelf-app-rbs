<template>
  <div class="flex items-center h-12 px-3 bg-primary border-b border-fg/10 flex-shrink-0">
    <button v-if="back" type="button" class="mr-2 flex items-center" @click="$emit('back')"><span class="material-symbols">arrow_back</span></button>
    <p class="text-lg font-semibold truncate">{{ title }}</p>
  </div>
</template>

<script>
export default {
  props: {
    title: String,
    back: Boolean
  }
}
</script>
