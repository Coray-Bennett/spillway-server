<template>
    <svg
      :width="size"
      :height="size"
      :viewBox="viewBox"
      :class="['base-icon', `icon-${name}`, className]"
      :aria-label="ariaLabel"
    >
      <use :href="`#icon-${name}`" />
    </svg>
  </template>
  
  <script setup>
  defineProps({
    name: {
      type: String,
      required: true
    },
    size: {
      type: [String, Number],
      default: 24
    },
    className: {
      type: String,
      default: ''
    },
    viewBox: {
      type: String,
      default: '0 0 24 24'
    },
    ariaLabel: {
      type: String,
      default: null
    }
  })
  </script>
  
  <style scoped>
  .base-icon {
    fill: currentColor;
    display: inline-block;
    vertical-align: middle;
    transition: all 0.2s ease;
  }
  </style>
  