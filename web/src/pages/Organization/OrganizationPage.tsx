import { useState, useEffect } from 'react'
import { Tree, Button, Space, Modal, Form, Input, message, Popconfirm } from 'antd'
import type { DataNode } from 'antd/es/tree'
import { orgApi } from '../../services/api'

function toTree(data: any[]): DataNode[] {
  return data.map((n) => ({ key: n._id, title: n.name, children: n.children?.length ? toTree(n.children) : undefined }))
}
function findNode(data: any[], id: number): any {
  for (const n of data) { if (n._id === id) return n; if (n.children) { const f = findNode(n.children, id); if (f) return f } }
  return null
}

export default function OrganizationPage() {
  const [raw, setRaw] = useState<any[]>([])
  const [tree, setTree] = useState<DataNode[]>([])
  const [selKey, setSelKey] = useState<number | null>(null)
  const [editOpen, setEditOpen] = useState(false)
  const [form] = Form.useForm()

  const query = async () => {
    const r = await orgApi.query()
    const d = (r.data.result as any[]) || []; setRaw(d); setTree(toTree(d))
  }
  useEffect(() => { query() }, [])

  const doAdd = (parentId?: number) => {
    form.resetFields()
    if (parentId !== undefined) form.setFieldsValue({ parentid: parentId })
    setEditOpen(true)
  }
  const doEdit = () => {
    if (selKey === null) return
    const n = findNode(raw, selKey)
    if (n) { form.setFieldsValue({ id: n._id, name: n.name }); setEditOpen(true) }
  }
  const doSave = async () => {
    const v = await form.validateFields()
    await orgApi.save(v); message.success('保存成功'); setEditOpen(false); query()
  }
  const doRemove = async () => {
    if (selKey === null) return
    await orgApi.remove(selKey); message.success('删除成功'); setSelKey(null); query()
  }

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => doAdd()}>新增根节点</Button>
        <Button disabled={selKey === null} onClick={() => doAdd(selKey!)}>新增子节点</Button>
        <Button disabled={selKey === null} onClick={doEdit}>编辑</Button>
        <Popconfirm title="确认删除？" onConfirm={doRemove} disabled={selKey === null}><Button disabled={selKey === null} danger>删除</Button></Popconfirm>
      </Space>
      <Tree treeData={tree} defaultExpandAll onSelect={(k) => setSelKey(k[0] as number ?? null)} />
      <Modal title="分组设置" open={editOpen} onOk={doSave} onCancel={() => setEditOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="parentid" hidden><Input /></Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
