import { useState, useEffect } from 'react'
import {
  Card, Row, Col, Tag, Button, Space, Modal, Form, Input, Select,
  message, Popconfirm, Typography, Tabs, Divider, Alert, Table, Empty, Spin,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, DatabaseOutlined, ExperimentOutlined, MedicineBoxOutlined, DesktopOutlined, ReloadOutlined } from '@ant-design/icons'
import { dsApi, discoveryApi } from '../../services/api'

const { Text, Title } = Typography

const CATEGORIES = [
  { key: 'triage', label: '分诊数据', icon: <DesktopOutlined />, desc: '科室大屏、诊室叫号屏的数据来源',
    types: ['primarytriage', 'leveldepart', 'secondarytriage', 'secondarytriagesplit', 'secondarytriageultrasonic'] },
  { key: 'lab', label: '检验数据', icon: <ExperimentOutlined />, desc: '抽血、检验窗口的叫号数据来源',
    types: ['drawbloodtriage'] },
  { key: 'pharmacy', label: '药房数据', icon: <MedicineBoxOutlined />, desc: '药房发药和取药的叫号数据来源',
    types: ['primarypharmacytriage', 'secondarypharmacytriage'] },
]

const TYPE_LABELS: Record<string, string> = {
  primarytriage: '一级分诊', leveldepart: '层级科室', secondarytriage: '二级分诊',
  secondarytriagesplit: '二级分屏', secondarytriageultrasonic: '超声分诊',
  drawbloodtriage: '检验窗口', primarypharmacytriage: '药房一级(发药)', secondarypharmacytriage: '药房二级(取药)',
}

export default function DataSourcePage() {
  const [list, setList] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [editCategory, setEditCategory] = useState('')
  const [form] = Form.useForm()
  const curType = Form.useWatch('type', form)

  // Oracle discovered data
  const [departments, setDepartments] = useState<any[]>([])
  const [screens, setScreens] = useState<any[]>([])
  const [queues, setQueues] = useState<any[]>([])
  const [windows, setWindows] = useState<any[]>([])
  const [pharmacyDepts, setPharmacyDepts] = useState<any[]>([])
  const [pharmacyWindows, setPharmacyWindows] = useState<any[]>([])
  const [discovering, setDiscovering] = useState(false)
  const curPharmacyDept = Form.useWatch('pharmacydeptno', form)

  const query = async () => {
    setLoading(true)
    try {
      const { data } = await dsApi.query({})
      setList(((data.result as any[]) || []))
    } catch { message.error('查询失败') }
    finally { setLoading(false) }
  }

  const discoverAll = async () => {
    setDiscovering(true)
    try {
      const [d, s, q, w, pd] = await Promise.all([
        discoveryApi.departments(), discoveryApi.screens(),
        discoveryApi.queues(), discoveryApi.windows(), discoveryApi.pharmacyDepts(),
      ])
      setDepartments(((d.data.result as any[]) || []))
      setScreens(((s.data.result as any[]) || []))
      setQueues(((q.data.result as any[]) || []))
      setWindows(((w.data.result as any[]) || []))
      setPharmacyDepts(((pd.data.result as any[]) || []))
      message.success(`发现 ${departments.length} 个科室, ${screens.length} 个屏幕, ${queues.length} 个队列`)
    } catch { message.error('发现数据失败，请检查Oracle连接') }
    finally { setDiscovering(false) }
  }

  // When pharmacy dept changes, load its windows
  useEffect(() => {
    if (curPharmacyDept) {
      discoveryApi.pharmacyWindows(curPharmacyDept).then(r => {
        setPharmacyWindows(((r.data.result as any[]) || []))
      })
    }
  }, [curPharmacyDept])

  useEffect(() => { query() }, [])

  const doEdit = (ds?: any, category?: string) => {
    if (ds) {
      form.setFieldsValue({
        _id: ds._id, name: ds.name, type: ds.type,
        departmentid: ds.departmentid, screenid: ds.screenid,
        screensplitid: ds.screensplitid, queue: ds.queue,
        consultingroomname: ds.consultingroomname,
        windowid: ds.windowid, pharmacydeptno: ds.pharmacydeptno,
        pharmacywinno: ds.pharmacywinno,
        morningcleartime: ds.morningcleartime, afternooncleartime: ds.afternooncleartime,
      })
      setEditCategory(CATEGORIES.find(c => c.types.includes(ds.type))?.key || '')
    } else {
      form.resetFields()
      setEditCategory(category || '')
    }
    setEditOpen(true)
  }

  const doSave = async () => {
    const v = await form.validateFields()
    try { await dsApi.save(v); message.success('保存成功'); setEditOpen(false); query() }
    catch (e: any) { message.error(e.response?.data?.detail || '保存失败') }
  }

  const doRemove = async (id: string) => {
    try { await dsApi.remove(id); message.success('删除成功'); query() }
    catch { message.error('删除失败') }
  }

  const renderCategory = (cat: typeof CATEGORIES[0]) => {
    const items = list.filter(ds => cat.types.includes(ds.type))
    const columns = [
      { title: '#', width: 50, render: (_: any, __: any, i: number) => i + 1 },
      { title: '名称', dataIndex: 'name' },
      { title: '类型', render: (_: any, r: any) => <Tag>{TYPE_LABELS[r.type] || r.type}</Tag> },
      { title: '配置项', render: (_: any, r: any) => {
        const parts: string[] = []
        if (r.departmentid) parts.push(`科室: ${r.departmentid}`)
        if (r.screenid) parts.push(`屏幕: ${r.screenid}`)
        if (r.queue) parts.push(`队列: ${r.queue}`)
        if (r.windowid) parts.push(`窗口: ${r.windowid}`)
        if (r.pharmacy) parts.push(`药房: ${r.pharmacy}`)
        return <Text type="secondary" style={{ fontSize: 12 }}>{parts.join(' · ') || '-'}</Text>
      }},
      { title: '操作', width: 100, render: (_: any, r: any) => (
        <Space size={4}>
          <Button size="small" type="link" icon={<EditOutlined />} onClick={() => doEdit(r)}>编辑</Button>
          <Popconfirm title="确认删除？" onConfirm={() => doRemove(r._id)}>
            <Button size="small" type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )},
    ]
    return (
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <Text style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{cat.desc}</Text>
          <Button size="small" type="primary" icon={<PlusOutlined />}
            onClick={() => doEdit(undefined, cat.key)}>新增</Button>
        </div>
        {items.length === 0 ? <Empty description="暂无配置" image={Empty.PRESENTED_IMAGE_SIMPLE} /> :
          <Table dataSource={items} columns={columns} rowKey="_id" size="small" pagination={false} />}
      </div>
    )
  }

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <div style={{ flexShrink: 0, marginBottom: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={4} style={{ margin: 0, color: 'var(--text-primary)' }}>数据源管理</Title>
            <Text style={{ color: 'var(--text-tertiary)', fontSize: 12 }}>从Oracle自动发现可用数据，选择配置即可</Text>
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={discoverAll} loading={discovering}>从Oracle发现数据</Button>
          </Space>
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto' }}>
        <Tabs defaultActiveKey="triage" items={CATEGORIES.map(cat => ({
          key: cat.key,
          label: <Space>{cat.icon}{cat.label}</Space>,
          children: renderCategory(cat),
        }))} />
      </div>

      {/* Edit modal — 使用 Oracle 发现的下拉列表 */}
      <Modal title="编辑数据源" open={editOpen} onOk={doSave} onCancel={() => setEditOpen(false)} width={600} okText="保存">
        <Form form={form} layout="vertical">
          <Form.Item name="_id" hidden><Input /></Form.Item>
          <Form.Item name="type" label="数据源类型" rules={[{ required: true }]}>
            <Select options={CATEGORIES.flatMap(c => c.types.map(t => ({ value: t, label: TYPE_LABELS[t] || t })))} />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true }]} tooltip="给数据源起个好记的名字">
            <Input placeholder="如：3楼内一科大屏" />
          </Form.Item>

          {/* 分诊: 科室选择（从Oracle发现） */}
          {(curType === 'primarytriage' || curType === 'leveldepart') && (
            <Form.Item name="departmentid" label="选择科室" rules={[{ required: true }]}
              tooltip="从Oracle自动发现的科室列表中选择">
              {departments.length > 0 ? (
                <Select mode="multiple" placeholder="选择科室（可多选）" showSearch
                  optionFilterProp="label" maxTagCount={3}>
                  {departments.map(d => <Select.Option key={d.id} value={String(d.id)} label={`${d.id} - ${d.name}`}>{d.id} - {d.name}</Select.Option>)}
                </Select>
              ) : (
                <div>
                  <Input placeholder="未发现数据，请先配置Oracle连接" disabled />
                  <Button size="small" type="link" onClick={discoverAll} loading={discovering}>重新发现</Button>
                </div>
              )}
            </Form.Item>
          )}

          {/* 二级分诊: 屏幕选择 */}
          {curType === 'secondarytriage' && (
            <Form.Item name="screenid" label="选择屏幕" rules={[{ required: true }]}
              tooltip="从Oracle自动发现的分诊屏列表中选择">
              {screens.length > 0 ? (
                <Select placeholder="选择屏幕" showSearch optionFilterProp="label">
                  {screens.map(s => <Select.Option key={s.id} value={String(s.id)} label={`${s.id} - ${s.name}`}>{s.id} - {s.name}</Select.Option>)}
                </Select>
              ) : (
                <div>
                  <Input placeholder="未发现数据" disabled />
                  <Button size="small" type="link" onClick={discoverAll} loading={discovering}>重新发现</Button>
                </div>
              )}
            </Form.Item>
          )}

          {/* 超声: 队列选择 */}
          {curType === 'secondarytriageultrasonic' && (
            <Form.Item name="queue" label="选择队列" rules={[{ required: true }]}
              tooltip="从PACS系统自动发现的超声队列">
              {queues.length > 0 ? (
                <Select mode="multiple" placeholder="选择队列（可多选）" showSearch optionFilterProp="label">
                  {queues.map(q => <Select.Option key={q.name} value={q.name} label={q.name}>{q.name}</Select.Option>)}
                </Select>
              ) : (
                <div>
                  <Input placeholder="未发现数据" disabled />
                  <Button size="small" type="link" onClick={discoverAll} loading={discovering}>重新发现</Button>
                </div>
              )}
            </Form.Item>
          )}

          {/* 检验: 窗口选择 */}
          {curType === 'drawbloodtriage' && (
            <Form.Item name="windowid" label="选择窗口" rules={[{ required: true }]}
              tooltip="从Oracle自动发现的检验窗口">
              {windows.length > 0 ? (
                <Select mode="multiple" placeholder="选择窗口（可多选）" showSearch optionFilterProp="label">
                  {windows.map(w => <Select.Option key={w.id} value={String(w.id)} label={`${w.id} - ${w.name}`}>{w.id} - {w.name}</Select.Option>)}
                </Select>
              ) : (
                <div>
                  <Input placeholder="未发现数据" disabled />
                  <Button size="small" type="link" onClick={discoverAll} loading={discovering}>重新发现</Button>
                </div>
              )}
            </Form.Item>
          )}

          {/* 药房: 部门 + 窗口 */}
          {(curType === 'primarypharmacytriage' || curType === 'secondarypharmacytriage') && <>
            <Form.Item name="pharmacydeptno" label="选择药房部门" rules={[{ required: true }]}
              tooltip="从Oracle自动发现的药房部门">
              {pharmacyDepts.length > 0 ? (
                <Select placeholder="选择药房部门" showSearch optionFilterProp="label">
                  {pharmacyDepts.map(d => <Select.Option key={d.id} value={d.id} label={`部门 ${d.id}`}>部门 {d.id}</Select.Option>)}
                </Select>
              ) : (
                <div>
                  <Input placeholder="未发现数据" disabled />
                  <Button size="small" type="link" onClick={discoverAll} loading={discovering}>重新发现</Button>
                </div>
              )}
            </Form.Item>
            {curPharmacyDept && (
              <Form.Item name="pharmacywinno" label="选择窗口" rules={[{ required: true }]}
                tooltip="从Oracle自动发现的发药窗口">
                {pharmacyWindows.length > 0 ? (
                  <Select mode="multiple" placeholder="选择窗口（可多选）" showSearch optionFilterProp="label">
                    {pharmacyWindows.map(w => <Select.Option key={w.id} value={String(w.id)} label={`窗口 ${w.id}`}>窗口 {w.id}</Select.Option>)}
                  </Select>
                ) : (
                  <Input placeholder="该部门暂无窗口数据" disabled />
                )}
              </Form.Item>
            )}
          </>}

          <Divider orientation="left" plain>自动清屏（可选）</Divider>
          <Row gutter={12}>
            <Col span={12}><Form.Item name="morningcleartime" label="上午下班清屏"><Input placeholder="HH:MM:SS" /></Form.Item></Col>
            <Col span={12}><Form.Item name="afternooncleartime" label="下午下班清屏"><Input placeholder="HH:MM:SS" /></Form.Item></Col>
          </Row>
        </Form>
      </Modal>
    </div>
  )
}
