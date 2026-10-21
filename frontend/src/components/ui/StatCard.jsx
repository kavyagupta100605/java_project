import React from "react";
import { motion } from "framer-motion";
import AnimatedCounter from "./AnimatedCounter";
import { item } from "../../lib/motion";

export default function StatCard({ icon: Icon, label, value, color = "#6366f1", onClick, active = false }) {
  return (
    <motion.button
      variants={item}
      whileHover={{ y: -5, scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      className={`glass p-5 text-left relative overflow-hidden group w-full cursor-pointer transition-colors duration-200 ${
        active ? "border-indigo-500/60 ring-1 ring-indigo-500/40" : "hover:border-white/20"
      }`}
    >
      <motion.div
        className="absolute -right-6 -top-6 w-24 h-24 rounded-full blur-2xl opacity-25 pointer-events-none"
        style={{ background: color }}
        animate={{ scale: [1, 1.3, 1] }}
        transition={{ repeat: Infinity, duration: 4, ease: "easeInOut" }}
      />
      <div className="flex items-center justify-between mb-3 relative z-10">
        <div
          className="w-10 h-10 rounded-xl flex items-center justify-center border border-white/10"
          style={{ background: `${color}15`, color }}
        >
          <Icon size={20} />
        </div>
      </div>
      <div className="text-3xl font-bold tracking-tight text-white mb-1 relative z-10">
        <AnimatedCounter value={value} />
      </div>
      <div className="text-xs font-medium text-slate-400 group-hover:text-slate-300 transition-colors relative z-10">
        {label}
      </div>
    </motion.button>
  );
}
