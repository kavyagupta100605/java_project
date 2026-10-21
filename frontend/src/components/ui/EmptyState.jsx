import React from 'react';
import { motion } from 'framer-motion';
import Button from './Button';

export default function EmptyState({
  icon: Icon,
  title = 'No items found',
  description = 'There are no records matching your criteria or none have been created yet.',
  actionLabel,
  onAction,
  className = '',
}) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`glass flex flex-col items-center justify-center p-12 text-center my-6 ${className}`}
    >
      {Icon && (
        <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mb-4 text-indigo-400">
          <Icon className="w-8 h-8" />
        </div>
      )}
      <h3 className="text-lg font-semibold text-slate-100 mb-1">{title}</h3>
      <p className="text-sm text-slate-400 max-w-md mb-6">{description}</p>
      {actionLabel && onAction && (
        <Button onClick={onAction} variant="primary" size="md">
          {actionLabel}
        </Button>
      )}
    </motion.div>
  );
}
