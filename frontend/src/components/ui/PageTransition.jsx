import React from "react";
import { motion } from "framer-motion";
import { pageVariants } from "../../lib/motion";

export default function PageTransition({ children, className = "" }) {
  return (
    <motion.div
      variants={pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      className={`w-full ${className}`}
    >
      {children}
    </motion.div>
  );
}
