import React, { useState, useEffect, useRef } from 'react';
import { Eye, EyeOff, Shield, Database, Cpu, HelpCircle } from 'lucide-react';

interface LoginViewProps {
  onLoginSuccess: (username: string) => void;
}

export default function LoginView({ onLoginSuccess }: LoginViewProps) {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('••••••••');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Dynamic cyber space particle connection simulation (similar to neurons and data flow in the screenshot!)
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;
    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    const handleResize = () => {
      if (canvas) {
        width = canvas.width = window.innerWidth;
        height = canvas.height = window.innerHeight;
      }
    };
    window.addEventListener('resize', handleResize);

    // Create particles
    const particleCount = 75;
    const particles: Array<{
      x: number;
      y: number;
      vx: number;
      vy: number;
      radius: number;
      color: string;
    }> = [];

    for (let i = 0; i < particleCount; i++) {
      particles.push({
        x: Math.random() * width,
        y: Math.random() * height,
        vx: (Math.random() - 0.5) * 0.4,
        vy: (Math.random() - 0.5) * 0.4,
        radius: Math.random() * 2 + 1,
        color: Math.random() > 0.4 ? 'rgba(0, 240, 255, 0.4)' : 'rgba(189, 0, 255, 0.4)',
      });
    }

    const draw = () => {
      ctx.fillStyle = '#02040a';
      ctx.fillRect(0, 0, width, height);

      // Draw cyber net/grid overlay
      ctx.strokeStyle = 'rgba(0, 240, 255, 0.02)';
      ctx.lineWidth = 1;
      const gridSize = 50;
      for (let x = 0; x < width; x += gridSize) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, height);
        ctx.stroke();
      }
      for (let y = 0; y < height; y += gridSize) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(width, y);
        ctx.stroke();
      }

      // Draw waves (similar to the quantum floor wireframe)
      ctx.strokeStyle = 'rgba(189, 0, 255, 0.08)';
      ctx.beginPath();
      const waveOffset = Date.now() * 0.0005;
      for (let x = 0; x < width; x += 10) {
        const y = height * 0.85 + Math.sin(x * 0.005 + waveOffset) * 20 + Math.cos(x * 0.002 - waveOffset) * 15;
        if (x === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();

      ctx.strokeStyle = 'rgba(0, 240, 255, 0.08)';
      ctx.beginPath();
      for (let x = 0; x < width; x += 10) {
        const y = height * 0.88 + Math.cos(x * 0.004 + waveOffset * 0.8) * 15 + Math.sin(x * 0.003 + waveOffset) * 10;
        if (x === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();

      // Update & draw particles
      particles.forEach((p, index) => {
        p.x += p.vx;
        p.y += p.vy;

        if (p.x < 0 || p.x > width) p.vx *= -1;
        if (p.y < 0 || p.y > height) p.vy *= -1;

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx.fillStyle = p.color;
        ctx.shadowBlur = 4;
        ctx.shadowColor = p.color;
        ctx.fill();
        ctx.shadowBlur = 0; // reset

        // Draw connections
        for (let j = index + 1; j < particles.length; j++) {
          const p2 = particles[j];
          const dist = Math.hypot(p.x - p2.x, p.y - p2.y);
          if (dist < 120) {
            const alpha = (1 - dist / 120) * 0.12;
            ctx.strokeStyle = p.color.includes('255, 0')
              ? `rgba(189, 0, 255, ${alpha})`
              : `rgba(0, 240, 255, ${alpha})`;
            ctx.lineWidth = 0.5;
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p2.x, p2.y);
            ctx.stroke();
          }
        }
      });

      animationFrameId = requestAnimationFrame(draw);
    };

    draw();

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim()) {
      setError('Please enter a username');
      return;
    }
    setIsSubmitting(true);
    setError('');

    // Fast simulated check
    setTimeout(() => {
      setIsSubmitting(false);
      onLoginSuccess(username);
    }, 900);
  };

  return (
    <div className="relative min-h-screen w-full flex items-center justify-center overflow-hidden font-sans select-none">
      {/* Background Live Interactive Canvas */}
      <canvas ref={canvasRef} className="absolute inset-0 block w-full h-full z-0" />

      {/* Cyberpunk grid subtle ambient mask */}
      <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-black/80 pointer-events-none z-10" />

      {/* Dynamic Cyber Orbs */}
      <div className="absolute top-[25%] left-[10%] w-96 h-96 rounded-full bg-cyan-500/10 blur-[120px] pointer-events-none animate-pulse-slow z-10" />
      <div className="absolute bottom-[20%] right-[10%] w-96 h-96 rounded-full bg-purple-600/10 blur-[120px] pointer-events-none animate-pulse-slow z-10" />

      {/* Central Login Shield Panel Container */}
      <div className="relative w-full max-w-lg mx-4 z-20" id="login-container">
        {/* Layered cyber offsets for depth */}
        <div className="absolute -inset-1.5 bg-gradient-to-r from-neon-cyan/20 to-neon-purple/20 rounded-2xl blur-lg opacity-75 pointer-events-none" />
        
        {/* Main Glass Panel */}
        <div className="relative cyber-panel cyber-glow-cyan rounded-2xl p-8 md:p-10 border border-white/[0.06] overflow-hidden">
          {/* Moving scanline */}
          <div className="absolute inset-0 pointer-events-none bg-gradient-to-b from-transparent via-cyan-500/[0.02] to-transparent h-1/2 w-full animate-pulse" style={{ animationDuration: '6s' }} />

          {/* Card Top Branding Header */}
          <div className="flex flex-col items-center text-center pb-6 border-b border-white/[0.08] mb-8">
            <div className="flex items-center space-x-2 bg-gradient-to-r from-cyan-500/10 to-purple-500/10 px-4 py-2 rounded-full border border-white/[0.05] mb-4">
              <Cpu className="w-5 h-5 text-neon-cyan animate-pulse" />
              <span className="font-display font-medium text-white tracking-widest text-sm uppercase">Enterprise KB</span>
            </div>

            <h1 className="font-display text-3xl md:text-4xl font-extrabold tracking-tight bg-gradient-to-b from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
              Enterprise Knowledge Base Agent
            </h1>
            <p className="mt-2 text-xs md:text-sm text-slate-400 font-mono uppercase tracking-wider">
              MVP Console
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleLogin} className="space-y-6">
            {error && (
              <div className="p-3 bg-red-950/40 border border-red-500/30 rounded-lg text-red-400 text-xs font-mono flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-red-500 animate-ping" />
                <span>[ERROR]: {error}</span>
              </div>
            )}

            {/* Username */}
            <div className="space-y-2">
              <label className="block text-xs font-mono uppercase tracking-widest text-slate-400">
                Username
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Enter your username"
                  className="w-full text-sm text-white px-4 py-3 rounded-xl cyber-input pl-11 placeholder-slate-600"
                  required
                />
                <div className="absolute left-4 top-1/2 -translate-y-1/2">
                  <span className="block w-2.5 h-2.5 rounded-full border border-neon-cyan bg-cyan-950/60" />
                </div>
              </div>
            </div>

            {/* Password */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-mono uppercase tracking-widest text-slate-400">
                  Password
                </label>
                <a
                  href="#forgot"
                  onClick={(e) => {
                    e.preventDefault();
                    setError('Password retrieval protocol is gated. Please contact system admin.');
                  }}
                  className="text-xs text-neon-cyan/80 hover:text-neon-cyan font-mono transition-colors"
                >
                  Forgot Password?
                </a>
              </div>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  className="w-full text-sm text-white px-4 py-3 rounded-xl cyber-input pl-11 pr-11 placeholder-slate-650"
                  required
                />
                <div className="absolute left-4 top-1/2 -translate-y-1/2">
                  <span className="block w-2.5 h-2.5 rounded-full border border-neon-purple bg-purple-950/60" />
                </div>
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white transition-colors"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full relative py-3.5 px-6 rounded-xl cyber-btn-cyan font-display font-bold tracking-wider text-sm transition-all flex items-center justify-center space-x-2 border border-cyan-400/20 active:scale-95 disabled:opacity-50 cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 rounded-full border-2 border-slate-900 border-t-transparent animate-spin" />
                  <span>AUTHORIZING UNIT...</span>
                </>
              ) : (
                <span>Login</span>
              )}
            </button>
          </form>
        </div>
      </div>

      {/* Persistent System Status Bar bottom - Image 1 */}
      <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-20 flex items-center space-x-2.5 bg-black/40 backdrop-blur-md border border-white/[0.05] px-4 py-1.5 rounded-full shadow-lg">
        <span className="relative flex h-2 w-2">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
        </span>
        <span className="text-[10px] font-mono uppercase tracking-widest text-slate-400">
          System Online
        </span>
      </div>
    </div>
  );
}
