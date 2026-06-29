import { useState, useEffect } from 'react'
import {
  Card, Table, Button, Space, Upload, Modal, Form, Input, message,
  Popconfirm, Typography, Tag, Progress, Divider, Alert,
} from 'antd'
import { UploadOutlined, DeleteOutlined, CloudDownloadOutlined, PlusOutlined } from '@ant-design/icons'
import { upApi } from '../../services/api'
import api from '../../services/api'

const { Text, Title } = Typography

export default function UpgradePage() {
  const [list, setList] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [sel, setSel] = useState<any>(null)
  const [uploadOpen, setUploadOpen] = useState(false)
  const [uploadForm] = Form.useForm()
  const [uploading, setUploading] = useState(false)

  const query = async () => {
    setLoading(true)
    try { const { data } = await upApi.query(); setList(((data.result as any[]) || [])) }
    catch { message.error('查询失败') }
    finally { setLoading(false) }
  }
  useEffect(() => { query() }, [])

  const doRemove = async () => {
    if (!sel) return
    try { await upApi.remove(sel._id); message.success('删除成功'); setSel(null); query() }
    catch { message.error('删除失败') }
  }

  const doUpload = async () => {
    const v = await uploadForm.validateFields()
    const formData = new FormData()
    formData.append('type', 'upgrade')
    formData.append('model', v.model)
    formData.append('appVersion', v.appVersion)

    setUploading(true)
    try {
      // Get the file from the form
      const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
      if (fileInput?.files?.[0]) {
        formData.append('file', fileInput.files[0])
        await api.post('/api/v1/upload?type=upgrade', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        message.success('上传成功')
        setUploadOpen(false)
        uploadForm.resetFields()
        query()
      } else {
        message.error('请选择文件')
      }
    } catch { message.error('上传失败') }
    finally { setUploading(false) }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0, color: '#fff' }}>升级包管理</Title>
          <Text style={{ color: 'rgba(255,255,255,0.35)', fontSize: 12 }}>
            上传APK后，终端会自动检查并下载更新
          </Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { uploadForm.resetFields(); setUploadOpen(true) }}>
          上传升级包
        </Button>
      </div>

      <Card bordered={false}>
        <Table
          dataSource={list} rowKey="_id" loading={loading} size="small" pagination={false}
          onRow={(r) => ({ onClick: () => setSel(r), style: { cursor: 'pointer', background: sel?._id === r._id ? 'rgba(24,144,255,0.1)' : undefined } })}
          columns={[
            { title: '#', width: 50, render: (_: any, __: any, i: number) => i + 1 },
            { title: '设备型号', dataIndex: 'model', render: (v: string) => <Tag>{v}</Tag> },
            { title: '版本号', dataIndex: 'appVersion' },
            { title: '文件名', dataIndex: 'originname' },
            { title: '上传时间', dataIndex: 'ut', render: (v: string) => v ? new Date(v).toLocaleString() : '-' },
          ]}
        />
      </Card>

      {/* Upload modal */}
      <Modal title="上传升级包" open={uploadOpen} onOk={doUpload} onCancel={() => setUploadOpen(false)}
        confirmLoading={uploading} okText="上传" cancelText="取消" width={500}>
        <Alert message="上传后终端会自动检查并下载更新" type="info" showIcon style={{ marginBottom: 16 }} />
        <Form form={uploadForm} layout="vertical">
          <Form.Item label="APK 文件" required>
            <input type="file" accept=".apk" style={{ width: '100%' }} />
            <Text type="secondary" style={{ fontSize: 12 }}>仅支持 .apk 格式</Text>
          </Form.Item>
          <Form.Item name="model" label="设备型号" rules={[{ required: true }]}
            tooltip="该升级包适用的设备型号，需与终端配置的model一致">
            <Input placeholder="如：qzfe3128" />
          </Form.Item>
          <Form.Item name="appVersion" label="版本号" rules={[{ required: true }]}
            tooltip="新版本号，格式：主版本.次版本.修订号（如 2.4.20）">
            <Input placeholder="如：2.4.20" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
