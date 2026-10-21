import React, { useState, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import CommandPalette from './CommandPalette';
import NotificationDrawer from './NotificationDrawer';
import PageTransition from '../components/ui/PageTransition';

export default function AppShell() {
  const [collapsed, setCollapsed] = useState(false);
  const [commandOpen, setCommandOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const location = useLocation();

  // Global Ctrl+K / Cmd+K shortcut listener
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        setCommandOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <div className="relative min-h-screen bg-slate-950 text-slate-100 flex overflow-hidden font-sans">
      {/* Drifting animated gradient background blobs */}
      <div className="fixed top-[-15%] left-[-10%] w-[600px] h-[600px] bg-indigo-600/15 rounded-full blur-[140px] pointer-events-none -z-10 animate-pulse" />
      <div className="fixed bottom-[-15%] right-[-10%] w-[650px] h-[650px] bg-violet-600/10 rounded-full blur-[160px] pointer-events-none -z-10" />
      <div className="fixed top-[40%] left-[30%] w-[450px] h-[450px] bg-blue-600/5 rounded-full blur-[120px] pointer-events-none -z-10" />

      {/* Main Layout Grid */}
      <Sidebar collapsed={collapsed} setCollapsed={setCollapsed} />

      <div className="flex-1 flex flex-col h-screen overflow-hidden min-w-0">
        <Topbar
          onOpenCommand={() => setCommandOpen(true)}
          onOpenNotifications={() => setNotifOpen(true)}
        />

        <main className="flex-1 overflow-y-auto p-6 md:p-8">
          <PageTransition key={location.pathname}>
            <Outlet />
          </PageTransition>
        </main>
      </div>

      {/* Overlays */}
      <CommandPalette isOpen={commandOpen} onClose={() => setCommandOpen(false)} />
      <NotificationDrawer isOpen={notifOpen} onClose={() => setNotifOpen(false)} />
    </div>
  );
}
