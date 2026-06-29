import { useState, useEffect } from 'react'
import { Card, Row, Col, Badge, Button, Space, Typography, Tag, List, message, Progress, Tooltip, Divider } from 'antd'
import {
  CheckCircleOutlined, CloseCircleOutlined, DisconnectOutlined,
  ReloadOutlined, ThunderboltOutlined, WarningOutlined,
  SettingOutlined, DatabaseOutlined, ApiOutlined, CloudServerOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import AnimatedNumber from '../../components/AnimatedNumber'
import api from '../../services/api'

const { Text } = Typography

const HEALTH_ICONS: Record<string, React.ReactNode> = {
  postgres: <DatabaseOutlined />,
  redis: <CloudServerOutlined />,
  oracle: <ApiOutlined />,
  socketio: <ApiOutlined />,
}

export default function DashboardPage() {
  const [boxes, setBoxes] = useState<any[]>([])
  const [health, setHealth] = useState<Record<string, any>>({})
  const [loading, setLoading] = useState(false)
  const [healthLoading, setHealthLoading] = useState(false)
  const nav = useNavigate()

  const load = async () => {
    setLoading(true)
    try { const { data } = await api.post('/api/v1/boxes/query', {}); setBoxes((data.result as any[]) || []) } catch {}
    setLoading(false)
  }

  const loadHealth = async () => {
    setHealthLoading(true)
    try { const { data } = await api.get('/api/v1/system/health'); setHealth(data.result || {}) }
    catch { setHealth({}) }
    setHealthLoading(false)
  }

  useEffect(() => { load(); loadHealth() }, [])

  const stats = {
    total: boxes.length,
    normal: boxes.filter(b => b.status === '正常').length,
    off: boxes.filter(b => b.status === '关机').length,
    disc: boxes.filter(b => b.status === '断开').length,
  }
  const onlineRate = stats.total > 0 ? Math.round(stats.normal / stats.total * 100) : 0

  const doQuickPower = async (ids: string[], cmd: string) => {
    for (const id of ids) await api.post('/api/v1/boxes/power', null, { params: { id, cmd } })
    message.success('操作完成'); load()
  }

  const offlineBoxes = boxes.filter(b => b.status === '断开')
  const allHealthy = Object.values(health).every((h: any) => h.status === 'ok')

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column', gap: 12, overflow: 'hidden' }}>
      {/* ═══ Row 1: 统计卡片 ═══ */}
      <Row gutter={[12, 12]} style={{ flexShrink: 0 }}>
        {[
          { label: '终端总数', value: stats.total, bg: 'linear-gradient(135deg, #3b82f6, #2563eb)' },
          { label: '正常运行', value: stats.normal, bg: 'linear-gradient(135deg, #22c55e, #16a34a)' },
          { label: '已关机', value: stats.off, bg: 'linear-gradient(135deg, #f59e0b, #d97706)' },
          { label: '已离线', value: stats.disc, bg: 'linear-gradient(135deg, #ef4444, #dc2626)' },
        ].map((s, i) => (
          <Col xs={12} sm={6} key={i}>
            <div style={{ padding: '14px 18px', borderRadius: 10, background: s.bg, cursor: 'pointer' }}
              onClick={() => nav('/box')}>
              <div style={{ color: 'rgba(255,255,255,0.65)', fontSize: 12 }}>{s.label}</div>
              <div style={{ color: '#fff', fontSize: 28, fontWeight: 700, lineHeight: 1.2, marginTop: 4 }}>
                <AnimatedNumber value={s.value} />
              </div>
            </div>
          </Col>
        ))}
      </Row>

      {/* ═══ Row 2: 三列内容 ═══ */}
      <div style={{ flex: 1, display: 'flex', gap: 12, minHeight: 0, overflow: 'hidden' }}>
        {/* 在线率 */}
        <div style={{ flex: '0 0 200px' }}>
          <Card bordered={false} bodyStyle={{ padding: 16, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <Text strong style={{ color: 'var(--text-primary)', fontSize: 13 }}>在线率</Text>
              <div style={{ margin: '12px 0 8px', fontSize: 36, fontWeight: 700, color: onlineRate >= 80 ? '#22c55e' : '#f59e0b' }}>
                <AnimatedNumber value={onlineRate} /><span style={{ fontSize: 18 }}>%</span>
              </div>
              <Text style={{ color: 'var(--text-tertiary)', fontSize: 12 }}>{stats.normal} / {stats.total} 台</Text>
            </div>
            <Progress percent={onlineRate} strokeColor={onlineRate >= 80 ? '#22c55e' : '#f59e0b'}
              trailColor="rgba(255,255,255,0.04)" size={{ height: 6 }} showInfo={false} />
          </Card>
        </div>

        {/* 需要处理 */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <Card bordered={false} bodyStyle={{ padding: 0, height: '100%', display: 'flex', flexDirection: 'column' }}
            title={<Space><WarningOutlined style={{ color: offlineBoxes.length > 0 ? '#ef4444' : '#22c55e' }} />
              <Text strong style={{ color: 'var(--text-primary)', fontSize: 13 }}>需要处理 ({offlineBoxes.length})</Text></Space>}
            extra={offlineBoxes.length > 0 && (
              <Button type="primary" danger size="small" icon={<ReloadOutlined />}
                onClick={() => doQuickPower(offlineBoxes.map(b => b.id), 'restart')}>重启全部</Button>
            )}>
            {offlineBoxes.length === 0 ? (
              <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <CheckCircleOutlined style={{ fontSize: 28, color: '#22c55e', marginRight: 8 }} />
                <Text style={{ color: 'var(--text-secondary)' }}>一切正常</Text>
              </div>
            ) : (
              <div style={{ flex: 1, overflow: 'auto', maxHeight: 200 }}>
                <List size="small" dataSource={offlineBoxes} renderItem={(box: any) => (
                  <List.Item style={{ padding: '6px 16px' }}
                    actions={[<Button size="small" type="link" icon={<ReloadOutlined />}
                      onClick={() => doQuickPower([box.id], 'restart')}>重启</Button>]}>
                    <List.Item.Meta
                      avatar={<DisconnectOutlined style={{ color: '#ef4444', fontSize: 14 }} />}
                      title={<Text style={{ fontSize: 12, color: 'var(--text-primary)' }}>{box.no} — {box.name}</Text>}
                      description={<Text style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>{box.org?.name || '新设备'}</Text>}
                    />
                  </List.Item>
                )} />
              </div>
            )}
          </Card>
        </div>

        {/* 快捷操作 */}
        <div style={{ flex: '0 0 240px' }}>
          <Card bordered={false} bodyStyle={{ padding: 16, height: '100%', display: 'flex', flexDirection: 'column' }}
            title={<Text strong style={{ color: 'var(--text-primary)', fontSize: 13 }}>快捷操作</Text>}>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <Button block icon={<ThunderboltOutlined />} size="small"
                onClick={async () => { await api.post('/api/v1/boxes/dataenabled/enable-all'); message.success('已开启'); load() }}>
                开启全部数据推送
              </Button>
              <Button block size="small" onClick={async () => {
                const off = boxes.filter(b => b.status === '关机')
                for (const b of off) await api.post('/api/v1/boxes/power', null, { params: { id: b.id, cmd: 'on' } })
                message.success(`已开机 ${off.length} 台`); load()
              }}>全部开机 ({stats.off})</Button>
              <Button block size="small" danger onClick={async () => {
                const on = boxes.filter(b => b.status === '正常')
                for (const b of on) await api.post('/api/v1/boxes/power', null, { params: { id: b.id, cmd: 'off' } })
                message.success(`已关机 ${on.length} 台`); load()
              }}>全部关机 ({stats.normal})</Button>
              <Divider style={{ margin: '4px 0' }} />
              <Button block size="small" type="link" icon={<SettingOutlined />}
                style={{ textAlign: 'left', color: 'var(--text-secondary)' }}
                onClick={() => nav('/box')}>终端与分组 →</Button>
              <Button block size="small" type="link" icon={<DatabaseOutlined />}
                style={{ textAlign: 'left', color: 'var(--text-secondary)' }}
                onClick={() => nav('/data')}>数据源 →</Button>
              <Button block size="small" type="link" icon={<ThunderboltOutlined />}
                style={{ textAlign: 'left', color: 'var(--text-secondary)' }}
                onClick={() => nav('/template')}>模板 →</Button>
            </div>
          </Card>
        </div>
      </div>

      {/* ═══ Row 3: 系统状态 + 底部状态条 ═══ */}
      <div style={{ flexShrink: 0 }}>
        <Card bordered={false} bodyStyle={{ padding: '8px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            {/* Left: system health */}
            <Space size={16}>
              {Object.entries(health).map(([key, h]: [string, any]) => (
                <Space key={key} size={4}>
                  {HEALTH_ICONS[key]}
                  <div style={{ width: 8, height: 8, borderRadius: '50%', background: h.status === 'ok' ? '#22c55e' : '#ef4444' }} />
                  <Text style={{ fontSize: 12 }}>{h.label}</Text>
                </Space>
              ))}
              {!Object.keys(health).length && <Text style={{ fontSize: 12, color: 'var(--text-tertiary)' }}>加载中...</Text>}
            </Space>
            {/* Right: terminal status + refresh */}
            <Space size={16}>
              <Space size={12}>
                <Space size={4}><div className="status-dot online" /><Text style={{ fontSize: 12 }}>在线 {stats.normal}</Text></Space>
                <Space size={4}><div className="status-dot off" /><Text style={{ fontSize: 12 }}>关机 {stats.off}</Text></Space>
                <Space size={4}><div className="status-dot offline" /><Text style={{ fontSize: 12 }}>离线 {stats.disc}</Text></Space>
              </Space>
              <Button size="small" icon={<ReloadOutlined />} onClick={() => { load(); loadHealth() }} loading={loading || healthLoading}>刷新</Button>
            </Space>
          </div>
        </Card>
      </div>
    </div>
  )
}
