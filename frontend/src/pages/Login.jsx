import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Shield, Lock, Mail, ArrowRight, Sparkles, KeyRound } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Button from '../components/ui/Button';
import { toast } from 'sonner';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e?.preventDefault();
    if (!email || !password) {
      toast.error('Please enter email and password');
      return;
    }

    setLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
      toast.success('Authentication successful! Welcome to KeyBridge');
    } catch (err) {
      const msg = err.response?.data?.message || 'Invalid credentials. Please verify your email & password.';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const fillCredentials = (userEmail, userPass) => {
    setEmail(userEmail);
    setPassword(userPass);
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center p-4 relative overflow-hidden bg-slate-950">
      {/* Background radial ambient lights */}
      <div className="absolute top-1/4 left-1/3 w-[500px] h-[500px] bg-indigo-600/20 rounded-full blur-[140px] pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/3 w-[500px] h-[500px] bg-violet-600/15 rounded-full blur-[140px] pointer-events-none" />

      <motion.div
        initial={{ opacity: 0, y: 20, scale: 0.95 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.5, ease: [0.23, 1, 0.32, 1] }}
        className="w-full max-w-md glass p-8 relative z-10 border-slate-800 shadow-2xl"
      >
        {/* Header Icon */}
        <div className="flex flex-col items-center text-center mb-8">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center shadow-xl shadow-indigo-500/30 mb-4 border border-indigo-400/30">
            <Shield className="w-8 h-8 text-white" />
          </div>
          <h2 className="text-2xl font-bold text-slate-100 tracking-tight">
            Key<span className="text-indigo-400">Bridge</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            PKI, Cryptographic Tokens & Digital Signature Platform
          </p>
        </div>

        {/* Login Form */}
        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
              Email Address
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@keybridge.com"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-900/80 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-indigo-500/80 focus:ring-1 focus:ring-indigo-500/80 transition-colors"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
              Password
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-900/80 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-indigo-500/80 focus:ring-1 focus:ring-indigo-500/80 transition-colors"
              />
            </div>
          </div>

          <Button
            type="submit"
            variant="primary"
            size="lg"
            loading={loading}
            icon={ArrowRight}
            iconPosition="right"
            className="w-full mt-2"
          >
            Authenticate & Sign In
          </Button>
        </form>

        {/* Demo Credentials Quick-Fill Chips */}
        <div className="mt-8 pt-6 border-t border-slate-800/80">
          <div className="flex items-center gap-1.5 text-xs text-slate-400 font-medium mb-3">
            <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            <span>Select Demo Account:</span>
          </div>

          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => fillCredentials('admin@keybridge.com', 'Admin@123')}
              className="px-2.5 py-2 rounded-xl bg-slate-900/60 hover:bg-indigo-600/20 border border-slate-800 hover:border-indigo-500/40 text-left transition-colors group cursor-pointer"
            >
              <div className="text-[11px] font-bold text-indigo-300 group-hover:text-indigo-200">Admin</div>
              <div className="text-[9px] text-slate-400">Full Access</div>
            </button>

            <button
              type="button"
              onClick={() => fillCredentials('user@keybridge.com', 'User@123')}
              className="px-2.5 py-2 rounded-xl bg-slate-900/60 hover:bg-indigo-600/20 border border-slate-800 hover:border-indigo-500/40 text-left transition-colors group cursor-pointer"
            >
              <div className="text-[11px] font-bold text-emerald-300 group-hover:text-emerald-200">User</div>
              <div className="text-[9px] text-slate-400">Sign & Verify</div>
            </button>

            <button
              type="button"
              onClick={() => fillCredentials('auditor@keybridge.com', 'Audit@123')}
              className="px-2.5 py-2 rounded-xl bg-slate-900/60 hover:bg-indigo-600/20 border border-slate-800 hover:border-indigo-500/40 text-left transition-colors group cursor-pointer"
            >
              <div className="text-[11px] font-bold text-amber-300 group-hover:text-amber-200">Auditor</div>
              <div className="text-[9px] text-slate-400">Read & Logs</div>
            </button>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
