import React from 'react';
import clsx from 'clsx';

export default function Skeleton({ className = '', variant = 'rect' }) {
  const variantStyles = {
    rect: 'rounded-xl',
    circle: 'rounded-full',
    text: 'rounded-md h-4',
  };

  return (
    <div
      className={clsx(
        'shimmer bg-slate-800/60 border border-slate-700/30',
        variantStyles[variant] || 'rounded-xl',
        className
      )}
    />
  );
}
