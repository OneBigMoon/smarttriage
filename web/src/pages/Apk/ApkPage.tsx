import { useState, useEffect, useRef } from 'react'
import { Table, Button, Space, Upload, Modal, Form, Input, message, Popconfirm, Tag, Typography } from 'antd'
import { UploadOutlined, QrcodeOutlined, DownloadOutlined, DeleteOutlined } from '@ant-design/icons'
import api from '../../services/api'

interface ApkEntry {
  id: string; filename: string; original: string; version: string
  flavor: string; notes: string; size: number; uploaded_at: string
}

const flavorMap: Record<string, string> = {
  default: '默认', qzfe: '泉州附二', fzpfy: '福州皮防',
}

export default function ApkPage() {
  const [list, setList] = useState<ApkEntry[]>([])
  const [loading, setLoading] = useState(false)
  const [sel, setSel] = useState<ApkEntry | null>(null)
  const [qrOpen, setQrOpen] = useState(false)
  const [qrConfigs, setQrConfigs] = useState<any[]>([])
  const [qrForm] = Form.useForm()
  const canvasRef = useRef<HTMLCanvasElement>(null)

  const query = async () => {
    setLoading(true)
    try { const { data } = await api.post('/api/v1/apk/list'); setList(data.result || []) }
    catch { message.error('查询失败') }
    finally { setLoading(false) }
  }

  useEffect(() => { query() }, [])

  const handleUpload = async (info: any) => {
    if (info.file.status === 'done') {
      message.success('上传成功')
      query()
    } else if (info.file.status === 'error') {
      message.error('上传失败')
    }
  }

  const doRemove = async () => {
    if (!sel) return
    try { await api.post('/api/v1/apk/remove', null, { params: { id: sel.id } }); message.success('删除成功'); setSel(null); query() }
    catch { message.error('删除失败') }
  }

  const doDownload = (entry: ApkEntry) => {
    window.open(`/api/v1/apk/download/${entry.id}`, '_blank')
  }

  const doBatchQR = async () => {
    const v = await qrForm.validateFields()
    try {
      const { data } = await api.post('/api/v1/apk/batch-qr', v)
      setQrConfigs(data.result || [])
      setQrOpen(true)
      // Generate QR codes after modal opens
      setTimeout(() => generateQRCodes(data.result || []), 100)
    } catch { message.error('生成失败') }
  }

  const generateQRCodes = async (configs: any[]) => {
    // Use a simple QR code library or API
    // For now, we'll use a canvas-based approach with qrcode.js (loaded via CDN)
    for (let i = 0; i < configs.length; i++) {
      const container = document.getElementById(`qr-${i}`)
      if (!container) continue
      container.innerHTML = '' // Clear

      const canvas = document.createElement('canvas')
      const text = JSON.stringify(configs[i])
      // Simple QR code generation using API
      try {
        const img = document.createElement('img')
        img.src = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(text)}`
        img.style.width = '200px'
        img.style.height = '200px'
        img.alt = configs[i].no || `QR ${i + 1}`
        container.appendChild(img)

        const label = document.createElement('div')
        label.textContent = configs[i].no || `Config ${i + 1}`
        label.style.textAlign = 'center'
        label.style.marginTop = '4px'
        label.style.fontWeight = 'bold'
        container.appendChild(label)
      } catch {
        container.textContent = text
      }
    }
  }

  const cols = [
    { title: '#', width: 50, render: (_: any, __: any, i: number) => i + 1 },
    { title: '文件名', dataIndex: 'original' },
    { title: '版本', dataIndex: 'version' },
    { title: 'Flavor', dataIndex: 'flavor', render: (f: string) => <Tag>{flavorMap[f] || f}</Tag> },
    { title: '大小', dataIndex: 'size', render: (s: number) => `${(s / 1024 / 1024).toFixed(1)} MB` },
    { title: '备注', dataIndex: 'notes' },
    { title: '上传时间', dataIndex: 'uploaded_at', render: (t: string) => t ? new Date(t).toLocaleString() : '' },
    { title: '操作', render: (_: any, r: ApkEntry) => (
      <Space>
        <Button size="small" icon={<DownloadOutlined />} onClick={() => doDownload(r)}>下载</Button>
      </Space>
    )},
  ]

  return (
    <div>
      <Space style={{ marginBottom: 16 }} wrap>
        <Upload
          action="/api/v1/apk/upload"
          accept=".apk"
          showUploadList={false}
          onChange={handleUpload}
          data={(file) => ({ version: '3.0.0', notes: file.name, flavor: 'default' })}
        >
          <Button type="primary" icon={<UploadOutlined />}>上传APK</Button>
        </Upload>
        <Popconfirm title="确认删除？" onConfirm={doRemove} disabled={!sel}>
          <Button disabled={!sel} danger icon={<DeleteOutlined />}>删除</Button>
        </Popconfirm>
        <Button icon={<QrcodeOutlined />} onClick={() => { qrForm.resetFields(); setQrOpen(true) }}>
          批量生成配网QR码
        </Button>
      </Space>

      <Table columns={cols} dataSource={list} rowKey="id" loading={loading} size="small" pagination={false}
        onRow={(r) => ({ onClick: () => setSel(r), style: { cursor: 'pointer', background: sel?.id === r.id ? '#e6f7ff' : undefined } })} />

      {/* QR Code Modal */}
      <Modal title="批量配网QR码" open={qrOpen} onCancel={() => setQrOpen(false)} width={900}
        footer={null} destroyOnClose>
        <Form form={qrForm} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item name="server_ip" label="服务器IP" rules={[{ required: true }]}>
            <Input placeholder="如：192.168.1.100" />
          </Form.Item>
          <Form.Item name="server_port" label="端口" initialValue="7016">
            <Input placeholder="7016" style={{ width: 80 }} />
          </Form.Item>
          <Form.Item name="count" label="数量" initialValue={10}>
            <Input type="number" style={{ width: 60 }} />
          </Form.Item>
          <Form.Item name="prefix" label="编号前缀" initialValue="BOX">
            <Input style={{ width: 80 }} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={doBatchQR}>生成</Button>
          </Form.Item>
        </Form>

        {qrConfigs.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16, maxHeight: 500, overflow: 'auto' }}>
            {qrConfigs.map((cfg, i) => (
              <div key={i} id={`qr-${i}`} style={{ textAlign: 'center', border: '1px solid #ddd', padding: 8, borderRadius: 4 }}>
              </div>
            ))}
          </div>
        )}

        <Typography.Paragraph type="secondary" style={{ marginTop: 16 }}>
          扫描QR码后，Android端将自动配置服务器地址并连接。每个QR码对应一个终端编号。
        </Typography.Paragraph>
      </Modal>
    </div>
  )
}
