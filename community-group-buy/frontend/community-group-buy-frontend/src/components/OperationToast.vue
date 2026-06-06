<script setup>
import { dismissNotification, useNotifications } from '../utils/notify'

const notices = useNotifications()
</script>

<template>
  <Teleport to="body">
    <TransitionGroup name="operation-toast" tag="div" class="operation-toast-region">
      <article
        v-for="notice in notices"
        :key="notice.id"
        class="operation-toast"
        :class="`is-${notice.type}`"
        role="status"
      >
        <span class="operation-toast-mark"></span>
        <div class="operation-toast-body">
          <strong>{{ notice.title }}</strong>
          <p>{{ notice.message }}</p>
        </div>
        <button
          class="operation-toast-close"
          type="button"
          aria-label="关闭通知"
          @click="dismissNotification(notice.id)"
        >
          ×
        </button>
      </article>
    </TransitionGroup>
  </Teleport>
</template>
