import React from 'react';
import { motion } from 'framer-motion';
import { CheckCircle2, XCircle, Clock, AlertCircle, ShieldCheck, GitBranch, Ban, Calendar, Fingerprint } from 'lucide-react';
import clsx from 'clsx';

const DEFAULT_STAGES = [
  { key: 'parsing', name: '1. Certificate Parsing', desc: 'Validates X.509 RFC 5280 DER/PEM structure', icon: Fingerprint },
  { key: 'chain', name: '2. Root CA Chain of Trust', desc: 'Verifies certificate signature against KeyBridge Root CA', icon: GitBranch },
  { key: 'revocation', name: '3. Revocation Check', desc: 'Queries CRL database for SHA-256 fingerprint status', icon: Ban },
  { key: 'validity', name: '4. Temporal Validity', desc: 'Ensures current timestamp is between notBefore and notAfter', icon: Calendar },
  { key: 'signature', name: '5. Cryptographic Match', desc: 'Recalculates SHA-256 and verifies RSA signature with public key', icon: ShieldCheck },
];

export default function PipelineSteps({ steps = [], activeIndex = -1 }) {
  return (
    <div className="w-full space-y-3">
      {DEFAULT_STAGES.map((stage, idx) => {
        const stepResult = steps.find((s) => s.stepNumber === idx + 1 || s.name?.toLowerCase().includes(stage.key));
        const status = stepResult ? (stepResult.passed ? 'pass' : 'fail') : (idx === activeIndex ? 'running' : 'pending');
        const Icon = stage.icon;

        return (
          <motion.div
            key={stage.key}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.08 }}
            className={clsx(
              'flex items-center justify-between p-3.5 rounded-xl border transition-all duration-300',
              status === 'pass' && 'bg-emerald-950/20 border-emerald-500/40 text-emerald-200',
              status === 'fail' && 'bg-rose-950/20 border-rose-500/40 text-rose-200',
              status === 'running' && 'bg-indigo-950/30 border-indigo-500/60 shadow-lg shadow-indigo-500/10 text-indigo-200 animate-pulse',
              status === 'pending' && 'bg-slate-900/40 border-slate-800 text-slate-400'
            )}
          >
            <div className="flex items-center gap-3.5">
              <div
                className={clsx(
                  'w-9 h-9 rounded-xl flex items-center justify-center border',
                  status === 'pass' && 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400',
                  status === 'fail' && 'bg-rose-500/20 border-rose-500/40 text-rose-400',
                  status === 'running' && 'bg-indigo-500/20 border-indigo-500/40 text-indigo-400',
                  status === 'pending' && 'bg-slate-800 border-slate-700 text-slate-500'
                )}
              >
                <Icon className="w-4 h-4" />
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <h5 className="text-sm font-semibold text-slate-100">{stage.name}</h5>
                </div>
                <p className="text-xs text-slate-400">
                  {stepResult?.details || stage.desc}
                </p>
              </div>
            </div>

            <div className="shrink-0 pl-3">
              {status === 'pass' && (
                <div className="flex items-center gap-1.5 text-xs font-semibold text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-lg border border-emerald-500/20">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>PASS</span>
                </div>
              )}
              {status === 'fail' && (
                <div className="flex items-center gap-1.5 text-xs font-semibold text-rose-400 bg-rose-500/10 px-2.5 py-1 rounded-lg border border-rose-500/20">
                  <XCircle className="w-4 h-4" />
                  <span>FAIL</span>
                </div>
              )}
              {status === 'running' && (
                <div className="flex items-center gap-1.5 text-xs font-semibold text-indigo-400 bg-indigo-500/10 px-2.5 py-1 rounded-lg border border-indigo-500/20">
                  <Clock className="w-4 h-4 animate-spin" />
                  <span>CHECKING</span>
                </div>
              )}
              {status === 'pending' && (
                <span className="text-xs font-mono text-slate-500 uppercase px-2">PENDING</span>
              )}
            </div>
          </motion.div>
        );
      })}
    </div>
  );
}
