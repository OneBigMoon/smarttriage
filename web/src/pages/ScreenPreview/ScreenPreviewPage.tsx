import { useState, useEffect, useCallback, useRef } from 'react'
import { Tag, Typography, Space, Button, Tooltip, Empty, Select, Input } from 'antd'
import {
  ReloadOutlined, FullscreenOutlined,
  DesktopOutlined, SearchOutlined, CheckCircleOutlined,
  PoweroffOutlined, DisconnectOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import { boxApi } from '../../services/api'

const { Text } = Typography

const STATUS_ICON: Record<string, React.ReactNode> = {
  '正常': <CheckCircleOutlined style={{ color: '#22c55e' }} />,
  '关机': <PoweroffOutlined style={{ color: '#f59e0b' }} />,
  '断开': <DisconnectOutlined style={{ color: '#ef4444' }} />,
}

const STATUS_TAG: Record<string, React.ReactNode> = {
  '正常': <Tag color="success">在线</Tag>,
  '关机': <Tag color="warning">关机</Tag>,
  '断开': <Tag color="error">离线</Tag>,
}

const STATUS_COLOR: Record<string, string> = {
  '正常': '#22c55e',
  '关机': '#f59e0b',
  '断开': '#ef4444',
}

export default function ScreenPreviewPage() {
  const [boxes, setBoxes] = useState<any[]>([])
  const [selBox, setSelBox] = useState<any | null>(null)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [refreshTs, setRefreshTs] = useState(0)          // bump → force iframe re-mount
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const load = useCallback(async () => {
    try {
      const { data } = await boxApi.query({})
      const list = (data.result as any[]) || []
      setBoxes(list)
      // If selected box is no longer in the list (deleted?), clear selection
      if (selBox && !list.find(b => b.id === selBox.id)) {
        setSelBox(null)
      }
    } catch {}
  }, [selBox])

  useEffect(() => { load() }, [load])

  // Auto refresh: 每 5 秒重新加载数据 + 刷新 iframe
  useEffect(() => {
    if (autoRefresh) {
      intervalRef.current = setInterval(() => {
        load()
        setRefreshTs(Date.now())
      }, 5000)
    }
    return () => { if (intervalRef.current) clearInterval(intervalRef.current) }
  }, [autoRefresh, load])

  const filtered = boxes.filter(b => {
    if (statusFilter !== 'all' && b.status !== statusFilter) return false
    if (search) {
      const q = search.toLowerCase()
      return (b.no?.toLowerCase().includes(q) || b.name?.toLowerCase().includes(q))
    }
    return true
  })

  const stats = {
    total: boxes.length,
    normal: boxes.filter(b => b.status === '正常').length,
    off: boxes.filter(b => b.status === '关机').length,
    disc: boxes.filter(b => b.status === '断开').length,
  }

  const previewUrl = selBox
    ? `${boxApi.previewUrl(selBox.id)}?t=${refreshTs || Date.now()}`
    : null

  const hasTemplate = selBox?.template?.name || selBox?.template?.id

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      {/* Top bar */}
      <div style={{ flexShrink: 0, marginBottom: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <Space>
            <Text strong style={{ fontSize: 16, color: 'var(--text-primary)' }}>终端屏幕预览</Text>
            <Text style={{ color: 'var(--text-tertiary)', fontSize: 12 }}>
              选择一台终端，查看其 Android 屏幕显示效果
            </Text>
          </Space>
          <Space>
            <Button
              size="small"
              icon={autoRefresh ? <SyncOutlined spin /> : <SyncOutlined />}
              type={autoRefresh ? 'primary' : 'default'}
              onClick={() => setAutoRefresh(!autoRefresh)}
            >
              自动刷新
            </Button>
            <Button size="small" icon={<ReloadOutlined />} onClick={() => { load(); setRefreshTs(Date.now()) }}>
              立即刷新
            </Button>
          </Space>
        </div>
      </div>

      {/* Main area: terminal list (left) + preview (right) */}
      <div style={{ flex: 1, display: 'flex', gap: 12, overflow: 'hidden', minHeight: 0 }}>
        {/* ─── Left: terminal list ─── */}
        <div style={{
          width: 280, flexShrink: 0,
          display: 'flex', flexDirection: 'column',
          background: 'var(--bg-surface)',
          border: '1px solid var(--border)',
          borderRadius: 8,
          overflow: 'hidden',
        }}>
          {/* Filters */}
          <div style={{ padding: '8px 10px', borderBottom: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: 6 }}>
            <Input
              prefix={<SearchOutlined />}
              placeholder="搜索编号/名称"
              value={search}
              onChange={e => setSearch(e.target.value)}
              size="small"
              allowClear
            />
            <Select value={statusFilter} onChange={setStatusFilter} size="small" style={{ width: '100%' }}>
              <Select.Option value="all">全部 ({stats.total})</Select.Option>
              <Select.Option value="正常">在线 ({stats.normal})</Select.Option>
              <Select.Option value="关机">关机 ({stats.off})</Select.Option>
              <Select.Option value="断开">离线 ({stats.disc})</Select.Option>
            </Select>
          </div>

          {/* Scrollable terminal list */}
          <div style={{ flex: 1, overflow: 'auto' }}>
            {filtered.length === 0 ? (
              <div style={{ padding: 30, textAlign: 'center' }}>
                <Empty description={<Text style={{ color: 'var(--text-tertiary)', fontSize: 12 }}>暂无终端</Text>} image={Empty.PRESENTED_IMAGE_SIMPLE} />
              </div>
            ) : (
              filtered.map(box => (
                <div
                  key={box.id}
                  onClick={() => setSelBox(box)}
                  style={{
                    padding: '8px 10px',
                    cursor: 'pointer',
                    borderBottom: '1px solid var(--border)',
                    background: selBox?.id === box.id ? 'rgba(59,130,246,0.08)' : 'transparent',
                    borderLeft: selBox?.id === box.id ? `3px solid ${STATUS_COLOR[box.status] || '#3b82f6'}` : '3px solid transparent',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 2 }}>
                    <Space size={6}>
                      {STATUS_ICON[box.status]}
                      <Text strong style={{ fontSize: 13, color: 'var(--text-primary)' }}>{box.no}</Text>
                    </Space>
                    {(STATUS_TAG as any)[box.status]}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Text style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>{box.name}</Text>
                    {box.org?.name && (
                      <Text style={{ fontSize: 10, color: 'var(--text-tertiary)' }}>{box.org.name}</Text>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* ─── Right: preview area ─── */}
        <div style={{
          flex: 1,
          display: 'flex', flexDirection: 'column',
          background: 'var(--bg-surface)',
          border: '1px solid var(--border)',
          borderRadius: 8,
          overflow: 'hidden',
        }}>
          {!selBox ? (
            /* No selection */
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12 }}>
              <DesktopOutlined style={{ fontSize: 48, color: 'var(--text-tertiary)', opacity: 0.2 }} />
              <Text style={{ color: 'var(--text-tertiary)', fontSize: 14 }}>请在左侧选择一台终端</Text>
            </div>
          ) : (
            <>
              {/* Preview header */}
              <div style={{
                padding: '8px 16px',
                borderBottom: '1px solid var(--border)',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              }}>
                <Space>
                  {STATUS_ICON[selBox.status]}
                  <Text strong style={{ fontSize: 14, color: 'var(--text-primary)' }}>
                    {selBox.no} — {selBox.name}
                  </Text>
                  {(STATUS_TAG as any)[selBox.status]}
                  {selBox.template?.name && (
                    <Tag style={{ fontSize: 11, lineHeight: '18px', margin: 0 }}>{selBox.template.name}</Tag>
                  )}
                </Space>
                <Space>
                  <Text style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>
                    {selBox.org?.name || '未分组'} · {selBox.ip || '无 IP'}
                  </Text>
                  <Tooltip title="新标签打开">
                    <Button size="small" type="text"
                      icon={<FullscreenOutlined style={{ fontSize: 14 }} />}
                      onClick={() => window.open(previewUrl!, `preview-${selBox.id}`)} />
                  </Tooltip>
                </Space>
              </div>

              {/* Preview iframe */}
              <div style={{
                flex: 1,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: '#000',
                padding: 16,
              }}>
                {selBox.status === '正常' && hasTemplate ? (
                  <div style={{
                    width: '100%', maxWidth: 960,
                    aspectRatio: '4 / 3',
                    position: 'relative',
                    borderRadius: 4,
                    overflow: 'hidden',
                    boxShadow: '0 8px 40px rgba(0,0,0,0.6)',
                  }}>
                    <iframe
                      key={`pv-${selBox.id}-${refreshTs}`}
                      src={previewUrl!}
                      style={{
                        position: 'absolute', top: 0, left: 0,
                        width: '100%', height: '100%',
                        border: 'none',
                      }}
                      title={`${selBox.no} preview`}
                      sandbox="allow-scripts allow-same-origin"
                    />
                  </div>
                ) : (
                  <div style={{ textAlign: 'center' }}>
                    <DesktopOutlined style={{ fontSize: 48, color: 'var(--text-tertiary)', opacity: 0.3 }} />
                    <div style={{ marginTop: 12, color: 'var(--text-tertiary)', fontSize: 14 }}>
                      {selBox.status !== '正常' ? '终端离线，无法预览' : '未配置显示模板'}
                    </div>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
