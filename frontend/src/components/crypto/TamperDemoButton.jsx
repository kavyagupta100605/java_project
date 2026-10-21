import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Bug, Sparkles, Check, RefreshCw } from 'lucide-react';
import Button from '../ui/Button';

export default function TamperDemoButton({ originalFile, onTamperToggle, isTampered }) {
  return (
    <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div className="flex items-center gap-3">
        <div className={`w-9 h-9 rounded-xl flex items-center justify-center border transition-colors ${
          isTampered ? 'bg-rose-500/20 text-rose-400 border-rose-500/40' : 'bg-indigo-500/20 text-indigo-400 border-indigo-500/40'
        }`}>
          <Bug className="w-4 h-4" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-slate-200">Interactive Security Demo</span>
            {isTampered && (
              <span className="text-[10px] bg-rose-500/20 text-rose-300 border border-rose-500/40 px-2 py-0.5 rounded font-mono">
                1-Byte Modified
              </span>
            )}
          </div>
          <p className="text-xs text-slate-400">
            {isTampered
              ? 'Document has been altered. Verification should flag TAMPERED!'
              : 'Simulate an attacker altering 1 byte in the file before verification.'}
          </p>
        </div>
      </div>

      <Button
        variant={isTampered ? 'secondary' : 'danger'}
        size="sm"
        icon={isTampered ? RefreshCw : Bug}
        onClick={onTamperToggle}
      >
        {isTampered ? 'Revert to Original' : 'Simulate Tampering'}
      </Button>
    </div>
  );
}
