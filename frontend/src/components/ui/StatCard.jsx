import React from "react";
import { motion } from "framer-motion";
import AnimatedCounter from "./AnimatedCounter";
import { item } from "../../lib/motion";

const COLOR_MAP = {
  indigo: {
    hex: "#6366f1",
    bg: "bg-indigo-500/15",
    text: "text-indigo-400",
    border: "border-indigo-500/30",
    glow: "#6366f1",
  },
  emerald: {
    hex: "#10b981",
    bg: "bg-emerald-500/15",
    text: "text-emerald-400",
    border: "border-emerald-500/30",
    glow: "#10b981",
  },
  amber: {
    hex: "#f59e0b",
    bg: "bg-amber-500/15",
    text: "text-amber-400",
    border: "border-amber-500/30",
    glow: "#f59e0b",
  },
  rose: {
    hex: "#f43f5e",
    bg: "bg-rose-500/15",
    text: "text-rose-400",
    border: "border-rose-500/30",
    glow: "#f43f5e",
  },
  cyan: {
    hex: "#06b6d4",
    bg: "bg-cyan-500/15",
    text: "text-cyan-400",
    border: "border-cyan-500/30",
    glow: "#06b6d4",
  },
  purple: {
    hex: "#a855f7",
    bg: "bg-purple-500/15",
    text: "text-purple-400",
    border: "border-purple-500/30",
    glow: "#a855f7",
  },
};

export default function StatCard({
  icon: Icon,
  title,
  label,
  value = 0,
  subtitle,
  description,
  color = "indigo",
  onClick,
  active = false,
}) {
  const displayTitle = title || label || "";
  const displaySubtitle = subtitle || description || "";

  const palette = COLOR_MAP[color] || {
    hex: typeof color === "string" && color.startsWith("#") ? color : "#6366f1",
    bg: "bg-indigo-500/15",
    text: "text-indigo-400",
    border: "border-indigo-500/30",
    glow: typeof color === "string" && color.startsWith("#") ? color : "#6366f1",
  };

  return (
    <motion.button
      variants={item}
      whileHover={{ y: -4, scale: 1.01 }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      type="button"
      className={`glass p-5 text-left relative overflow-hidden group w-full cursor-pointer transition-all duration-300 rounded-2xl flex flex-col justify-between ${
        active
          ? "border-indigo-500/60 ring-1 ring-indigo-500/40 shadow-lg shadow-indigo-500/10"
          : "hover:border-slate-700/80 hover:shadow-xl"
      }`}
    >
      {/* Ambient Radial Glow */}
      <motion.div
        className="absolute -right-6 -top-6 w-28 h-28 rounded-full blur-2xl opacity-20 pointer-events-none"
        style={{ background: palette.glow }}
        animate={{ scale: [1, 1.25, 1], opacity: [0.15, 0.3, 0.15] }}
        transition={{ repeat: Infinity, duration: 4, ease: "easeInOut" }}
      />

      {/* Top Row: Icon */}
      <div className="flex items-center justify-between mb-3 relative z-10">
        <div
          className={`w-10 h-10 rounded-xl flex items-center justify-center border transition-transform duration-300 group-hover:scale-105 ${palette.bg} ${palette.text} ${palette.border}`}
        >
          {Icon && <Icon size={20} />}
        </div>
      </div>

      {/* Heading / Title */}
      <div className="relative z-10 mb-1">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 group-hover:text-slate-300 transition-colors">
          {displayTitle}
        </h4>
      </div>

      {/* Big Counter Value */}
      <div className="text-3xl font-extrabold tracking-tight text-white mb-2 relative z-10">
        <AnimatedCounter value={Number(value) || 0} />
      </div>

      {/* Subtitle / Description */}
      {displaySubtitle && (
        <div className="text-xs text-slate-400 group-hover:text-slate-300 transition-colors relative z-10 pt-2 border-t border-slate-800/80 truncate">
          {displaySubtitle}
        </div>
      )}
    </motion.button>
  );
}
