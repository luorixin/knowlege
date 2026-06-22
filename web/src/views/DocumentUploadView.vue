<template>
  <div class="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[500px] text-white">
    <!-- Outer Banner title -->
    <div class="text-center pb-4 border-b border-white/[0.08] mb-6 shrink-0">
      <h1 class="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase m-0">
        Data Matrix Injection
      </h1>
    </div>

    <!-- Page Header -->
    <div class="cyber-panel rounded-2xl p-4 mb-6 border border-white/[0.06] flex flex-col md:flex-row md:items-center justify-between gap-4 shrink-0">
      <div class="flex flex-col gap-1">
        <h2 class="text-lg font-bold text-white m-0 tracking-tight">Data Upload Protocol</h2>
        <p class="text-[10px] font-mono text-slate-500 uppercase tracking-widest m-0 flex items-center gap-1">
          <span class="material-symbols-outlined text-[12px]">info</span>
          ACCEPTED FORMATS: PDF, DOC, PPT, XLS, MD, TXT
        </p>
      </div>
      <button @click="router.push('/documents')" class="py-2 px-4 rounded-lg bg-slate-900 border border-slate-700 hover:border-neon-cyan hover:text-neon-cyan text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer outline-none uppercase tracking-wider">
        <span class="material-symbols-outlined text-[14px]">view_list</span>
        View Databanks
      </button>
    </div>

    <!-- Error Alert -->
    <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-950/40 border border-red-500/30 text-red-400 text-xs font-mono flex items-center gap-2 shrink-0">
      <span class="material-symbols-outlined text-[16px]">error</span>
      {{ error }}
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 flex-1 min-h-0 overflow-y-auto scrollbar-thin pr-2">
      <!-- Central Upload Card -->
      <div class="lg:col-span-2">
        <div class="cyber-panel rounded-2xl p-6 border border-white/[0.06] relative overflow-hidden h-full">
          <div class="absolute right-[-20px] top-[-20px] text-slate-800 opacity-20 text-[120px] font-black material-symbols-outlined pointer-events-none">cloud_upload</div>
          
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="cyber-form relative z-10 flex flex-col h-full">
            <!-- Knowledge Base & Title -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <el-form-item label="Target Nexus" prop="spaceId" class="cyber-form-item">
                <div class="relative w-full">
                   <select
                     v-model="form.spaceId"
                     class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-neon-cyan focus:border-neon-cyan focus:outline-none pr-8 appearance-none cursor-pointer uppercase tracking-wider"
                   >
                     <option v-for="space in knowledgeStore.spaces" :key="space.id" :value="space.id">
                       {{ space.name }}
                     </option>
                   </select>
                   <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-[16px] text-neon-cyan pointer-events-none">expand_more</span>
                </div>
              </el-form-item>
              <el-form-item label="Packet Alias (Optional)" class="cyber-form-item">
                <input v-model="form.title" type="text" class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-white focus:border-neon-cyan focus:outline-none transition-colors placeholder-slate-600" placeholder="Defaults to filename" />
              </el-form-item>
            </div>

            <!-- Industry, Service Line, Confidentiality -->
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <el-form-item label="Sector" class="cyber-form-item">
                <input v-model="form.industry" type="text" class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-white focus:border-neon-cyan focus:outline-none transition-colors placeholder-slate-600" placeholder="E.g. Finance" />
              </el-form-item>
              <el-form-item label="Vector" class="cyber-form-item">
                <input v-model="form.serviceLine" type="text" class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-white focus:border-neon-cyan focus:outline-none transition-colors placeholder-slate-600" placeholder="E.g. Engineering" />
              </el-form-item>
              <el-form-item label="Security Clearance" class="cyber-form-item">
                <div class="relative w-full">
                   <select
                     v-model="form.confidentialLevel"
                     class="w-full bg-slate-950 px-3 py-2.5 rounded border border-white/[0.1] text-xs font-mono text-neon-cyan focus:border-neon-cyan focus:outline-none pr-8 appearance-none cursor-pointer uppercase tracking-wider"
                   >
                     <option value="INTERNAL">Internal</option>
                     <option value="CONFIDENTIAL">Confidential</option>
                     <option value="PUBLIC">Public</option>
                   </select>
                   <span class="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 text-[16px] text-neon-cyan pointer-events-none">expand_more</span>
                </div>
              </el-form-item>
            </div>

            <!-- Dropzone Area -->
            <el-form-item label="Payload Injection" prop="file" class="cyber-form-item flex-1 flex flex-col">
              <el-upload
                class="cyber-dropzone flex-1"
                drag
                action="#"
                :auto-upload="false"
                :limit="1"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                :accept="acceptedTypes"
              >
                <div class="flex flex-col items-center justify-center h-full gap-3 p-8">
                  <span class="material-symbols-outlined text-[64px] text-neon-cyan/50 group-hover:text-neon-cyan transition-colors duration-300">data_object</span>
                  <h3 class="text-sm font-mono uppercase tracking-widest text-slate-200 m-0">Drag Payload Here or Click</h3>
                  <p class="text-[10px] font-mono text-slate-500 m-0 text-center max-w-[80%] leading-relaxed">
                    Data will be processed into vector chunks. The original file content is not permanently retained; only source references are kept.
                  </p>
                </div>
              </el-upload>
            </el-form-item>

            <!-- Footer Actions -->
            <div class="flex justify-end items-center gap-4 mt-6 pt-6 border-t border-white/[0.05]">
              <button class="px-4 py-2 bg-transparent text-slate-400 border border-slate-700 hover:text-white hover:border-slate-500 rounded text-xs font-mono uppercase tracking-wider transition-colors outline-none cursor-pointer" type="button" @click="reset">
                Reset
              </button>
              <button class="px-6 py-2 bg-neon-cyan text-slate-900 border border-neon-cyan hover:bg-cyan-400 hover:shadow-[0_0_15px_rgba(0,240,255,0.4)] rounded text-xs font-mono font-bold uppercase tracking-wider transition-all flex items-center gap-2 outline-none cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed" type="button" :disabled="submitting" @click="submit">
                <span v-if="submitting" class="material-symbols-outlined text-[14px] animate-spin">sync</span>
                <span v-else class="material-symbols-outlined text-[14px]">rocket_launch</span>
                Inject & Parse
              </button>
            </div>
          </el-form>
        </div>
      </div>

      <!-- Right Column: Info & Success State -->
      <div class="flex flex-col gap-6">
        <!-- Info Card -->
        <div class="cyber-panel rounded-2xl p-5 border border-white/[0.06]">
          <h4 class="text-xs font-mono font-bold uppercase tracking-widest text-white m-0 mb-4 flex items-center gap-2 pb-3 border-b border-white/[0.05]">
            <span class="material-symbols-outlined text-neon-cyan text-[16px]">menu_book</span>
            Injection Guidelines
          </h4>
          <ul class="flex flex-col gap-3 m-0 p-0 list-none">
            <li class="flex items-start gap-2">
              <span class="material-symbols-outlined text-neon-cyan text-[14px] mt-0.5">check_circle</span>
              <p class="text-[11px] font-mono text-slate-400 m-0 leading-relaxed">Ensure documents are unencrypted to allow automated metadata extraction.</p>
            </li>
            <li class="flex items-start gap-2">
              <span class="material-symbols-outlined text-neon-cyan text-[14px] mt-0.5">check_circle</span>
              <p class="text-[11px] font-mono text-slate-400 m-0 leading-relaxed">Maximum payload size is limited to 120MB per packet.</p>
            </li>
            <li class="flex items-start gap-2">
              <span class="material-symbols-outlined text-neon-cyan text-[14px] mt-0.5">check_circle</span>
              <p class="text-[11px] font-mono text-slate-400 m-0 leading-relaxed">Image or PDF uploads will automatically trigger the OCR matrix.</p>
            </li>
          </ul>
        </div>

        <!-- Success State Card -->
        <transition name="fade-slide">
          <div v-if="result" class="cyber-panel rounded-2xl border border-green-500/30 overflow-hidden relative group">
             <div class="absolute inset-0 bg-green-500/5 group-hover:bg-green-500/10 transition-colors pointer-events-none"></div>
             <div class="p-4 border-b border-green-500/20 bg-green-950/30 flex items-center gap-3">
               <div class="w-8 h-8 rounded-full bg-green-500/20 border border-green-500/50 flex items-center justify-center text-green-400">
                 <span class="material-symbols-outlined text-[18px]">done_all</span>
               </div>
               <span class="text-xs font-mono font-bold text-green-400 uppercase tracking-widest">Injection Complete</span>
             </div>
             <div class="p-5">
               <div class="flex flex-wrap gap-2 mb-4">
                 <span v-if="result.duplicated" class="px-2 py-0.5 rounded bg-orange-950/50 text-orange-400 border border-orange-500/50 text-[9px] font-mono uppercase font-bold tracking-wider">Duplicate Signature</span>
                 <span class="px-2 py-0.5 rounded bg-slate-900 text-slate-300 border border-white/[0.1] text-[9px] font-mono uppercase font-bold tracking-wider">ID: {{ result.documentId }}</span>
                 <span class="px-2 py-0.5 rounded bg-blue-950/50 text-blue-400 border border-blue-500/50 text-[9px] font-mono uppercase font-bold tracking-wider flex items-center gap-1.5">
                   <span class="w-1.5 h-1.5 rounded-full bg-blue-400 animate-pulse"></span>
                   Phase: {{ result.parseStatus }}
                 </span>
               </div>
               <p class="text-[11px] font-mono text-slate-400 leading-relaxed m-0 mb-4">
                 Payload has entered the processing queue. Telemetry will be available shortly.
               </p>
               <button class="w-full py-2.5 rounded border border-green-500/50 text-green-400 hover:bg-green-500 hover:text-slate-900 text-xs font-mono font-bold uppercase tracking-wider transition-colors outline-none cursor-pointer" @click="router.push('/tasks')">
                 Monitor Tasks
               </button>
             </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
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
  spaceId: [{ required: true, message: 'Select a target nexus', trigger: 'change' }],
  file: [{ required: true, message: 'Provide a payload', trigger: 'change' }],
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
    ElMessage.success('Payload injected successfully')
    // We intentionally don't redirect so user sees the success state card
    // router.push('/tasks')
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

<style>
/* Transitions */
.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.5s ease;
}
.fade-slide-enter-from, .fade-slide-leave-to {
  opacity: 0;
  transform: translateY(16px);
}

/* Scrollbar */
.scrollbar-thin::-webkit-scrollbar {
  width: 4px;
}
.scrollbar-thin::-webkit-scrollbar-track {
  background: transparent;
}
.scrollbar-thin::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}
.scrollbar-thin:hover::-webkit-scrollbar-thumb {
  background-color: rgba(0, 240, 255, 0.3);
}

/* Cyber Form Element Plus Overrides */
.cyber-form-item .el-form-item__label {
  font-size: 10px !important;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace !important;
  color: #94a3b8 !important;
  text-transform: uppercase !important;
  letter-spacing: 0.05em !important;
  padding-bottom: 4px !important;
  line-height: 1 !important;
}

/* Cyber Dropzone */
.cyber-dropzone {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.cyber-dropzone .el-upload {
  height: 100%;
}
.cyber-dropzone .el-upload-dragger {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background-color: rgba(2, 6, 23, 0.5) !important;
  border: 1px dashed rgba(255, 255, 255, 0.1) !important;
  border-radius: 0.5rem !important;
  transition: all 0.3s ease !important;
}
.cyber-dropzone .el-upload-dragger:hover,
.cyber-dropzone .el-upload-dragger.is-dragover {
  border-color: rgba(0, 240, 255, 0.5) !important;
  background-color: rgba(0, 240, 255, 0.05) !important;
}
.cyber-dropzone .el-upload-list {
  margin-top: 1rem;
}
.cyber-dropzone .el-upload-list__item {
  background-color: rgba(15, 23, 42, 0.8) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 0.5rem !important;
  margin-bottom: 0.5rem !important;
}
.cyber-dropzone .el-upload-list__item-name {
  color: #00f0ff !important;
  font-family: ui-monospace, SFMono-Regular, monospace !important;
  font-size: 11px !important;
}
.cyber-dropzone .el-upload-list__item:hover {
  background-color: rgba(15, 23, 42, 1) !important;
  border-color: rgba(0, 240, 255, 0.3) !important;
}
</style>
