import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

const STATUS_COLORS = {
  ACTIVE: '#10b981',
  EXPIRED: '#f59e0b',
  REVOKED: '#f43f5e',
  SUSPENDED: '#8b5cf6',
  PENDING: '#0ea5e9',
};

function CustomDonutTooltip({ active, payload }) {
  if (active && payload && payload.length) {
    const data = payload[0];
    return (
      <div className="glass-dropdown p-2.5 border border-slate-700/80 shadow-xl text-xs flex items-center gap-2">
        <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: data.payload.fill }} />
        <span className="font-semibold text-slate-200">{data.name}:</span>
        <span className="font-mono text-slate-100">{data.value}</span>
      </div>
    );
  }
  return null;
}

export default function StatusDonut({ data = [], totalCount = 0 }) {
  const chartData = data.map((item) => ({
    name: item.status,
    value: item.count,
    fill: STATUS_COLORS[item.status] || '#64748b',
  }));

  return (
    <div className="relative w-full h-64 flex items-center justify-center">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Tooltip content={<CustomDonutTooltip />} />
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            innerRadius={65}
            outerRadius={85}
            paddingAngle={4}
            dataKey="value"
            stroke="none"
          >
            {chartData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.fill} />
            ))}
          </Pie>
        </PieChart>
      </ResponsiveContainer>

      {/* Center Label */}
      <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
        <span className="text-2xl font-bold text-slate-100 tracking-tight">{totalCount}</span>
        <span className="text-[10px] uppercase tracking-wider text-slate-400 font-medium">Certificates</span>
      </div>
    </div>
  );
}
