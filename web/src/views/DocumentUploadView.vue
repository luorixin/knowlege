<template>
  <main class="stitch-upload-page">
    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1 class="font-headline-lg">上传文档</h1>
        <p class="subtitle">支持的格式: PDF, Word, PPT, Excel, Markdown, TXT</p>
      </div>
      <button class="outline-btn" @click="router.push('/documents')">
        查看文档列表
      </button>
    </div>

    <!-- Error Alert -->
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="mb-6"
    />

    <div class="grid-layout">
      <!-- Central Upload Card -->
      <div class="main-column">
        <div class="glass-card">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="upload-form">
            <!-- Knowledge Base & Title -->
            <div class="form-row-2">
              <el-form-item label="知识库" prop="spaceId">
                <el-select v-model="form.spaceId" class="stitch-select full-width" placeholder="选择知识库">
                  <el-option
                    v-for="space in knowledgeStore.spaces"
                    :key="space.id"
                    :label="space.name"
                    :value="space.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="文档标题">
                <el-input v-model="form.title" class="stitch-input" placeholder="为空时使用文件名" />
              </el-form-item>
            </div>

            <!-- Industry, Service Line, Confidentiality -->
            <div class="form-row-3">
              <el-form-item label="行业">
                <el-input v-model="form.industry" class="stitch-input" placeholder="例如：金融" />
              </el-form-item>
              <el-form-item label="服务线">
                <el-input v-model="form.serviceLine" class="stitch-input" placeholder="例如：数据治理" />
              </el-form-item>
              <el-form-item label="密级">
                <el-select v-model="form.confidentialLevel" class="stitch-select full-width">
                  <el-option label="内部" value="INTERNAL" />
                  <el-option label="机密" value="CONFIDENTIAL" />
                  <el-option label="公开" value="PUBLIC" />
                </el-select>
              </el-form-item>
            </div>

            <!-- Dropzone Area -->
            <el-form-item label="文件上传" prop="file">
              <el-upload
                class="stitch-dropzone full-width"
                drag
                action="#"
                :auto-upload="false"
                :limit="1"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                :accept="acceptedTypes"
              >
                <div class="dropzone-content group">
                  <span class="material-symbols-outlined upload-icon">cloud_upload</span>
                  <h3>拖拽文件到此处或点击选择</h3>
                  <p>文件内容不会入库，仅保存 source_uri</p>
                </div>
              </el-upload>
            </el-form-item>

            <!-- Footer Actions -->
            <div class="footer-actions">
              <button class="reset-btn" type="button" @click="reset">重置</button>
              <button class="submit-btn" type="button" :disabled="submitting" @click="submit">
                <span v-if="submitting" class="material-symbols-outlined spin-icon">sync</span>
                上传并解析
              </button>
            </div>
          </el-form>
        </div>
      </div>

      <!-- Right Column: Info & Success State -->
      <div class="side-column">
        <!-- Info Card -->
        <div class="info-card glass-card">
          <h4>上传指南</h4>
          <ul class="guidelines">
            <li>
              <span class="material-symbols-outlined text-primary">check_circle</span>
              <p>请确保文档未加密，以便自动提取元数据。</p>
            </li>
            <li>
              <span class="material-symbols-outlined text-primary">check_circle</span>
              <p>单个文件最大支持 120MB。</p>
            </li>
            <li>
              <span class="material-symbols-outlined text-primary">check_circle</span>
              <p>上传图片或 PDF 会自动触发 OCR 解析。</p>
            </li>
          </ul>
        </div>

        <!-- Success State Card -->
        <transition name="fade-slide">
          <div v-if="result" class="success-card">
            <div class="success-header">
              <div class="success-title">
                <div class="icon-circle">
                  <span class="material-symbols-outlined">check</span>
                </div>
                <span>上传完成</span>
              </div>
            </div>
            <div class="success-body">
              <div class="tags-row">
                <span v-if="result.duplicated" class="tag tag-warning">重复文件</span>
                <span class="tag tag-normal">文档 ID: {{ result.documentId }}</span>
                <span class="tag tag-status">
                  <span class="pulse-dot"></span>
                  状态: {{ result.parseStatus }}
                </span>
              </div>
              <p class="success-desc">文档已加入解析队列，请稍后查看详情。</p>
              <button class="outline-primary-btn" @click="router.push('/tasks')">
                查看解析状态
              </button>
            </div>
          </div>
        </transition>

        <!-- Visual Asset -->
        <div class="visual-asset" v-if="!result">
          <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuAPS6l2cZlintbcgtTbZKyb8Qaal3pZJLg7FkRaSusgnGdZxgNfl4yjyo8MFd8yEgAY-jNKq0bj11e_Ax7fLiWJDecHZQwxsBuk4ETCi5d3Jg_Pxd-Pgl4HhmtlfDSBWxwoHsFTptsoddjPodpPmoo5WFO3XX5CXl7o7gRIINk7B6hm9db3h01QT3USG8JA6vrkYW26IFY30B1tcXZe8VO-J5oSChKPbdSnXrH-vMpfdixSMzvmICTJlHm8h-h2RPgMS4BU6ZMN6mk" alt="Decoration" />
          <div class="visual-overlay"></div>
          <div class="visual-text">
            <p class="visual-tag">功能亮点</p>
            <p class="visual-title">AI 驱动的元数据提取</p>
          </div>
        </div>
      </div>
    </div>
  </main>
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
    router.push('/tasks')
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

<style scoped>
/* Styling to match Stitch */
* {
  box-sizing: border-box;
}
.stitch-upload-page {
  /* max-width: 1280px; */
  margin: 0 auto;
  /* padding: 24px; */
  /* background-color: #faf8ff; */
  color: #131b2e;
  font-family: 'Inter', sans-serif;
  min-height: calc(100vh - 74px);
}
.text-primary { color: #004ac6; }
.full-width { width: 100%; }
.mb-6 { margin-bottom: 24px; }

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}
.font-headline-lg {
  font-size: 24px;
  line-height: 32px;
  font-weight: 600;
  margin: 0 0 4px 0;
}
.subtitle {
  font-size: 14px;
  color: #505f76;
  margin: 0;
}
.outline-btn {
  padding: 8px 16px;
  border: 1px solid #737686;
  border-radius: 8px;
  background: transparent;
  color: #004ac6;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.outline-btn:hover {
  background-color: #f2f3ff;
}

/* Layout */
.grid-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 32px;
}
@media (max-width: 1024px) {
  .grid-layout { grid-template-columns: 1fr; }
}

/* Glass Card */
.glass-card {
  background-color: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid #c3c6d7;
  border-radius: 16px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05);
  padding: 32px;
}

/* Forms */
.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.form-row-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
@media (max-width: 768px) {
  .form-row-2, .form-row-3 { grid-template-columns: 1fr; }
}

:deep(.el-form-item__label) {
  font-size: 12px;
  font-weight: 500;
  color: #434655;
  padding-bottom: 8px;
}

:deep(.stitch-select .el-select__wrapper),
:deep(.stitch-input .el-input__wrapper) {
  background-color: #ffffff;
  border: 1px solid #c3c6d7;
  border-radius: 8px;
  box-shadow: none !important;
  min-height: 44px;
  padding: 8px 16px;
}
:deep(.stitch-select .el-select__wrapper.is-focused),
:deep(.stitch-input .el-input__wrapper.is-focus) {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1) !important;
}

/* Dropzone */
:deep(.stitch-dropzone .el-upload-dragger) {
  background-color: transparent;
  border: 2px dashed #c3c6d7;
  border-radius: 16px;
  padding: 48px;
  transition: all 0.3s;
}
:deep(.stitch-dropzone .el-upload-dragger:hover),
:deep(.stitch-dropzone .el-upload-dragger.is-dragover) {
  border-color: rgba(0, 74, 198, 0.5);
  background-color: rgba(0, 74, 198, 0.05);
}
.dropzone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.upload-icon {
  font-size: 64px;
  color: rgba(0, 74, 198, 0.4);
  transition: transform 0.3s, color 0.3s;
}
:deep(.el-upload-dragger:hover) .upload-icon {
  color: #004ac6;
  transform: scale(1.1);
}
.dropzone-content h3 {
  font-size: 18px;
  font-weight: 500;
  color: #131b2e;
  margin: 0;
}
.dropzone-content p {
  font-size: 14px;
  color: #505f76;
  margin: 0;
}

/* Footer Actions */
.footer-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
  margin-top: 24px;
}
.reset-btn {
  background: transparent;
  border: none;
  font-size: 14px;
  font-weight: 500;
  color: #505f76;
  cursor: pointer;
  padding: 8px 16px;
}
.reset-btn:hover {
  color: #131b2e;
}
.submit-btn {
  background-color: #004ac6;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 32px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.submit-btn:hover:not(:disabled) {
  background-color: #2563eb;
}
.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}
.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.spin-icon {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  100% { transform: rotate(360deg); }
}

/* Info Card */
.side-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.info-card h4 {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 16px 0;
}
.guidelines {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.guidelines li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.guidelines p {
  font-size: 14px;
  color: #434655;
  margin: 0;
  line-height: 1.5;
}

/* Success Card */
.success-card {
  background-color: #ffffff;
  border-radius: 16px;
  border: 1px solid rgba(0, 74, 198, 0.2);
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1);
  overflow: hidden;
}
.success-header {
  background-color: rgba(0, 74, 198, 0.1);
  padding: 16px 24px;
  border-bottom: 1px solid rgba(0, 74, 198, 0.1);
}
.success-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #004ac6;
  font-weight: 600;
  font-size: 14px;
}
.icon-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #004ac6;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}
.icon-circle span {
  font-size: 18px;
}
.success-body {
  padding: 24px;
}
.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.tag-warning {
  background-color: #ffdad6;
  color: #93000a;
}
.tag-normal {
  background-color: #e2e7ff;
  color: #434655;
}
.tag-status {
  background-color: #d0e1fb;
  color: #54647a;
  display: flex;
  align-items: center;
  gap: 6px;
}
.pulse-dot {
  width: 6px;
  height: 6px;
  background-color: #b7c8e1;
  border-radius: 50%;
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.4; }
  100% { opacity: 1; }
}
.success-desc {
  font-size: 14px;
  color: #505f76;
  margin: 0 0 16px 0;
  line-height: 1.5;
}
.outline-primary-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid #004ac6;
  border-radius: 8px;
  background: transparent;
  color: #004ac6;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.outline-primary-btn:hover {
  background-color: rgba(0, 74, 198, 0.05);
}

.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.5s ease;
}
.fade-slide-enter-from, .fade-slide-leave-to {
  opacity: 0;
  transform: translateY(16px);
}

/* Visual Asset */
.visual-asset {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  aspect-ratio: 4/3;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.visual-asset img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.7s;
}
.visual-asset:hover img {
  transform: scale(1.05);
}
.visual-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.6), transparent);
}
.visual-text {
  position: absolute;
  bottom: 24px;
  left: 24px;
  color: white;
}
.visual-tag {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  opacity: 0.8;
  margin: 0 0 4px 0;
}
.visual-title {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}
</style>
