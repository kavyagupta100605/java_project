import React from 'react';
import { motion } from 'framer-motion';
import { Hash, KeyRound, PenTool, Database, CheckCircle2, Loader2 } from 'lucide-react';
import clsx from 'clsx';

const STEPS = [
  { id: 1, label: 'Calculating SHA-256 Hash', icon: Hash, desc: 'Generating cryptographic digest of document bytes' },
  { id: 2, label: 'Accessing PKCS#12 Keystore', icon: KeyRound, desc: 'Verifying PIN & unlocking RSA private key' },
  { id: 3, label: 'RSA Digital Signature', icon: PenTool, desc: 'Encrypting hash with RSA-2048 private key' },
  { id: 4, label: 'Audit Trail & Persistence', icon: Database, desc: 'Recording signed document & immutable audit log' },
];

export default function SigningProgress({ activeStep = 1, docHash = '', isComplete = false }) {
  return (
    <div className="glass p-6 w-full max-w-xl mx-auto space-y-6">
      <div className="flex items-center justify-between border-b border-slate-800 pb-4">
        <div>
          <h4 className="text-base font-semibold text-slate-100">Cryptographic Signing Pipeline</h4>
          <p className="text-xs text-slate-400">Strict zero-key exposure RSA signing execution</p>
        </div>
        {isComplete ? (
          <span className="flex items-center gap-1.5 text-xs font-semibold text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
            <CheckCircle2 className="w-4 h-4" /> Signature Complete
          </span>
        ) : (
          <span className="flex items-center gap-1.5 text-xs font-semibold text-indigo-400 bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20 animate-pulse">
            <Loader2 className="w-3.5 h-3.5 animate-spin" /> Processing
          </span>
        )}
      </div>

      {/* Steps List */}
      <div className="space-y-4">
        {STEPS.map((step) => {
          const Icon = step.icon;
          const isDone = isComplete || activeStep > step.id;
          const isCurrent = !isComplete && activeStep === step.id;

          return (
            <motion.div
              key={step.id}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: step.id * 0.1 }}
              className={clsx(
                'flex items-start gap-4 p-3.5 rounded-xl border transition-all duration-300',
                isDone && 'bg-emerald-500/5 border-emerald-500/30',
                isCurrent && 'bg-indigo-500/10 border-indigo-500/50 shadow-lg shadow-indigo-500/10',
                !isDone && !isCurrent && 'bg-slate-900/40 border-slate-800/60 opacity-60'
              )}
            >
              <div
                className={clsx(
                  'w-9 h-9 rounded-xl flex items-center justify-center shrink-0 border transition-colors',
                  isDone && 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40',
                  isCurrent && 'bg-indigo-500/20 text-indigo-400 border-indigo-500/40 animate-pulse',
                  !isDone && !isCurrent && 'bg-slate-800 text-slate-500 border-slate-700'
                )}
              >
                {isDone ? (
                  <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                ) : isCurrent ? (
                  <Loader2 className="w-5 h-5 animate-spin text-indigo-400" />
                ) : (
                  <Icon className="w-4 h-4" />
                )}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <h5
                    className={clsx(
                      'text-sm font-medium',
                      isDone ? 'text-emerald-300' : isCurrent ? 'text-indigo-300' : 'text-slate-400'
                    )}
                  >
                    {step.label}
                  </h5>
                  <span className="text-[10px] font-mono uppercase text-slate-500">Step {step.id}/4</span>
                </div>
                <p className="text-xs text-slate-400 mt-0.5">{step.desc}</p>
              </div>
            </motion.div>
          );
        })}
      </div>

      {/* Hash preview box */}
      {docHash && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          className="bg-slate-950/80 p-3.5 rounded-xl border border-slate-800 space-y-1"
        >
          <div className="flex items-center justify-between text-[11px] text-slate-400">
            <span className="uppercase font-semibold tracking-wider text-slate-500">Document Digest</span>
            <span className="font-mono text-indigo-400">SHA-256</span>
          </div>
          <p className="font-mono text-xs text-indigo-200 break-all select-all">{docHash}</p>
        </motion.div>
      )}
    </div>
  );
}
