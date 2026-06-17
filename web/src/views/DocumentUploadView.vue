<template>
  <section class="page-section upload-page">
    <div class="section-header">
      <div>
        <h2>文档上传</h2>
        <p>支持 PDF、Word、PPT、Excel、Markdown、TXT</p>
      </div>
      <el-button :icon="Files" @click="router.push('/documents')">文档列表</el-button>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="upload-form">
      <el-form-item label="知识库" prop="spaceId">
        <el-select v-model="form.spaceId" class="full-width" placeholder="选择知识库">
          <el-option
            v-for="space in knowledgeStore.spaces"
            :key="space.id"
            :label="space.name"
            :value="space.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="文档标题">
        <el-input v-model="form.title" placeholder="为空时使用文件名" />
      </el-form-item>
      <div class="form-row">
        <el-form-item label="行业">
          <el-input v-model="form.industry" placeholder="例如：金融、教育" />
        </el-form-item>
        <el-form-item label="服务线">
          <el-input v-model="form.serviceLine" placeholder="例如：数据治理" />
        </el-form-item>
        <el-form-item label="密级">
          <el-select v-model="form.confidentialLevel" class="full-width">
            <el-option label="内部" value="INTERNAL" />
            <el-option label="机密" value="CONFIDENTIAL" />
            <el-option label="公开" value="PUBLIC" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="文件" prop="file">
        <el-upload
          drag
          action="#"
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :accept="acceptedTypes"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处或点击选择</div>
          <template #tip>
            <div class="el-upload__tip">文件内容不会入库，仅保存 source_uri。</div>
          </template>
        </el-upload>
      </el-form-item>
      <div class="form-actions">
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">上传并创建解析任务</el-button>
      </div>
    </el-form>

    <el-result
      v-if="result"
      icon="success"
      title="上传完成"
      :sub-title="`文档 ID：${result.documentId}，解析任务：${result.parseTaskId}`"
    >
      <template #extra>
        <el-space>
          <el-tag v-if="result.duplicated" type="warning" effect="plain">重复文件</el-tag>
          <el-tag type="info" effect="plain">{{ result.parseStatus }}</el-tag>
          <el-button type="primary" @click="router.push('/tasks')">查看解析状态</el-button>
        </el-space>
      </template>
    </el-result>
  </section>
</template>

<script setup lang="ts">
import { Files, UploadFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { uploadDocument } from '@/api/documents'
import { apiErrorMessage } from '@/api/http'
import type { DocumentUploadResult } from '@/api/types'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'DocumentUploadView',
})

const router = useRouter()
const userStore = useUserStore()
const knowledgeStore = useKnowledgeStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const error = ref('')
const selectedFile = ref<File | null>(null)
const result = ref<DocumentUploadResult | null>(null)
const acceptedTypes = '.pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.md,.markdown,.txt'

const form = reactive({
  spaceId: knowledgeStore.selectedSpaceId,
  title: '',
  industry: '',
  serviceLine: '',
  confidentialLevel: 'INTERNAL',
  file: '',
})

const rules: FormRules = {
  spaceId: [{ required: true, message: '请选择知识库', trigger: 'change' }],
  file: [{ required: true, message: '请选择文件', trigger: 'change' }],
}

onMounted(async () => {
  if (userStore.tenantId) {
    await knowledgeStore.ensureSpaces(userStore.tenantId)
  }
  form.spaceId = knowledgeStore.selectedSpaceId || knowledgeStore.spaces[0]?.id || null
})

function handleFileChange(file: UploadFile) {
  selectedFile.value = file.raw || null
  form.file = file.name
}

function handleFileRemove() {
  selectedFile.value = null
  form.file = ''
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  if (!selectedFile.value || !form.spaceId) return
  submitting.value = true
  error.value = ''
  result.value = null
  try {
    knowledgeStore.selectSpace(form.spaceId)
    result.value = await uploadDocument({
      spaceId: form.spaceId,
      file: selectedFile.value,
      title: form.title,
      industry: form.industry,
      serviceLine: form.serviceLine,
      confidentialLevel: form.confidentialLevel,
    })
    ElMessage.success('文档已入库')
  } catch (err) {
    error.value = apiErrorMessage(err)
  } finally {
    submitting.value = false
  }
}

function reset() {
  form.title = ''
  form.industry = ''
  form.serviceLine = ''
  form.confidentialLevel = 'INTERNAL'
  form.file = ''
  selectedFile.value = null
  result.value = null
  formRef.value?.clearValidate()
}
</script>
