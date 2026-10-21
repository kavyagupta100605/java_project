import React, { useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, ChevronUp, ChevronLeft, ChevronRight, Search, Inbox } from 'lucide-react';
import EmptyState from './EmptyState';
import Skeleton from './Skeleton';

export default function DataTable({
  columns,
  data = [],
  loading = false,
  searchPlaceholder = 'Search records...',
  searchKeys = [],
  defaultSortField = null,
  defaultSortDir = 'asc',
  pageSize = 10,
  renderExpandedRow = null,
  emptyTitle = 'No data available',
  emptyDescription = 'There are no records to display.',
  actions = null,
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState(defaultSortField);
  const [sortDir, setSortDir] = useState(defaultSortDir);
  const [currentPage, setCurrentPage] = useState(1);
  const [expandedRows, setExpandedRows] = useState(new Set());

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDir((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('asc');
    }
  };

  const toggleExpand = (id) => {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  // Filtered & Sorted Data
  const processedData = useMemo(() => {
    let result = [...data];

    if (searchTerm.trim() && searchKeys.length > 0) {
      const query = searchTerm.toLowerCase();
      result = result.filter((row) =>
        searchKeys.some((key) => {
          const val = row[key];
          return val !== null && val !== undefined && String(val).toLowerCase().includes(query);
        })
      );
    }

    if (sortField) {
      result.sort((a, b) => {
        let aVal = a[sortField];
        let bVal = b[sortField];
        if (aVal === null || aVal === undefined) return 1;
        if (bVal === null || bVal === undefined) return -1;
        if (typeof aVal === 'string') {
          return sortDir === 'asc'
            ? aVal.localeCompare(bVal)
            : bVal.localeCompare(aVal);
        }
        return sortDir === 'asc' ? aVal - bVal : bVal - aVal;
      });
    }

    return result;
  }, [data, searchTerm, searchKeys, sortField, sortDir]);

  const totalPages = Math.ceil(processedData.length / pageSize) || 1;
  const paginatedData = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return processedData.slice(start, start + pageSize);
  }, [processedData, currentPage, pageSize]);

  return (
    <div className="glass overflow-hidden flex flex-col">
      {/* Table Header with Search and Custom Actions */}
      <div className="p-4 border-b border-slate-800/80 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder={searchPlaceholder}
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setCurrentPage(1);
            }}
            className="w-full pl-10 pr-4 py-2 bg-slate-900/60 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-indigo-500/80 focus:ring-1 focus:ring-indigo-500/80 transition-colors"
          />
        </div>
        {actions && <div className="flex items-center gap-2">{actions}</div>}
      </div>

      {/* Table View */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="text-xs uppercase bg-slate-900/80 text-slate-400 border-b border-slate-800 font-semibold tracking-wider">
            <tr>
              {renderExpandedRow && <th className="w-8 px-4 py-3" />}
              {columns.map((col) => (
                <th
                  key={col.key || col.field}
                  onClick={() => col.sortable !== false && col.field && handleSort(col.field)}
                  className={`px-4 py-3 ${
                    col.sortable !== false && col.field ? 'cursor-pointer select-none hover:text-slate-100' : ''
                  } ${col.headerClassName || ''}`}
                >
                  <div className="flex items-center gap-1.5">
                    <span>{col.header}</span>
                    {col.field && sortField === col.field && (
                      sortDir === 'asc' ? <ChevronUp className="w-3.5 h-3.5 text-indigo-400" /> : <ChevronDown className="w-3.5 h-3.5 text-indigo-400" />
                    )}
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/50">
            {loading ? (
              Array.from({ length: pageSize }).map((_, idx) => (
                <tr key={idx} className="animate-pulse">
                  {renderExpandedRow && <td className="px-4 py-3.5"><Skeleton variant="rect" className="w-4 h-4" /></td>}
                  {columns.map((col, cIdx) => (
                    <td key={cIdx} className="px-4 py-3.5">
                      <Skeleton variant="text" className="w-3/4" />
                    </td>
                  ))}
                </tr>
              ))
            ) : paginatedData.length === 0 ? (
              <tr>
                <td colSpan={columns.length + (renderExpandedRow ? 1 : 0)} className="py-12">
                  <EmptyState icon={Inbox} title={emptyTitle} description={emptyDescription} />
                </td>
              </tr>
            ) : (
              paginatedData.map((row, idx) => {
                const rowId = row.id || idx;
                const isExpanded = expandedRows.has(rowId);
                return (
                  <React.Fragment key={rowId}>
                    <motion.tr
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      transition={{ duration: 0.15 }}
                      className="hover:bg-slate-800/40 transition-colors group cursor-default"
                    >
                      {renderExpandedRow && (
                        <td className="px-4 py-3 text-slate-500">
                          <button
                            onClick={() => toggleExpand(rowId)}
                            className="p-1 hover:bg-slate-700/50 rounded transition-colors text-slate-400 hover:text-slate-200"
                          >
                            <ChevronDown
                              className={`w-4 h-4 transition-transform duration-200 ${
                                isExpanded ? 'rotate-180 text-indigo-400' : ''
                              }`}
                            />
                          </button>
                        </td>
                      )}
                      {columns.map((col, cIdx) => (
                        <td key={cIdx} className={`px-4 py-3.5 ${col.className || ''}`}>
                          {col.render ? col.render(row[col.field], row) : row[col.field] ?? '—'}
                        </td>
                      ))}
                    </motion.tr>

                    {renderExpandedRow && isExpanded && (
                      <tr className="bg-slate-900/40">
                        <td colSpan={columns.length + 1} className="px-6 py-4 border-y border-slate-800/80">
                          <motion.div
                            initial={{ opacity: 0, height: 0 }}
                            animate={{ opacity: 1, height: 'auto' }}
                            exit={{ opacity: 0, height: 0 }}
                            transition={{ duration: 0.2 }}
                          >
                            {renderExpandedRow(row)}
                          </motion.div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      {!loading && processedData.length > 0 && (
        <div className="px-4 py-3 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400 bg-slate-900/30">
          <div>
            Showing <span className="font-semibold text-slate-200">{Math.min((currentPage - 1) * pageSize + 1, processedData.length)}</span> to{' '}
            <span className="font-semibold text-slate-200">{Math.min(currentPage * pageSize, processedData.length)}</span> of{' '}
            <span className="font-semibold text-slate-200">{processedData.length}</span> results
          </div>
          <div className="flex items-center gap-1.5">
            <button
              onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
              disabled={currentPage === 1}
              className="p-1.5 rounded-lg border border-slate-800 bg-slate-900/60 hover:bg-slate-800 text-slate-300 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-2 font-medium text-slate-300">
              {currentPage} / {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
              disabled={currentPage === totalPages}
              className="p-1.5 rounded-lg border border-slate-800 bg-slate-900/60 hover:bg-slate-800 text-slate-300 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
