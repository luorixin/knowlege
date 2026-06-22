<template>
  <div class="font-sans space-y-6 text-white max-w-[1400px] mx-auto py-8">
    
    <!-- Visual Title Header (Matches Image 5) -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/[0.08] pb-6">
      <div>
        <h1 class="font-display text-2xl md:text-3xl font-bold text-white tracking-wide">
          Cybersecurity Permissions Console
        </h1>
        <p class="text-xs text-slate-400 font-mono uppercase tracking-wider mt-1 flex items-center gap-2">
          <span class="w-1.5 h-1.5 rounded-full bg-neon-purple animate-pulse"></span>
          Virtual Guard Node: <span class="text-neon-purple">ACTIVE-WG-09</span>
        </p>
      </div>

      <!-- Neon Cyan "Add Member" Button (Image 5) -->
      <button
        @click="handleAdd"
        class="self-start sm:self-center px-4.5 py-2 rounded-xl bg-cyan-400 text-slate-950 font-display font-bold text-xs tracking-wider transition-all hover:bg-neon-cyan hover:shadow-[0_0_15px_rgba(0,240,255,0.4)] active:scale-95 cursor-pointer flex items-center gap-1.5"
      >
        <span class="material-symbols-outlined text-[18px]">person_add</span>
        <span>Add {{ currentTabName }}</span>
      </button>
    </div>

    <!-- Segmented Controller navigation tabs (Image 5) -->
    <div class="flex items-center space-x-1 border-b border-white/[0.05]">
      <button
        @click="activeTab = 'members'; refreshData()"
        :class="[
          'py-3 px-4.5 text-xs font-mono tracking-wider transition-all relative border-b-2 cursor-pointer',
          activeTab === 'members' ? 'border-neon-cyan text-neon-cyan font-bold' : 'border-transparent text-slate-400 hover:text-white'
        ]"
      >
        Space Members
      </button>
      <button
        @click="activeTab = 'roles'; refreshData()"
        :class="[
          'py-3 px-4.5 text-xs font-mono tracking-wider transition-all relative border-b-2 cursor-pointer',
          activeTab === 'roles' ? 'border-neon-cyan text-neon-cyan font-bold' : 'border-transparent text-slate-400 hover:text-white'
        ]"
      >
        Roles
      </button>
      <button
        @click="activeTab = 'policies'; refreshData()"
        :class="[
          'py-3 px-4.5 text-xs font-mono tracking-wider transition-all relative border-b-2 cursor-pointer',
          activeTab === 'policies' ? 'border-neon-cyan text-neon-cyan font-bold' : 'border-transparent text-slate-400 hover:text-white'
        ]"
      >
        Policies
      </button>
    </div>

    <!-- Space Members Tab (Matches screenshot 5 layout) -->
    <div v-if="activeTab === 'members'" class="space-y-4">
      
      <!-- Member Search filter -->
      <div class="relative max-w-sm">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[16px] text-slate-500">search</span>
        <input
          type="text"
          placeholder="Filter members by name..."
          v-model="searchTerm"
          class="w-full text-xs text-white pl-9 pr-4 py-2 rounded-xl border border-white/[0.08] bg-slate-950/80 focus:border-neon-cyan focus:outline-none placeholder:text-slate-500"
        />
      </div>

      <!-- Members Grid/List Panel -->
      <div class="cyber-panel rounded-2xl overflow-hidden border border-white/[0.06] shadow-xl" v-loading="loading">
        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse min-w-[650px]">
            <thead>
              <tr class="border-b border-white/[0.08] bg-slate-950/40 text-[10px] font-mono uppercase text-slate-400 tracking-wider">
                <th class="py-3.5 px-4.5">Username</th>
                <th class="py-3.5 px-4.5">Display Name</th>
                <th class="py-3.5 px-4.5">Roles</th>
                <th class="py-3.5 px-4.5">Status</th>
                <th class="py-3.5 px-4.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-white/[0.05] text-xs">
              <tr
                v-for="m in filteredMembers"
                :key="m.id"
                class="hover:bg-white/[0.015] transition-colors group relative"
              >
                <!-- Active border row highlight -->
                <td class="py-4.5 px-4.5 font-mono text-neon-cyan font-semibold">
                  {{ m.username }}
                </td>

                <td class="py-4.5 px-4.5 text-slate-200">
                  {{ m.displayName || m.username }}
                </td>

                <!-- Role Pill badges -->
                <td class="py-4.5 px-4.5">
                  <div class="flex flex-wrap gap-1.5">
                    <span
                      class="px-2 py-0.5 rounded text-[10px] font-mono border"
                      :class="
                        m.role === 'ADMIN'
                          ? 'border-indigo-500/35 text-indigo-400 bg-indigo-950/15'
                          : m.role === 'KNOWLEDGE_ADMIN'
                          ? 'border-cyan-500/30 text-cyan-400 bg-cyan-950/15'
                          : 'border-slate-500/25 text-slate-400 bg-slate-950/20'
                      "
                    >
                      {{ m.role || 'VIEWER' }}
                    </span>
                  </div>
                </td>

                <!-- Active/Inactive status with dynamic bullet animation -->
                <td class="py-4.5 px-4.5">
                  <span class="flex items-center gap-1.5 font-mono">
                    <span
                      class="w-2 h-2 rounded-full"
                      :class="
                        m.status === 'ACTIVE'
                          ? 'bg-emerald-500 shadow-[0_0_8px_#10b981] animate-pulse'
                          : 'bg-red-500 shadow-[0_0_8px_#ef4444]'
                      "
                    ></span>
                    <span :class="m.status === 'ACTIVE' ? 'text-emerald-400' : 'text-red-400'">
                      {{ m.status === 'ACTIVE' ? 'Active' : 'Inactive' }}
                    </span>
                  </span>
                </td>

                <!-- Edit/Remove buttons -->
                <td class="py-4.5 px-4.5 text-right font-mono">
                  <div class="flex items-center justify-end space-x-2">
                    <button
                      class="px-2.5 py-1 rounded border border-white/[0.06] hover:border-cyan-500/40 text-slate-450 hover:text-white transition-all text-[10px] cursor-pointer"
                    >
                      Edit
                    </button>
                    <button
                      class="px-2.5 py-1 rounded border border-red-950/40 hover:border-red-500 text-red-500 hover:text-red-400 hover:bg-red-950/15 transition-all text-[10px] cursor-pointer"
                    >
                      Remove
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="filteredMembers.length === 0">
                <td colspan="5" class="py-8 text-center text-slate-500 font-mono text-xs">
                  No members found matching "{{ searchTerm }}"
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination controls -->
        <div class="bg-slate-950/40 px-6 py-4 border-t border-white/[0.05] flex items-center justify-between text-xs font-mono text-slate-500">
          <span>Showing 1-{{ filteredMembers.length }} of {{ filteredMembers.length }}</span>
          <div class="flex items-center space-x-1.5">
            <button class="px-2.5 py-1 rounded bg-cyan-950/40 text-neon-cyan border border-neon-cyan/30">1</button>
            <button class="px-2.5 py-1.5 rounded text-[10px] hover:text-white transition-all flex items-center gap-0.5 cursor-pointer">
              <span>Next</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Roles & Polices tab visual mockups -->
    <div v-if="activeTab === 'roles'" class="grid grid-cols-1 md:grid-cols-3 gap-6" v-loading="loading">
      <div v-for="role in roles" :key="role.id" class="cyber-panel rounded-2xl p-6 border-white/[0.06] relative">
        <h3 class="text-sm font-mono uppercase font-semibold mb-2" :class="role.isSystem ? 'text-neon-cyan' : 'text-slate-200'">{{ role.name }}</h3>
        <p class="text-slate-350 text-xs leading-relaxed mb-4">
          {{ role.description || 'Custom role with defined policy attachments.' }}
        </p>
        <div class="flex justify-between items-center mt-4">
          <span class="text-[10px] font-mono text-slate-500">{{ role.memberCount || 0 }} members assigned</span>
          <button v-if="!role.isSystem" @click="handleDeleteRole(role.id)" class="text-[10px] font-mono text-red-500 hover:text-red-400 transition-colors cursor-pointer">Delete</button>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'policies'" class="space-y-4" v-loading="loading">
      <div v-for="policy in policies" :key="policy.id" class="cyber-panel rounded-2xl p-6 border-white/[0.06] flex items-center justify-between">
        <div>
          <h3 class="text-xs font-mono uppercase tracking-wider text-white font-bold mb-1">{{ policy.name }}</h3>
          <p class="text-slate-400 text-xs mt-1">Resource: {{ policy.resource }}</p>
          <div class="flex gap-2 mt-2">
            <span v-for="action in policy.actions" :key="action" class="text-[9px] font-mono text-slate-500 border border-slate-700/50 bg-slate-800/30 px-1.5 py-0.5 rounded">
              {{ action }}
            </span>
          </div>
        </div>
        <span class="px-2.5 py-1 rounded bg-emerald-950/45 text-emerald-400 font-mono text-[9px] font-bold border border-emerald-500/20 uppercase tracking-widest">
          {{ policy.isSystem ? 'SYSTEM' : 'CUSTOM' }}
        </span>
      </div>
    </div>

    <!-- Modals -->
    <!-- Add Member Modal -->
    <div v-if="memberModalVisible" class="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center z-50 p-4">
      <div class="bg-slate-900 border border-white/10 rounded-2xl max-w-md w-full p-6 md:p-8 shadow-2xl relative">
        <h2 class="font-display text-xl font-bold text-white mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-cyan-400 text-[20px]">person_add</span>
          <span>Add Member to Virtual Space</span>
        </h2>
        
        <form @submit.prevent="submitMember" class="space-y-4">
          <div>
            <label class="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1">
              Username
            </label>
            <input
              type="text"
              v-model="memberForm.username"
              placeholder="e.g. alice.wonder"
              class="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none font-mono"
              required
            />
          </div>

          <div>
            <label class="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-2">
              Assign Cyber Roles
            </label>
            <select
              v-model="memberForm.roleId"
              class="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none font-mono"
              required
            >
              <option value="" disabled>Select a role...</option>
              <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.name }}</option>
            </select>
          </div>

          <div class="flex items-center justify-end space-x-3 pt-4">
            <button
              type="button"
              @click="memberModalVisible = false"
              class="px-4 py-2 rounded-xl border border-white/[0.08] text-xs font-mono text-slate-400 hover:text-white tracking-wider cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="px-4 py-2 rounded-xl cyber-btn-cyan text-xs font-mono tracking-wider cursor-pointer disabled:opacity-50"
            >
              Authorize Unit
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Add Role Modal -->
    <div v-if="roleModalVisible" class="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center z-50 p-4">
      <div class="bg-slate-900 border border-white/10 rounded-2xl max-w-md w-full p-6 md:p-8 shadow-2xl relative">
        <h2 class="font-display text-xl font-bold text-white mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-cyan-400 text-[20px]">badge</span>
          <span>Create Custom Role</span>
        </h2>
        
        <form @submit.prevent="submitRole" class="space-y-4">
          <div>
            <label class="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1">
              Role Name
            </label>
            <input
              type="text"
              v-model="roleForm.name"
              placeholder="e.g. External Auditor"
              class="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none font-mono"
              required
            />
          </div>

          <div>
            <label class="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1">
              Role Description
            </label>
            <textarea
              v-model="roleForm.description"
              placeholder="Briefly describe the purpose of this role"
              rows="3"
              class="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none font-mono"
            ></textarea>
          </div>

          <div class="flex items-center justify-end space-x-3 pt-4">
            <button
              type="button"
              @click="roleModalVisible = false"
              class="px-4 py-2 rounded-xl border border-white/[0.08] text-xs font-mono text-slate-400 hover:text-white tracking-wider cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="px-4 py-2 rounded-xl cyber-btn-cyan text-xs font-mono tracking-wider cursor-pointer disabled:opacity-50"
            >
              Confirm Creation
            </button>
          </div>
        </form>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref, computed, onMounted } from 'vue'
import { getRoles, getMembers, getPolicies, createRole, addMember, deleteRole, type Role, type Member, type Policy } from '@/api/permissions'

defineOptions({
  name: 'PermissionsView',
})

const activeTab = ref('members')
const searchTerm = ref('')
const loading = ref(false)
const submitting = ref(false)

const members = ref<Member[]>([])
const roles = ref<Role[]>([])
const policies = ref<Policy[]>([])

const currentTabName = computed(() => {
  if (activeTab.value === 'members') return 'Member'
  if (activeTab.value === 'roles') return 'Role'
  return 'Policy'
})

const filteredMembers = computed(() => {
  if (!searchTerm.value) return members.value
  const term = searchTerm.value.toLowerCase()
  return members.value.filter(m => 
    m.username.toLowerCase().includes(term) || 
    (m.displayName && m.displayName.toLowerCase().includes(term))
  )
})

// Modals
const roleModalVisible = ref(false)
const roleForm = ref({ name: '', description: '' })

const memberModalVisible = ref(false)
const memberForm = ref({ username: '', roleId: '' })

const loadData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'members') {
      members.value = await getMembers()
    } else if (activeTab.value === 'roles') {
      roles.value = await getRoles()
    } else if (activeTab.value === 'policies') {
      policies.value = await getPolicies()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadData()
}

// Global initialization
onMounted(async () => {
  // Preload roles so member modal has options
  roles.value = await getRoles()
  loadData()
})

const handleAdd = () => {
  if (activeTab.value === 'members') {
    memberForm.value = { username: '', roleId: '' }
    memberModalVisible.value = true
  } else if (activeTab.value === 'roles') {
    roleForm.value = { name: '', description: '' }
    roleModalVisible.value = true
  } else {
    ElMessage.info('暂不支持通过界面直接新增策略')
  }
}

const submitRole = async () => {
  if (!roleForm.value.name) return
  submitting.value = true
  try {
    await createRole(roleForm.value)
    ElMessage.success('角色创建成功')
    roleModalVisible.value = false
    refreshData()
  } catch (err: any) {
    ElMessage.error(err.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

const submitMember = async () => {
  if (!memberForm.value.username || !memberForm.value.roleId) return
  submitting.value = true
  try {
    await addMember(memberForm.value)
    ElMessage.success('成员添加成功')
    memberModalVisible.value = false
    refreshData()
  } catch (err: any) {
    ElMessage.error(err.message || '添加失败，请确认用户存在')
  } finally {
    submitting.value = false
  }
}

const handleDeleteRole = async (roleId: string) => {
  try {
    await ElMessageBox.confirm('确定要删除此角色吗？', '警告', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
    
    await deleteRole(roleId)
    ElMessage.success('删除成功')
    refreshData()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}
</script>
