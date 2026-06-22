<template>
  <div class="relative min-h-screen w-full flex items-center justify-center overflow-hidden font-sans select-none bg-[#02040a]">
    <!-- Background Live Interactive Canvas -->
    <canvas ref="canvasRef" class="absolute inset-0 block w-full h-full z-0"></canvas>

    <!-- Cyberpunk grid subtle ambient mask -->
    <div class="absolute inset-0 bg-gradient-to-t from-black via-transparent to-black/80 pointer-events-none z-10"></div>

    <!-- Dynamic Cyber Orbs -->
    <div class="absolute top-[25%] left-[10%] w-96 h-96 rounded-full bg-cyan-500/10 blur-[120px] pointer-events-none animate-pulse-slow z-10"></div>
    <div class="absolute bottom-[20%] right-[10%] w-96 h-96 rounded-full bg-purple-600/10 blur-[120px] pointer-events-none animate-pulse-slow z-10"></div>

    <!-- Central Login Shield Panel Container -->
    <div class="relative w-full max-w-lg mx-4 z-20" id="login-container">
      <!-- Layered cyber offsets for depth -->
      <div class="absolute -inset-1.5 bg-gradient-to-r from-neon-cyan/20 to-neon-purple/20 rounded-2xl blur-lg opacity-75 pointer-events-none"></div>
      
      <!-- Main Glass Panel -->
      <div class="relative cyber-panel cyber-glow-cyan rounded-2xl p-8 md:p-10 border border-white/[0.06] overflow-hidden">
        <!-- Moving scanline -->
        <div class="absolute inset-0 pointer-events-none bg-gradient-to-b from-transparent via-cyan-500/[0.02] to-transparent h-1/2 w-full animate-pulse" style="animation-duration: 6s;"></div>

        <!-- Card Top Branding Header -->
        <div class="flex flex-col items-center text-center pb-6 border-b border-white/[0.08] mb-8">
          <div class="flex items-center space-x-2 bg-gradient-to-r from-cyan-500/10 to-purple-500/10 px-4 py-2 rounded-full border border-white/[0.05] mb-4">
            <span class="material-symbols-outlined w-5 h-5 text-neon-cyan animate-pulse">memory</span>
            <span class="font-display font-medium text-white tracking-widest text-sm uppercase">Enterprise KB</span>
          </div>

          <h1 class="font-display text-3xl md:text-4xl font-extrabold tracking-tight bg-gradient-to-b from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
            Enterprise Knowledge Base Agent
          </h1>
          <p class="mt-2 text-xs md:text-sm text-slate-400 font-mono uppercase tracking-wider">
            MVP Console
          </p>
        </div>

        <!-- Form -->
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          @submit.prevent="submit"
          class="space-y-6"
        >
          <!-- Username -->
          <div class="space-y-2">
            <label class="block text-xs font-mono uppercase tracking-widest text-slate-400">
              Username
            </label>
            <div class="relative">
              <input
                type="text"
                v-model="form.username"
                placeholder="Enter your username"
                class="w-full text-sm text-white px-4 py-3 rounded-xl cyber-input pl-11 placeholder-slate-600"
                required
              />
              <div class="absolute left-4 top-1/2 -translate-y-1/2">
                <span class="block w-2.5 h-2.5 rounded-full border border-neon-cyan bg-cyan-950/60"></span>
              </div>
            </div>
          </div>

          <!-- Password -->
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <label class="block text-xs font-mono uppercase tracking-widest text-slate-400">
                Password
              </label>
              <a href="#" @click.prevent="ElMessage.warning('Password retrieval protocol is gated. Please contact system admin.')" class="text-xs text-neon-cyan/80 hover:text-neon-cyan font-mono transition-colors">
                Forgot Password?
              </a>
            </div>
            <div class="relative">
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="form.password"
                placeholder="Enter your password"
                class="w-full text-sm text-white px-4 py-3 rounded-xl cyber-input pl-11 pr-11 placeholder-slate-600"
                @keyup.enter="submit"
                required
              />
              <div class="absolute left-4 top-1/2 -translate-y-1/2">
                <span class="block w-2.5 h-2.5 rounded-full border border-neon-purple bg-purple-950/60"></span>
              </div>
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white transition-colors cursor-pointer"
              >
                <span class="material-symbols-outlined text-[16px]">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>

          <!-- Submit Button -->
          <button
            type="button"
            :disabled="submitting"
            @click="submit"
            class="w-full relative py-3.5 px-6 rounded-xl cyber-btn-cyan font-display font-bold tracking-wider text-sm transition-all flex items-center justify-center space-x-2 border border-cyan-400/20 active:scale-95 disabled:opacity-50 cursor-pointer"
          >
            <template v-if="submitting">
              <div class="w-4 h-4 rounded-full border-2 border-slate-900 border-t-transparent animate-spin"></div>
              <span>AUTHORIZING UNIT...</span>
            </template>
            <template v-else>
              <span>Login</span>
            </template>
          </button>
        </el-form>
      </div>
    </div>

    <!-- Persistent System Status Bar bottom -->
    <div class="absolute bottom-6 left-1/2 -translate-x-1/2 z-20 flex items-center space-x-2.5 bg-black/40 backdrop-blur-md border border-white/[0.05] px-4 py-1.5 rounded-full shadow-lg">
      <span class="relative flex h-2 w-2">
        <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
        <span class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
      </span>
      <span class="text-[10px] font-mono uppercase tracking-widest text-slate-400">
        System Online
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

defineOptions({
  name: 'LoginView',
})

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const canvasRef = ref<HTMLCanvasElement | null>(null)
const submitting = ref(false)
const showPassword = ref(false)

const form = reactive({
  username: 'admin',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

let animationFrameId: number

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  let width = (canvas.width = window.innerWidth)
  let height = (canvas.height = window.innerHeight)

  const handleResize = () => {
    if (canvas) {
      width = canvas.width = window.innerWidth
      height = canvas.height = window.innerHeight
    }
  }
  window.addEventListener('resize', handleResize)

  // Create particles
  const particleCount = 75
  const particles: Array<{
    x: number
    y: number
    vx: number
    vy: number
    radius: number
    color: string
  }> = []

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
      radius: Math.random() * 2 + 1,
      color: Math.random() > 0.4 ? 'rgba(0, 240, 255, 0.4)' : 'rgba(189, 0, 255, 0.4)',
    })
  }

  const draw = () => {
    ctx.fillStyle = '#02040a'
    ctx.fillRect(0, 0, width, height)

    // Draw cyber net/grid overlay
    ctx.strokeStyle = 'rgba(0, 240, 255, 0.02)'
    ctx.lineWidth = 1
    const gridSize = 50
    for (let x = 0; x < width; x += gridSize) {
      ctx.beginPath()
      ctx.moveTo(x, 0)
      ctx.lineTo(x, height)
      ctx.stroke()
    }
    for (let y = 0; y < height; y += gridSize) {
      ctx.beginPath()
      ctx.moveTo(0, y)
      ctx.lineTo(width, y)
      ctx.stroke()
    }

    // Draw waves
    ctx.strokeStyle = 'rgba(189, 0, 255, 0.08)'
    ctx.beginPath()
    const waveOffset = Date.now() * 0.0005
    for (let x = 0; x < width; x += 10) {
      const y = height * 0.85 + Math.sin(x * 0.005 + waveOffset) * 20 + Math.cos(x * 0.002 - waveOffset) * 15
      if (x === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    }
    ctx.stroke()

    ctx.strokeStyle = 'rgba(0, 240, 255, 0.08)'
    ctx.beginPath()
    for (let x = 0; x < width; x += 10) {
      const y = height * 0.88 + Math.cos(x * 0.004 + waveOffset * 0.8) * 15 + Math.sin(x * 0.003 + waveOffset) * 10
      if (x === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    }
    ctx.stroke()

    // Update & draw particles
    particles.forEach((p, index) => {
      p.x += p.vx
      p.y += p.vy

      if (p.x < 0 || p.x > width) p.vx *= -1
      if (p.y < 0 || p.y > height) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = p.color
      ctx.shadowBlur = 4
      ctx.shadowColor = p.color
      ctx.fill()
      ctx.shadowBlur = 0

      // Draw connections
      for (let j = index + 1; j < particles.length; j++) {
        const p2 = particles[j]
        const dist = Math.hypot(p.x - p2.x, p.y - p2.y)
        if (dist < 120) {
          const alpha = (1 - dist / 120) * 0.12
          ctx.strokeStyle = p.color.includes('255, 0')
            ? `rgba(189, 0, 255, ${alpha})`
            : `rgba(0, 240, 255, ${alpha})`
          ctx.lineWidth = 0.5
          ctx.beginPath()
          ctx.moveTo(p.x, p.y)
          ctx.lineTo(p2.x, p2.y)
          ctx.stroke()
        }
      }
    })

    animationFrameId = requestAnimationFrame(draw)
  }

  draw()

  onUnmounted(() => {
    cancelAnimationFrame(animationFrameId)
    window.removeEventListener('resize', handleResize)
  })
})

async function submit() {
  if (!formRef.value) return
  if (!form.username.trim()) {
    ElMessage.error('Please enter a username')
    return
  }
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
