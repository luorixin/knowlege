<template>
  <div class="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[500px] text-white">
    <!-- Outer Banner title -->
    <div class="text-center pb-4 border-b border-white/[0.08] mb-4 shrink-0">
      <h1 class="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase m-0">
        Data Nexus Registry
      </h1>
    </div>

    <!-- Toolbar -->
    <div class="cyber-panel rounded-2xl p-4 mb-5 border border-white/[0.06] flex flex-col md:flex-row md:items-center justify-between gap-4 shrink-0">
      <div class="flex flex-col gap-1">
        <h2 class="text-lg font-bold text-white m-0 tracking-tight">Knowledge Spaces</h2>
        <p class="text-[10px] font-mono text-slate-500 uppercase tracking-widest m-0">Tenant ID: {{ userStore.tenantId }}</p>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <button @click="refresh" :disabled="knowledgeStore.loading" class="py-2 px-3 rounded-lg border border-white/[0.08] hover:border-neon-cyan hover:text-neon-cyan text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50 outline-none">
          <span class="material-symbols-outlined text-[14px]" :class="{ 'animate-spin': knowledgeStore.loading }">refresh</span>
          <span>Sync</span>
        </button>
        <button @click="dialogVisible = true" class="py-2 px-4 rounded-lg bg-neon-cyan text-slate-900 border border-neon-cyan hover:bg-cyan-400 hover:shadow-[0_0_15px_rgba(0,240,255,0.4)] text-xs font-mono font-bold transition-all flex items-center gap-1.5 cursor-pointer uppercase tracking-wider outline-none">
          <span class="material-symbols-outlined text-[14px]">add</span>
          New Nexus
        </button>
      </div>
    </div>

    <!-- Alert -->
    <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-950/40 border border-red-500/30 text-red-400 text-xs font-mono flex items-center gap-2 shrink-0">
      <span class="material-symbols-outlined text-[16px]">error</span>
      {{ error }}
    </div>

    <div v-loading="knowledgeStore.loading" class="flex-1 min-h-[200px] overflow-auto scrollbar-thin p-1">
      <div v-if="knowledgeStore.spaces.length === 0 && !knowledgeStore.loading" class="flex flex-col items-center justify-center text-slate-500 min-h-[300px] border border-dashed border-white/[0.1] rounded-2xl bg-slate-950/30">
        <span class="material-symbols-outlined text-6xl mb-4 opacity-50">schema</span>
        <p class="font-mono text-sm uppercase tracking-wider mb-4">No Enclaves Found</p>
        <button @click="dialogVisible = true" class="py-2 px-4 rounded border border-neon-cyan text-neon-cyan hover:bg-neon-cyan/10 text-xs font-mono uppercase transition-colors outline-none cursor-pointer">
          Initialize First Nexus
        </button>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <div 
          v-for="space in knowledgeStore.spaces" 
          :key="space.id" 
          class="group relative cyber-panel rounded-xl p-5 border border-white/[0.06] hover:border-neon-cyan/50 transition-all cursor-pointer flex flex-col h-56 bg-gradient-to-br hover:from-cyan-950/20 hover:to-transparent"
          @click="openDetail(space.id)"
        >
          <!-- Corner Accents -->
          <div class="absolute top-0 left-0 w-2 h-2 border-t border-l border-white/[0.2] group-hover:border-neon-cyan transition-colors rounded-tl-xl"></div>
          <div class="absolute top-0 right-0 w-2 h-2 border-t border-r border-white/[0.2] group-hover:border-neon-cyan transition-colors rounded-tr-xl"></div>
          <div class="absolute bottom-0 left-0 w-2 h-2 border-b border-l border-white/[0.2] group-hover:border-neon-cyan transition-colors rounded-bl-xl"></div>
          <div class="absolute bottom-0 right-0 w-2 h-2 border-b border-r border-white/[0.2] group-hover:border-neon-cyan transition-colors rounded-br-xl"></div>
          
          <div class="flex justify-between items-start mb-4 relative z-10">
            <h3 class="text-base font-bold text-white m-0 line-clamp-1 group-hover:text-neon-cyan transition-colors flex-1" :title="space.name">{{ space.name }}</h3>
            <span class="text-[9px] font-mono font-bold px-2 py-0.5 rounded border uppercase ml-2 shrink-0" 
                  :class="space.status === 'ACTIVE' ? 'bg-green-950/30 text-green-400 border-green-500/50' : 'bg-slate-800 border-slate-600 text-slate-300'">
              {{ space.status }}
            </span>
          </div>
          
          <p class="text-xs text-slate-400 flex-1 mb-5 line-clamp-3 leading-relaxed relative z-10" :title="space.description">
            {{ space.description || 'No description provided.' }}
          </p>
          
          <div class="flex items-center justify-between border-t border-white/[0.05] pt-4 mt-auto relative z-10">
            <div class="flex gap-2">
              <span class="text-[9px] font-mono px-1.5 py-0.5 rounded border bg-transparent text-slate-500 border-white/[0.1] uppercase">
                {{ space.visibility === 'PRIVATE' ? 'PRIVATE' : space.visibility }}
              </span>
            </div>
            <div class="flex gap-2" @click.stop>
              <button class="text-[10px] font-mono text-neon-cyan hover:text-white px-2 py-1 rounded bg-cyan-950/30 border border-neon-cyan/30 hover:bg-neon-cyan hover:border-neon-cyan outline-none cursor-pointer transition-colors" @click="selectAndUpload(space.id)">UPLOAD</button>
              <button class="text-[10px] font-mono text-purple-400 hover:text-white px-2 py-1 rounded bg-purple-950/30 border border-purple-500/30 hover:bg-purple-500 hover:border-purple-500 outline-none cursor-pointer transition-colors" @click="selectAndChat(space.id)">QUERY</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="dialogVisible" title="Initialize Nexus" width="480px" class="cyber-dialog" :show-close="false">
      <template #header="{ titleId, titleClass }">
         <div class="flex items-center justify-between border-b border-white/[0.08] pb-3 mb-2">
            <h4 :id="titleId" :class="titleClass" class="!m-0 !text-sm font-mono uppercase tracking-widest text-white flex items-center gap-2">
              <span class="material-symbols-outlined text-neon-cyan text-[18px]">add_box</span>
              Initialize Nexus
            </h4>
            <button @click="dialogVisible = false" class="text-slate-500 hover:text-white cursor-pointer outline-none">
              <span class="material-symbols-outlined text-[18px]">close</span>
            </button>
         </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="cyber-form mt-2">
        <el-form-item label="IDENTIFIER (Name)" prop="name">
          <el-input v-model="form.name" maxlength="128" show-word-limit placeholder="Enter nexus alias" class="cyber-input" />
        </el-form-item>
        <el-form-item label="PARAMETERS (Description)">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1024" show-word-limit placeholder="Describe the knowledge domain" class="cyber-textarea" />
        </el-form-item>
        <el-form-item label="SECURITY CLEARANCE (Visibility)">
          <el-select v-model="form.visibility" class="w-full">
            <el-option label="PRIVATE" value="PRIVATE" />
            <el-option label="TENANT WIDE" value="TENANT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3 pt-3 border-t border-white/[0.08]">
          <button @click="dialogVisible = false" class="px-4 py-2 rounded bg-slate-800 text-white text-xs font-mono border border-slate-700 hover:bg-slate-700 transition-colors cursor-pointer outline-none uppercase tracking-wider">
            Abort
          </button>
          <button @click="createSpace" :disabled="creating" class="px-4 py-2 rounded bg-neon-cyan text-slate-900 border border-neon-cyan hover:bg-cyan-400 transition-colors text-xs font-mono font-bold flex items-center gap-1 cursor-pointer outline-none uppercase tracking-wider disabled:opacity-50">
            <span class="material-symbols-outlined text-[14px]" :class="{ 'animate-spin': creating }">{{ creating ? 'refresh' : 'check' }}</span>
            Commit
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { apiErrorMessage } from '@/api/http'
import type { EntityId } from '@/api/types'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'KnowledgeBasesView',
})

const router = useRouter()
const userStore = useUserStore()
const knowledgeStore = useKnowledgeStore()
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const creating = ref(false)
const error = ref('')

const form = reactive({
  name: '',
  description: '',
  visibility: 'PRIVATE',
})

const rules: FormRules = {
  name: [{ required: true, message: 'Please input nexus identifier', trigger: 'blur' }],
}

onMounted(refresh)

async function refresh() {
  if (!userStore.tenantId) return
  error.value = ''
  try {
    await knowledgeStore.fetchSpaces(userStore.tenantId)
  } catch (err) {
    error.value = apiErrorMessage(err)
  }
}

async function createSpace() {
  if (!formRef.value || !userStore.tenantId || !userStore.userId) return
  await formRef.value.validate()
  creating.value = true
  try {
    const space = await knowledgeStore.createSpace({
      tenantId: userStore.tenantId,
      ownerUserId: userStore.userId,
      name: form.name,
      description: form.description,
      visibility: form.visibility,
    })
    dialogVisible.value = false
    form.name = ''
    form.description = ''
    ElMessage.success('Nexus initialized successfully')
    router.push(`/knowledge-bases/${space.id}`)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err))
  } finally {
    creating.value = false
  }
}

function openDetail(spaceId: EntityId) {
  knowledgeStore.selectSpace(spaceId)
  router.push(`/knowledge-bases/${spaceId}`)
}

function selectAndChat(spaceId: EntityId) {
  knowledgeStore.selectSpace(spaceId)
  router.push('/chat')
}

function selectAndUpload(spaceId: EntityId) {
  knowledgeStore.selectSpace(spaceId)
  router.push('/documents/upload')
}
</script>

<style>
/* Custom scrollbar for cyber components */
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

/* ElDialog Cyber overrides */
.cyber-dialog {
  background: #020617 !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
  border-radius: 1rem !important;
  box-shadow: 0 0 40px rgba(0,0,0,0.8), 0 0 0 1px rgba(0,240,255,0.1) !important;
}
.cyber-dialog .el-dialog__header {
  padding: 1.5rem 1.5rem 0.5rem !important;
  margin-right: 0 !important;
}
.cyber-dialog .el-dialog__body {
  padding: 1rem 1.5rem !important;
}
.cyber-dialog .el-dialog__footer {
  padding: 0 1.5rem 1.5rem !important;
}

/* ElForm Cyber overrides */
.cyber-form .el-form-item__label {
  color: #94a3b8 !important;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace !important;
  font-size: 0.625rem !important;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding-bottom: 4px !important;
  line-height: 1.2 !important;
}

.cyber-form .el-input__wrapper,
.cyber-form .el-textarea__inner {
  background-color: #020617 !important;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.08) inset !important;
  border-radius: 0.5rem !important;
}

.cyber-form .el-input__wrapper.is-focus,
.cyber-form .el-textarea__inner:focus {
  box-shadow: 0 0 0 1px #00f0ff inset !important;
}

.cyber-form .el-input__inner,
.cyber-form .el-textarea__inner {
  color: #fff !important;
  font-family: ui-monospace, SFMono-Regular, monospace !important;
  font-size: 0.75rem !important;
}

/* ElSelect Dropdown overrides */
.el-select-dropdown {
  background: #020617 !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
}
.el-select-dropdown__item {
  color: #94a3b8 !important;
  font-family: ui-monospace, SFMono-Regular, monospace !important;
  font-size: 0.75rem !important;
}
.el-select-dropdown__item.hover,
.el-select-dropdown__item:hover {
  background-color: rgba(255,255,255,0.05) !important;
}
.el-select-dropdown__item.selected {
  color: #00f0ff !important;
  font-weight: 700 !important;
  background-color: rgba(0,240,255,0.1) !important;
}
</style>
