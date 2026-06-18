<template>
  <main class="grid min-h-screen place-items-center p-6 bg-slate-100">
    <section class="w-full max-w-[440px] p-8 bg-white border border-slate-200 rounded-xl shadow-sm">
      <div class="flex items-center gap-3 mb-6">
        <span class="inline-grid w-8 h-8 text-white bg-emerald-700 rounded-md place-items-center font-bold text-lg">K</span>
        <div>
          <h1 class="m-0 text-lg leading-snug font-bold text-slate-900">企业知识库智能体</h1>
          <p class="m-0 mt-1 text-slate-500 text-sm">MVP 控制台</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="用户 ID" prop="userId">
          <el-input v-model="form.userId" />
        </el-form-item>
        <el-form-item label="租户 ID" prop="tenantId">
          <el-input v-model="form.tenantId" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-button class="w-full mt-2" type="primary" size="large" :loading="submitting" @click="submit">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'LoginView',
})

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  userId: '42',
  tenantId: '1001',
  displayName: 'MVP 用户',
})

const rules: FormRules = {
  userId: [{ required: true, message: '请输入用户 ID', trigger: 'change' }],
  tenantId: [{ required: true, message: '请输入租户 ID', trigger: 'change' }],
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  userStore.login(form)
  submitting.value = false
  router.push(String(route.query.redirect || '/knowledge-bases'))
}
</script>
