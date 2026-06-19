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
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button class="w-full mt-2" type="primary" size="large" :loading="submitting" @click="submit">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
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
  username: 'admin',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await userStore.login(form.username, form.password)
    router.push(String(route.query.redirect || '/knowledge-bases'))
  } catch (err: any) {
    ElMessage.error(err.message || '登录失败')
  } finally {
    submitting.value = false
  }
}
</script>
