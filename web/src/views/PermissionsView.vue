<template>
  <section class="page-section max-w-[1400px] mx-auto py-8 px-6">
    <div class="flex items-center justify-between mb-8">
      <div>
        <h2 class="text-2xl font-bold text-slate-900 tracking-tight m-0">权限管理</h2>
        <p class="text-slate-500 mt-1.5 font-medium m-0 flex items-center gap-2">
          <span class="material-symbols-outlined text-[16px]">security</span>
          管理空间成员、角色权限和访问策略
        </p>
      </div>
      <div class="flex items-center gap-3">
        <el-button :icon="Refresh" @click="refreshData" class="!rounded-lg font-medium shadow-sm hover:text-blue-600">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd" class="!rounded-lg font-medium shadow-md shadow-blue-500/20 bg-blue-700 hover:bg-blue-600 border-none">
          新增{{ currentTabName }}
        </el-button>
      </div>
    </div>

    <div class="stitch-card bg-white/80 backdrop-blur-sm !p-0 overflow-hidden">
      <el-tabs v-model="activeTab" class="permissions-tabs px-6 pt-4" @tab-change="refreshData">
        <!-- 空间成员 -->
        <el-tab-pane label="空间成员" name="members">
          <el-table v-loading="loading" :data="members" empty-text="暂无成员" class="w-full">
            <el-table-column prop="username" label="用户名" min-width="150" />
            <el-table-column prop="displayName" label="显示名称" min-width="150" />
            <el-table-column prop="role" label="分配角色" min-width="150">
              <template #default="{ row }">
                <span class="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                  {{ row.role }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold"
                      :class="row.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-100 text-slate-600 border border-slate-200'">
                  <span class="w-1.5 h-1.5 rounded-full" :class="row.status === 'ACTIVE' ? 'bg-emerald-500' : 'bg-slate-400'"></span>
                  {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" align="right">
              <template #default="{ row }">
                <el-button link type="primary" class="!font-semibold hover:text-blue-800">编辑角色</el-button>
                <el-button link type="danger" class="!font-semibold">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 角色 -->
        <el-tab-pane label="角色" name="roles">
          <el-table v-loading="loading" :data="roles" empty-text="暂无角色" class="w-full">
            <el-table-column prop="name" label="角色名称" width="200">
              <template #default="{ row }">
                <div class="flex items-center gap-2 font-medium text-slate-800">
                  <span class="material-symbols-outlined text-[18px]" :class="row.isSystem ? 'text-amber-500' : 'text-blue-500'">
                    {{ row.isSystem ? 'shield' : 'badge' }}
                  </span>
                  {{ row.name }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="角色描述" min-width="250" show-overflow-tooltip />
            <el-table-column prop="memberCount" label="关联成员数" width="120" />
            <el-table-column prop="policyCount" label="关联策略数" width="120" />
            <el-table-column label="操作" width="220" fixed="right" align="right">
              <template #default="{ row }">
                <el-button link type="primary" class="!font-semibold hover:text-blue-800">编辑</el-button>
                <el-button link type="primary" class="!font-semibold hover:text-blue-800">分配策略</el-button>
                <el-button link type="danger" class="!font-semibold" :disabled="row.isSystem" @click="handleDeleteRole(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 策略列表 -->
        <el-tab-pane label="策略列表" name="policies">
          <el-table v-loading="loading" :data="policies" empty-text="暂无策略" class="w-full">
            <el-table-column prop="name" label="策略名称" width="220">
              <template #default="{ row }">
                <span class="font-medium text-slate-800">{{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="resource" label="资源范围" width="220">
              <template #default="{ row }">
                <code class="px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 text-[13px] border border-slate-200">
                  {{ row.resource }}
                </code>
              </template>
            </el-table-column>
            <el-table-column prop="actions" label="允许操作" min-width="300">
              <template #default="{ row }">
                <div class="flex flex-wrap gap-2">
                  <span v-for="action in row.actions" :key="action" 
                        class="px-2 py-0.5 rounded-md bg-slate-50 border border-slate-200 text-slate-600 text-xs font-mono">
                    {{ action }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right" align="right">
              <template #default="{ row }">
                <el-button link type="primary" class="!font-semibold hover:text-blue-800">编辑</el-button>
                <el-button link type="danger" class="!font-semibold" :disabled="row.isSystem">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Modals -->
    <!-- Add Role Modal -->
    <el-dialog v-model="roleModalVisible" title="新增角色" width="500px" class="!rounded-xl" destroy-on-close>
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="80px" class="mt-4">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称（例如：外部审核员）" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="简要描述角色的用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roleModalVisible = false" class="!rounded-lg">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitRole" class="!rounded-lg bg-blue-700 border-none">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Add Member Modal -->
    <el-dialog v-model="memberModalVisible" title="添加空间成员" width="500px" class="!rounded-xl" destroy-on-close>
      <el-form ref="memberFormRef" :model="memberForm" :rules="memberRules" label-width="80px" class="mt-4">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="memberForm.username" placeholder="请输入系统中已有的用户名" />
        </el-form-item>
        <el-form-item label="分配角色" prop="roleId">
          <el-select v-model="memberForm.roleId" placeholder="请选择角色" class="w-full">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="memberModalVisible = false" class="!rounded-lg">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitMember" class="!rounded-lg bg-blue-700 border-none">确定</el-button>
        </div>
      </template>
    </el-dialog>

  </section>
</template>

<script setup lang="ts">
import { Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref, computed, onMounted } from 'vue'
import { getRoles, getMembers, getPolicies, createRole, addMember, deleteRole, type Role, type Member, type Policy } from '@/api/permissions'

defineOptions({
  name: 'PermissionsView',
})

const activeTab = ref('members')
const loading = ref(false)
const submitting = ref(false)

const members = ref<Member[]>([])
const roles = ref<Role[]>([])
const policies = ref<Policy[]>([])

const currentTabName = computed(() => {
  if (activeTab.value === 'members') return '成员'
  if (activeTab.value === 'roles') return '角色'
  return '策略'
})

// Modals
const roleModalVisible = ref(false)
const roleFormRef = ref()
const roleForm = ref({ name: '', description: '' })
const roleRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const memberModalVisible = ref(false)
const memberFormRef = ref()
const memberForm = ref({ username: '', roleId: '' })
const memberRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

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
  if (!roleFormRef.value) return
  await roleFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
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
  })
}

const submitMember = async () => {
  if (!memberFormRef.value) return
  await memberFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
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
  })
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

<style scoped>
/* Scoped styles can be minimal since we use Tailwind */
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #e2e8f0;
}
:deep(.el-tabs__item) {
  font-weight: 600;
  color: #64748b;
}
:deep(.el-tabs__item.is-active) {
  color: #1d4ed8;
}
:deep(.el-tabs__active-bar) {
  background-color: #1d4ed8;
  height: 3px;
  border-radius: 3px 3px 0 0;
}
</style>
