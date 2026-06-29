import { useState, useEffect, useMemo, useCallback } from 'react'
import {
  Row, Col, Table, Button, Input, Select, Space, Tag, Tree,
  Modal, Form, Pagination, message, Popconfirm, Typography, Dropdown,
  InputNumber, Divider, Tooltip,
} from 'antd'
import {
  CheckCircleOutlined, CloseCircleOutlined, DisconnectOutlined,
  PoweroffOutlined, ReloadOutlined, SettingOutlined, DeleteOutlined,
  SearchOutlined, FolderOutlined, FolderOpenOutlined,
  PlusOutlined, EditOutlined, DeleteOutlined as DeleteIcon,
  SwapOutlined, BulbOutlined, ClockCircleOutlined, PictureOutlined,
  AppstoreOutlined, EyeOutlined,
} from '@ant-design/icons'
import { boxApi, dsApi, styleApi, orgApi } from '../../services/api'
import api from '../../services/api'
import type { DataNode } from 'antd/es/tree'

const { Text } = Typography

export default function BoxPage() {
  // ── Data ──
  const [allBoxes, setAllBoxes] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [search, setSearch] = useState('')
  const [orgTree, setOrgTree] = useState<any[]>([])
  const [orgList, setOrgList] = useState<any[]>([])
  const [styles, setStyles] = useState<{ value: string; label: string }[]>([])
  const [dss, setDss] = useState<{ value: string; label: string }[]>([])
  const [templates, setTemplates] = useState<{ value: string; label: string }[]>([])

  // ── Filter + Pagination ──
  const [selectedOrg, setSelectedOrg] = useState<number | null>(null)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [selectedRow, setSelectedRow] = useState<any>(null)

  // ── Modals ──
  const [editBox, setEditBox] = useState<any>(null)
  const [moveBox, setMoveBox] = useState<any>(null)
  const [editOrg, setEditOrg] = useState<{ mode: 'add' | 'edit'; parent?: number } | null>(null)
  const [editForm] = Form.useForm()
  const [orgForm] = Form.useForm()

  // ── Load ──
  const load = async () => {
    setLoading(true)
    try { const { data } = await boxApi.query({ no: search, name: search }); setAllBoxes((data.result as any[]) || []) }
    catch { message.error('加载失败') }
    setLoading(false)
  }

  const loadOrgs = async () => {
    const { data } = await orgApi.query()
    const list = (data.result as any[]) || []
    setOrgList(list)
    setOrgTree(buildTree(list))
  }

  useEffect(() => { load(); loadOrgs() }, [])
  useEffect(() => { setPage(1) }, [search, selectedOrg])

  // ── Filter ──
  const filteredBoxes = useMemo(() => {
    if (selectedOrg === null) return allBoxes
    const orgIds = new Set<number>()
    const collect = (o: any) => { orgIds.add(o.id); o.children?.forEach(collect) }
    const target = orgList.find(o => o.id === selectedOrg)
    if (target) collect(target)
    return allBoxes.filter(b => orgIds.has(b.org?.id))
  }, [allBoxes, selectedOrg, orgList])

  const pagedBoxes = useMemo(() => filteredBoxes.slice((page - 1) * pageSize, page * pageSize), [filteredBoxes, page, pageSize])

  const stats = useMemo(() => ({
    total: filteredBoxes.length,
    normal: filteredBoxes.filter(b => b.status === '正常').length,
    off: filteredBoxes.filter(b => b.status === '关机').length,
    disc: filteredBoxes.filter(b => b.status === '断开').length,
  }), [filteredBoxes])

  // ── Helpers ──
  // 计算每个分组及其子分组的终端数量
  function countBoxesPerOrg(orgs: any[], boxes: any[]): Map<number, number> {
    const counts = new Map<number, number>()
    // 统计每个 org id 直接拥有的终端数
    const directCount = new Map<number, number>()
    boxes.forEach(b => {
      const orgId = b.org?.id
      if (orgId != null) directCount.set(orgId, (directCount.get(orgId) || 0) + 1)
    })
    // 递归计算：本节点 + 所有子节点
    function calc(org: any): number {
      const direct = directCount.get(org.id) || 0
      const childTotal = (org.children || []).reduce((sum: number, c: any) => sum + calc(c), 0)
      const total = direct + childTotal
      counts.set(org.id, total)
      return total
    }
    orgs.forEach(calc)
    return counts
  }

  const orgBoxCounts = useMemo(() => countBoxesPerOrg(orgList, allBoxes), [orgList, allBoxes])

  function buildTree(data: any[]): DataNode[] {
    return data.map(n => {
      const count = orgBoxCounts.get(n.id) || 0
      return {
        key: n.id,
        title: <span style={{ fontSize: 13 }}>{n.name} <span style={{ color: 'var(--text-tertiary)', fontSize: 11 }}>({count})</span></span>,
        icon: n.children?.length ? <FolderOpenOutlined /> : <FolderOutlined />,
        children: n.children?.length ? buildTree(n.children) : undefined,
      }
    })
  }

  const doBatch = async (cmd: string) => {
    const targets = filteredBoxes.filter(b => cmd === 'on' ? b.status === '关机' : b.status === '正常')
    if (!targets.length) { message.info('没有可操作的终端'); return }
    const label = cmd === 'on' ? '开机' : cmd === 'off' ? '关机' : '重启'
    Modal.confirm({
      title: `确认${label}`, content: `将对 ${targets.length} 台终端执行${label}操作`,
      onOk: async () => { for (const b of targets) await boxApi.power(b.id, cmd); message.success(`已${label} ${targets.length} 台`); load() },
    })
  }

  const doPower = async (id: string, cmd: string) => {
    try { await boxApi.power(id, cmd); message.success('操作成功'); load() } catch { message.error('操作失败') }
  }

  const saveEdit = async () => {
    const v = await editForm.validateFields()
    const id = v.id; delete v.id
    try { await boxApi.save({ id, ...v }); message.success('保存成功'); setEditBox(null); load() }
    catch (e: any) { message.error(e.response?.data?.detail || '保存失败') }
  }

  const handleMove = async (orgId: number) => {
    if (!moveBox) return
    try { await boxApi.move(moveBox.id, orgId); message.success(`已移动`); setMoveBox(null); load() }
    catch { message.error('移动失败') }
  }

  const saveOrg = async () => {
    const v = await orgForm.validateFields()
    try { await orgApi.save({ id: v.id, name: v.name, parentid: v.parentid }); message.success('保存成功'); setEditOrg(null); loadOrgs() }
    catch { message.error('保存失败') }
  }

  // ── Table columns ──
  const columns = [
    { title: '编号', dataIndex: 'no', width: 100, sorter: (a: any, b: any) => a.no?.localeCompare(b.no) },
    { title: '名称', dataIndex: 'name', width: 140, ellipsis: true },
    { title: '状态', dataIndex: 'status', width: 80,
      render: (s: string) => <Tag color={s === '正常' ? 'success' : s === '关机' ? 'default' : 'error'}>{s}</Tag>,
      filters: [{ text: '正常', value: '正常' }, { text: '关机', value: '关机' }, { text: '断开', value: '断开' }],
      onFilter: (v: any, r: any) => r.status === v,
    },
    { title: '分组', width: 120, render: (_: any, r: any) => r.org?.name || '新设备' },
    { title: 'IP', dataIndex: 'ip', width: 130 },
    { title: '数据源', width: 140, render: (_: any, r: any) => r.datasource?.name || <Text type="secondary">-</Text> },
    { title: '模板', width: 120, render: (_: any, r: any) => r.template?.name || <Text type="secondary">-</Text> },
    { title: '音量', dataIndex: 'volume', width: 60, render: (v: number) => v != null ? `${v}/9` : '-' },
    { title: '操作', width: 180, fixed: 'right' as const,
      render: (_: any, r: any) => (
        <Space size={4}>
          <Tooltip title="屏幕预览"><Button size="small" type="link" icon={<EyeOutlined />} onClick={() => window.open(boxApi.previewUrl(r.id), `preview-${r.id}`)} /></Tooltip>
          <Tooltip title="设置"><Button size="small" type="link" icon={<SettingOutlined />} onClick={() => { editForm.setFieldsValue({ id: r.id, name: r.name, style: r.style || '', datasource: r.datasource?.id ? String(r.datasource.id) : '', template: r.template?.id ? String(r.template.id) : '', powerontime: r.powerontime || '07:30:00', powerofftime: r.powerofftime || '18:30:00', volume: r.volume ?? 9, horselamp: r.horselamp || '', title: r.title || '', rotation: r.rotation || 'auto', winname: r.winname || '' }); setEditBox(r) }} /></Tooltip>
          <Tooltip title="移动分组"><Button size="small" type="link" icon={<SwapOutlined />} onClick={() => setMoveBox(r)} /></Tooltip>
          <Tooltip title={r.status === '关机' ? '开机' : r.status === '正常' ? '关机' : '重启'}>
            <Button size="small" type="link" danger={r.status === '正常'}
              onClick={() => doPower(r.id, r.status === '关机' ? 'on' : r.status === '正常' ? 'off' : 'restart')}>
              {r.status === '关机' ? '开机' : r.status === '正常' ? '关机' : '重启'}
            </Button>
          </Tooltip>
        </Space>
      ),
    },
  ]

  const selectedOrgName = selectedOrg === null ? '全部终端' : orgList.find(o => o.id === selectedOrg)?.name || ''

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      {/* ═══ TOP: 搜索栏 + 统计 + 操作 ═══ */}
      <div style={{ flexShrink: 0, marginBottom: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <Space wrap>
            <Input prefix={<SearchOutlined />} placeholder="搜索编号/名称" value={search}
              onChange={e => setSearch(e.target.value)} onPressEnter={load} style={{ width: 200 }} allowClear />
            <Select value={selectedOrg ?? 'all'} onChange={(v: any) => setSelectedOrg(v === 'all' ? null : Number(v))}
              style={{ width: 160 }} placeholder="选择分组">
              <Select.Option value="all">全部分组</Select.Option>
              {orgList.filter(o => o.id !== -1).map(o => <Select.Option key={o.id} value={o.id}>{o.name}</Select.Option>)}
            </Select>
            <Button type="primary" icon={<SearchOutlined />} onClick={load}>查询</Button>
          </Space>
          <Space>
            <Tag>共 {stats.total} 台</Tag>
            <Tag color="success">{stats.normal} 在线</Tag>
            <Tag>{stats.off} 关机</Tag>
            <Tag color="error">{stats.disc} 离线</Tag>
          </Space>
        </div>
        <Space size={4}>
          <Button size="small" icon={<PoweroffOutlined style={{ color: '#22c55e' }} />} onClick={() => doBatch('on')}>批量开机</Button>
          <Button size="small" danger icon={<PoweroffOutlined />} onClick={() => doBatch('off')}>批量关机</Button>
          <Button size="small" icon={<ReloadOutlined />} onClick={() => doBatch('restart')}>批量重启</Button>
        </Space>
      </div>

      {/* ═══ MIDDLE: 分组树 + 表格 ═══ */}
      <div style={{ flex: 1, display: 'flex', gap: 12, overflow: 'hidden', minHeight: 0 }}>
        {/* Left: group tree */}
        <div style={{ width: 200, flexShrink: 0, overflow: 'auto', border: '1px solid var(--border)', borderRadius: 8, background: 'var(--bg-surface)' }}>
          <div style={{ padding: '8px 12px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Text strong style={{ fontSize: 12 }}>分组</Text>
            <Button size="small" type="link" icon={<PlusOutlined />} onClick={() => { orgForm.resetFields(); setEditOrg({ mode: 'add' }) }}>新增</Button>
          </div>
          <div style={{ padding: '4px 0' }}>
            <div onClick={() => setSelectedOrg(null)} style={{ padding: '6px 12px', cursor: 'pointer', background: selectedOrg === null ? 'rgba(59,130,246,0.1)' : undefined, borderRadius: 4, margin: '0 4px', fontSize: 13 }}>
              <AppstoreOutlined style={{ marginRight: 6 }} />全部 ({allBoxes.length})
            </div>
            <Tree treeData={orgTree} defaultExpandAll selectedKeys={selectedOrg !== null ? [selectedOrg] : []}
              onSelect={k => { const id = k[0]; setSelectedOrg(typeof id === 'number' ? id : null) }} blockNode
              titleRender={(node: any) => (
                <Dropdown trigger={['contextMenu']} menu={{
                  items: [
                    { key: 'add', label: '添加子分组', icon: <PlusOutlined />,
                      onClick: () => { orgForm.resetFields(); orgForm.setFieldsValue({ parentid: node.key }); setEditOrg({ mode: 'add' }) } },
                    { key: 'edit', label: '编辑', icon: <EditOutlined />,
                      onClick: () => { orgForm.setFieldsValue({ id: node.key, name: node.title?.props?.children || node.title }); setEditOrg({ mode: 'edit' }) } },
                    ...(node.key !== -1 && node.key !== 0 ? [{
                      key: 'delete', label: '删除', icon: <DeleteIcon style={{ color: '#ef4444' }} />,
                      onClick: () => Modal.confirm({ title: '确认删除？', onOk: async () => { await orgApi.remove(node.key); message.success('删除成功'); loadOrgs() } })
                    }] : []),
                  ],
                }}>
                  <span onContextMenu={e => e.preventDefault()} style={{ fontSize: 13 }}>{node.title}</span>
                </Dropdown>
              )}
            />
          </div>
        </div>

        {/* Right: table */}
        <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column', border: '1px solid var(--border)', borderRadius: 8, background: 'var(--bg-surface)' }}>
          <Table
            columns={columns}
            dataSource={pagedBoxes}
            rowKey="id"
            loading={loading}
            size="small"
            pagination={false}
            scroll={{ y: 'calc(100vh - 310px)' }}
            rowClassName={(r: any) => selectedRow?.id === r.id ? 'ant-table-row-selected' : ''}
            onRow={(r) => ({ onClick: () => setSelectedRow(r), style: { cursor: 'pointer' } })}
          />
          {/* Bottom: pagination */}
          <div style={{ flexShrink: 0, padding: '8px 16px', borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'flex-end' }}>
            <Pagination current={page} pageSize={pageSize} total={filteredBoxes.length}
              showSizeChanger showTotal={t => `共 ${t} 条`}
              pageSizeOptions={['10', '20', '50', '100']}
              size="small"
              onChange={(p, ps) => { setPage(p); setPageSize(ps) }} />
          </div>
        </div>
      </div>

      {/* ═══ Modals ═══ */}
      {/* Edit box */}
      <Modal title={`设置: ${editBox?.no || ''}`} open={!!editBox} onOk={saveEdit} onCancel={() => setEditBox(null)} width={600}>
        <Form form={editForm} layout="vertical" size="small">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Row gutter={12}>
            <Col span={12}><Form.Item name="name" label="终端名称"><Input /></Form.Item></Col>
            <Col span={12}><Form.Item name="winname" label="窗口名"><Input /></Form.Item></Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}><Form.Item name="style" label="显示样式" tooltip="控制终端的显示布局"><Select options={styles} allowClear /></Form.Item></Col>
            <Col span={12}><Form.Item name="template" label="显示模板" tooltip="控制终端的具体显示内容"><Select options={templates} allowClear showSearch optionFilterProp="label" /></Form.Item></Col>
            <Col span={12}><Form.Item name="datasource" label="数据源" tooltip="控制终端从哪个数据源获取叫号数据"><Select options={dss} allowClear showSearch optionFilterProp="label" /></Form.Item></Col>
            <Col span={12}><Form.Item name="rotation" label="屏幕方向"><Select options={[{ value: 'auto', label: '自动' }, { value: '0', label: '横屏' }, { value: '180', label: '反向横屏' }, { value: '270', label: '竖屏' }, { value: '90', label: '反向竖屏' }]} /></Form.Item></Col>
          </Row>
          <Row gutter={12}>
            <Col span={8}><Form.Item name="powerontime" label="开机时间"><Input prefix={<ClockCircleOutlined />} placeholder="07:30:00" /></Form.Item></Col>
            <Col span={8}><Form.Item name="powerofftime" label="关机时间"><Input prefix={<ClockCircleOutlined />} placeholder="18:30:00" /></Form.Item></Col>
            <Col span={8}><Form.Item name="volume" label="音量 (0-9)"><Input type="number" min={0} max={9} style={{ width: '100%' }} /></Form.Item></Col>
          </Row>
          <Form.Item name="title" label="标题" tooltip="终端顶部大标题"><Input prefix={<PictureOutlined />} /></Form.Item>
          <Form.Item name="horselamp" label="走马灯" tooltip="底部滚动提示文字"><Input prefix={<BulbOutlined />} /></Form.Item>
        </Form>
      </Modal>

      {/* Move */}
      <Modal title={`移动 ${moveBox?.no} 到...`} open={!!moveBox} onCancel={() => setMoveBox(null)} footer={null} width={400}>
        <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>当前: {moveBox?.org?.name || '未分组'}</Text>
        <div style={{ maxHeight: 400, overflow: 'auto' }}>
          {orgList.filter(o => o.id !== -1).map(o => (
            <div key={o.id} onClick={() => handleMove(o.id)} className="terminal-card"
              style={{ height: 'auto', padding: '10px 14px', marginBottom: 6, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Space><FolderOutlined style={{ color: '#f59e0b' }} /><Text>{o.name}</Text>
                {moveBox?.org?.id === o.id && <Tag color="blue">当前</Tag>}</Space>
            </div>
          ))}
        </div>
      </Modal>

      {/* Edit org */}
      <Modal title={editOrg?.mode === 'add' ? '添加分组' : '编辑分组'} open={!!editOrg} onOk={saveOrg} onCancel={() => setEditOrg(null)} width={400}>
        <Form form={orgForm} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="parentid" hidden><Input /></Form.Item>
          <Form.Item name="name" label="分组名称" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
