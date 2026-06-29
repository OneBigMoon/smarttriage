import { useState, useEffect, useRef } from 'react'
import { Card, Row, Col, Tag, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Empty, Segmented, Typography } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined, CopyOutlined } from '@ant-design/icons'
import api from '../../services/api'

interface Template { id: number; name: string; key: string; kind: string; html: string; css: string; js: string; logo: string | null; version: string }

const kindColors: Record<string, string> = { native: 'blue', web: 'green' }
const kindLabels: Record<string, string> = { native: '原生', web: 'Web模板' }

// Pre-built template snippets for non-technical users
const SNIPPETS = [
  { label: '排队叫号表格', html: '<div class="queue">\n  <div class="header">排队信息</div>\n  {{#each queues}}\n  <div class="item">\n    <span class="callno">{{callno}}</span>\n    <span class="name">{{patientname}}</span>\n    <span class="dept">{{officename}}</span>\n  </div>\n  {{/each}}\n</div>' },
  { label: '单个叫号大字', html: '<div class="called">\n  <div class="ticket">{{callno}}</div>\n  <div class="name">{{patientname}}</div>\n  <div class="dept">{{officename}} {{doctorname}}</div>\n</div>' },
  { label: '医生信息卡片', html: '<div class="doctor-card">\n  <div class="photo"><img src="{{doctorphoto}}" /></div>\n  <div class="info">\n    <div class="name">{{doctorname}}</div>\n    <div class="title">{{doctortitle}}</div>\n    <div class="intro">{{doctorintro}}</div>\n  </div>\n</div>' },
]

const PRESET_CSS = `* { margin: 0; padding: 0; box-sizing: border-box; }
body { background: #0a0a1a; color: #fff; font-family: -apple-system, sans-serif; min-height: 100vh; }
.queue .header { background: #1a1a2e; padding: 16px 24px; font-size: 24px; color: #ffd700; }
.queue .item { display: flex; justify-content: space-between; padding: 14px 24px; border-bottom: 1px solid #222; }
.queue .item:hover { background: #1a1a2e; }
.callno { color: #ffd700; font-size: 28px; font-weight: bold; min-width: 80px; }
.name { font-size: 20px; }
.dept { color: #aaa; font-size: 16px; }
.called { text-align: center; padding: 60px; }
.called .ticket { font-size: 120px; color: #ffd700; font-weight: bold; }
.called .name { font-size: 48px; margin-top: 16px; }
.called .dept { font-size: 24px; color: #aaa; margin-top: 12px; }`

export default function TemplatePage() {
  const [list, setList] = useState<Template[]>([])
  const [sel, setSel] = useState<Template | null>(null)
  const [editOpen, setEditOpen] = useState(false)
  const [previewOpen, setPreviewOpen] = useState(false)
  const [form] = Form.useForm()
  const [viewMode, setViewMode] = useState<'card' | 'code'>('card')
  const previewRef = useRef<HTMLIFrameElement>(null)

  const query = async () => {
    try { const { data } = await api.post('/api/v1/templates/query'); setList(data.result || []) } catch {}
  }
  useEffect(() => { query() }, [])

  const doEdit = (t?: Template | null) => {
    if (t) {
      form.setFieldsValue({ id: t.id, name: t.name, key: t.key, kind: t.kind, html: t.html, css: t.css, js: t.js, logo: t.logo || '', version: t.version })
    } else {
      form.resetFields()
      form.setFieldsValue({ kind: 'web', version: '1.0.0', css: PRESET_CSS })
    }
    setEditOpen(true)
  }

  const doSave = async () => {
    const v = await form.validateFields()
    try { await api.post('/api/v1/templates/save', v); message.success('保存成功'); setEditOpen(false); query() }
    catch (e: any) { message.error(e.response?.data?.detail || '保存失败') }
  }

  const doRemove = async (id: number) => {
    try { await api.post('/api/v1/templates/remove', null, { params: { id } }); message.success('删除成功'); setSel(null); query() }
    catch { message.error('删除失败') }
  }

  const insertSnippet = (html: string) => {
    const cur = form.getFieldValue('html') || ''
    form.setFieldsValue({ html: cur + '\n' + html })
  }

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Space>
          <Segmented options={[{ value: 'card', label: '卡片视图' }, { value: 'code', label: '列表视图' }]} value={viewMode} onChange={v => setViewMode(v as any)} />
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => doEdit()}>新建模板</Button>
      </div>

      {viewMode === 'card' ? (
        <Row gutter={[16, 16]}>
          {list.length === 0 && <Col span={24}><Empty description="暂无模板" /></Col>}
          {list.map(t => (
            <Col xs={24} sm={12} md={8} key={t.id}>
              <Card
                hoverable
                onClick={() => { setSel(t); setPreviewOpen(true) }}
                style={{ height: 220, borderTop: `3px solid ${kindColors[t.kind] === 'green' ? '#52c41a' : '#1890ff'}` }}
                actions={[
                  <Button type="link" size="small" icon={<EyeOutlined />} onClick={(e) => { e.stopPropagation(); setSel(t); setPreviewOpen(true) }}>预览</Button>,
                  <Button type="link" size="small" icon={<EditOutlined />} onClick={(e) => { e.stopPropagation(); doEdit(t) }}>编辑</Button>,
                  <Popconfirm title="确认删除？" onConfirm={() => doRemove(t.id)} onCancel={e => e?.stopPropagation()}>
                    <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={e => e.stopPropagation()}>删除</Button>
                  </Popconfirm>,
                ]}
              >
                <Card.Meta
                  title={<Space>{t.name} <Tag color={kindColors[t.kind]}>{kindLabels[t.kind]}</Tag></Space>}
                  description={
                    <div>
                      <div>Key: <code>{t.key}</code></div>
                      <div>版本: {t.version}</div>
                      <div style={{ marginTop: 8 }}>
                        <div style={{ background: '#f5f5f5', borderRadius: 4, padding: 8, height: 80, overflow: 'hidden', fontSize: 11, color: '#666', fontFamily: 'monospace' }}>
                          {t.html ? t.html.substring(0, 120) + '...' : '(空模板)'}
                        </div>
                      </div>
                    </div>
                  }
                />
              </Card>
            </Col>
          ))}
        </Row>
      ) : (
        <Card>
          {list.map(t => (
            <div key={t.id} style={{ display: 'flex', alignItems: 'center', padding: '12px 0', borderBottom: '1px solid #f0f0f0', gap: 16 }}>
              <Tag color={kindColors[t.kind]} style={{ minWidth: 60, textAlign: 'center' }}>{kindLabels[t.kind]}</Tag>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600 }}>{t.name}</div>
                <div style={{ color: '#999', fontSize: 12 }}>Key: {t.key} | v{t.version}</div>
              </div>
              <Space>
                <Button size="small" icon={<EyeOutlined />} onClick={() => { setSel(t); setPreviewOpen(true) }}>预览</Button>
                <Button size="small" icon={<EditOutlined />} onClick={() => doEdit(t)}>编辑</Button>
                <Popconfirm title="确认删除？" onConfirm={() => doRemove(t.id)}>
                  <Button size="small" danger icon={<DeleteOutlined />}>删除</Button>
                </Popconfirm>
              </Space>
            </div>
          ))}
        </Card>
      )}

      {/* Preview modal */}
      <Modal title={`预览: ${sel?.name}`} open={previewOpen} onCancel={() => setPreviewOpen(false)}
        footer={null} width={800} destroyOnClose>
        {sel?.kind === 'web' ? (
          <iframe ref={previewRef} src={`/api/v1/templates/serve/${sel.id}`}
            style={{ width: '100%', height: 500, border: '1px solid #ddd', borderRadius: 4 }} title="preview" />
        ) : (
          <div style={{ padding: 40, textAlign: 'center', color: '#999' }}>原生模板无法在此预览</div>
        )}
      </Modal>

      {/* Edit modal */}
      <Modal title="模板编辑" open={editOpen} onOk={doSave} onCancel={() => setEditOpen(false)}
        width={900} okText="保存" cancelText="取消" destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Row gutter={16}>
            <Col span={8}><Form.Item name="name" label="模板名称" rules={[{ required: true }]}><Input /></Form.Item></Col>
            <Col span={8}><Form.Item name="key" label="唯一Key" rules={[{ required: true }]}><Input /></Form.Item></Col>
            <Col span={8}><Form.Item name="kind" label="类型"><Select options={[{ value: 'web', label: 'Web模板' }, { value: 'native', label: '原生模板' }]} /></Form.Item></Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="version" label="版本"><Input /></Form.Item></Col>
            <Col span={12}><Form.Item name="logo" label="Logo URL"><Input /></Form.Item></Col>
          </Row>

          <div style={{ marginBottom: 8 }}>
            <Space>
              <Typography.Text strong>代码片段:</Typography.Text>
              {SNIPPETS.map((s, i) => (
                <Button key={i} size="small" icon={<CopyOutlined />} onClick={() => insertSnippet(s.html)}>{s.label}</Button>
              ))}
            </Space>
          </div>

          <Form.Item name="html" label="HTML" rules={[{ required: true }]}>
            <Input.TextArea rows={10} style={{ fontFamily: 'monospace', fontSize: 13 }}
              placeholder="HTML template with {{variable}} placeholders" />
          </Form.Item>
          <Form.Item name="css" label="CSS">
            <Input.TextArea rows={5} style={{ fontFamily: 'monospace', fontSize: 13 }} />
          </Form.Item>
          <Form.Item name="js" label="JavaScript (可选)">
            <Input.TextArea rows={4} style={{ fontFamily: 'monospace', fontSize: 13 }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
