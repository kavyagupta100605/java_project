export const ease = [0.22, 1, 0.36, 1];
export const spring = { type: "spring", stiffness: 300, damping: 26 };
export const gentleSpring = { type: "spring", stiffness: 180, damping: 20 };

export const pageVariants = {
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0, transition: { duration: 0.4, ease } },
  exit:    { opacity: 0, y: -12, transition: { duration: 0.2 } },
};

export const container = {
  hidden: {},
  show: { transition: { staggerChildren: 0.07, delayChildren: 0.1 } },
};

export const item = {
  hidden: { opacity: 0, y: 20 },
  show:   { opacity: 1, y: 0, transition: { duration: 0.45, ease } },
};

export const fadeScale = {
  initial: { opacity: 0, scale: 0.92 },
  animate: { opacity: 1, scale: 1, transition: spring },
  exit:    { opacity: 0, scale: 0.95, transition: { duration: 0.15 } },
};
