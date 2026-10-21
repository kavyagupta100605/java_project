import React from 'react';
import { motion } from 'framer-motion';
import { Check } from 'lucide-react';
import clsx from 'clsx';

export default function Stepper({ steps, currentStep, className = '' }) {
  return (
    <div className={clsx('w-full py-4', className)}>
      <div className="flex items-center justify-between relative">
        {/* Background track line */}
        <div className="absolute top-1/2 left-0 right-0 h-0.5 bg-slate-800 -translate-y-1/2 z-0" />
        
        {/* Active progress track */}
        <motion.div
          className="absolute top-1/2 left-0 h-0.5 bg-gradient-to-r from-indigo-500 to-violet-500 -translate-y-1/2 z-0"
          initial={false}
          animate={{
            width: steps.length > 1 ? `${(currentStep / (steps.length - 1)) * 100}%` : '0%',
          }}
          transition={{ duration: 0.4, ease: 'easeInOut' }}
        />

        {steps.map((step, idx) => {
          const isCompleted = idx < currentStep;
          const isCurrent = idx === currentStep;
          return (
            <div key={idx} className="relative z-10 flex flex-col items-center">
              <motion.div
                initial={false}
                animate={{
                  scale: isCurrent ? 1.15 : 1,
                  backgroundColor: isCompleted
                    ? 'rgb(99, 102, 241)'
                    : isCurrent
                    ? 'rgb(15, 23, 42)'
                    : 'rgb(30, 41, 59)',
                  borderColor: isCompleted || isCurrent ? 'rgb(99, 102, 241)' : 'rgb(51, 65, 85)',
                }}
                className={clsx(
                  'w-9 h-9 rounded-full flex items-center justify-center border-2 text-xs font-semibold shadow-lg transition-colors',
                  isCurrent && 'ring-4 ring-indigo-500/20 text-indigo-400',
                  isCompleted && 'text-white',
                  !isCompleted && !isCurrent && 'text-slate-400'
                )}
              >
                {isCompleted ? <Check className="w-4 h-4 stroke-[2.5]" /> : idx + 1}
              </motion.div>
              <span
                className={clsx(
                  'mt-2 text-xs font-medium tracking-tight whitespace-nowrap',
                  isCurrent ? 'text-indigo-400' : isCompleted ? 'text-slate-300' : 'text-slate-500'
                )}
              >
                {step.label || step}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
