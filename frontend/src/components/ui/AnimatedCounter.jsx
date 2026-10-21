import React, { useEffect } from "react";
import { animate, useMotionValue, useTransform, motion } from "framer-motion";

export default function AnimatedCounter({ value, duration = 1.0 }) {
  const mv = useMotionValue(0);
  const rounded = useTransform(mv, v => Math.round(v));

  useEffect(() => {
    const controls = animate(mv, value, {
      duration,
      ease: [0.16, 1, 0.3, 1],
    });
    return controls.stop;
  }, [value, duration, mv]);

  return <motion.span className="tabular-nums">{rounded}</motion.span>;
}
