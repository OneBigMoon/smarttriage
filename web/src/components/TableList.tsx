/**
 * TableList — 通用分页表格组件
 * 统一分页、空状态、加载状态、行选择
 */
import { useState, useEffect, useCallback } from 'react'
import { Table, Empty, Spin, Typography } from 'antd'

const { Text } = Typography

interface TableListProps {
  /** 数据加载函数，返回 { data: any[] } */
  fetchFn: (params: { page: number; pageSize: number; [key: string]: any }) => Promise<{ data: any }>
  /** 额外查询参数 */
  extraParams?: Record<string, any>
  /** 表格列定义 */
  columns: any[]
  /** 行唯一标识 */
  rowKey?: string
  /** 默认每页条数 */
  defaultPageSize?: number
  /** 行选择回调 */
  onRowSelect?: (record: any) => void
  /** 当前选中行 */
  selectedRow?: any
  /** 行样式 */
  rowClassName?: (record: any) => string
}

export default function TableList({
  fetchFn, extraParams = {}, columns, rowKey = '_id',
  defaultPageSize = 20, onRowSelect, selectedRow, rowClassName,
}: TableListProps) {
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(defaultPageSize)
  const [total, setTotal] = useState(0)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await fetchFn({ page, pageSize, ...extraParams })
      const list = res?.data?.result
      if (Array.isArray(list)) {
        setData(list)
        setTotal(list.length) // API 返回全量，前端分页
      }
    } catch {}
    setLoading(false)
  }, [page, pageSize, JSON.stringify(extraParams)])

  useEffect(() => { load() }, [load])

  // 前端分页（API 返回全量数据）
  const pagedData = data.slice((page - 1) * pageSize, page * pageSize)

  if (!loading && data.length === 0) {
    return (
      <div style={{ padding: 60, textAlign: 'center' }}>
        <Empty description={<Text style={{ color: 'var(--text-tertiary)' }}>暂无数据</Text>} />
      </div>
    )
  }

  return (
    <Table
      columns={columns}
      dataSource={pagedData}
      rowKey={rowKey}
      loading={loading}
      size="small"
      pagination={{
        current: page,
        pageSize: pageSize,
        total: total,
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: (t) => `共 ${t} 条`,
        pageSizeOptions: ['10', '20', '50', '100'],
        onChange: (p, ps) => { setPage(p); setPageSize(ps) },
      }}
      onRow={(record) => ({
        onClick: () => onRowSelect?.(record),
        style: {
          cursor: 'pointer',
          background: selectedRow?.[rowKey] === record[rowKey] ? 'rgba(59,130,246,0.1)' : undefined,
        },
      })}
      rowClassName={rowClassName}
    />
  )
}
