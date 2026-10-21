import React, { useRef, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { UploadCloud, FileCheck, X } from "lucide-react";
import { fadeScale } from "../../lib/motion";

export default function FileDrop({ onFileSelect, accept = "*", label = "Drop files here or click to browse", sublabel = "Maximum file size: 10MB" }) {
  const [dragging, setDragging] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const inputRef = useRef(null);

  const handleDragOver = e => {
    e.preventDefault();
    setDragging(true);
  };

  const handleDragLeave = () => {
    setDragging(false);
  };

  const handleDrop = e => {
    e.preventDefault();
    setDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const handleChange = e => {
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  };

  const handleFile = file => {
    setSelectedFile(file);
    if (onFileSelect) onFileSelect(file);
  };

  const clearFile = e => {
    e.stopPropagation();
    setSelectedFile(null);
    if (inputRef.current) inputRef.current.value = "";
    if (onFileSelect) onFileSelect(null);
  };

  const formatBytes = bytes => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  return (
    <div className="w-full">
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        onChange={handleChange}
        className="hidden"
      />
      <motion.div
        animate={{
          scale: dragging ? 1.02 : 1,
          borderColor: dragging ? "rgba(99, 102, 241, 0.8)" : "rgba(255, 255, 255, 0.15)",
          backgroundColor: dragging ? "rgba(99, 102, 241, 0.08)" : "rgba(255, 255, 255, 0.02)",
        }}
        transition={{ duration: 0.2 }}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        className="border-2 border-dashed rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer transition-colors group relative overflow-hidden"
      >
        <AnimatePresence mode="wait">
          {selectedFile ? (
            <motion.div
              key="selected"
              {...fadeScale}
              className="flex items-center gap-4 bg-indigo-500/10 border border-indigo-500/30 px-5 py-3 rounded-xl max-w-full"
              onClick={e => e.stopPropagation()}
            >
              <div className="w-10 h-10 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center shrink-0">
                <FileCheck size={20} />
              </div>
              <div className="min-w-0 pr-2">
                <div className="text-sm font-medium text-white truncate max-w-xs">{selectedFile.name}</div>
                <div className="text-xs text-slate-400">{formatBytes(selectedFile.size)}</div>
              </div>
              <button
                type="button"
                onClick={clearFile}
                className="p-1 rounded-md text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
              >
                <X size={16} />
              </button>
            </motion.div>
          ) : (
            <motion.div key="empty" className="text-center" {...fadeScale}>
              <div className="w-14 h-14 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center mx-auto mb-4 text-slate-400 group-hover:text-indigo-400 group-hover:border-indigo-500/40 group-hover:scale-105 transition-all">
                <UploadCloud size={28} />
              </div>
              <div className="text-sm font-medium text-slate-200 group-hover:text-white transition-colors">
                {label}
              </div>
              <div className="text-xs text-slate-400 mt-1">{sublabel}</div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  );
}
