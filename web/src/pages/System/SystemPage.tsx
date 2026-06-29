import { useState, useEffect } from 'react'
import { Form, Input, Button, Card, message, Typography, Tabs, Row, Col, Alert, Space, Divider } from 'antd'
import { DatabaseOutlined, CloudServerOutlined, SaveOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { sysApi } from '../../services/api'

const { Text, Title } = Typography

export default function SystemPage() {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [saved, setSaved] = useState(false)

  useEffect(() => { sysApi.get().then(r => { if (r.data.result) form.setFieldsValue(r.data.result) }) }, [])

  const doSave = async () => {
    const v = await form.validateFields()
    if (!v.url) { message.error('请填写Oracle连接URL'); return }
    setLoading(true)
    try { await sysApi.save(v); message.success('保存成功'); setSaved(true); setTimeout(() => setSaved(false), 3000) }
    catch { message.error('保存失败') }
    finally { setLoading(false) }
  }

  return (
    <div>
      {saved && <Alert message="配置已保存" description="Oracle同步服务将自动使用新配置，无需重启。" type="success" showIcon closable style={{ marginBottom: 16 }} />}

      <Tabs defaultActiveKey="connection" items={[
        {
          key: 'connection',
          label: <Space><CloudServerOutlined />数据库连接</Space>,
          children: (
            <Card bordered={false}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 20 }}>
                配置连接医院HIS系统的Oracle数据库。只需填写一次，系统启动时自动连接。
              </Text>
              <Form form={form} layout="vertical" style={{ maxWidth: 600 }}>
                <Form.Item name="url" label="连接字符串" tooltip="Oracle TNS 连接串，格式：IP:端口/服务名">
                  <Input placeholder="192.168.1.100:1521/orcl" />
                </Form.Item>
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item name="username" label="账户"><Input placeholder="分诊系统使用的Oracle账户" /></Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name="password" label="密码"><Input.Password /></Form.Item>
                  </Col>
                </Row>
              </Form>
            </Card>
          ),
        },
        {
          key: 'business',
          label: <Space><DatabaseOutlined />业务数据</Space>,
          children: (
            <Card bordered={false}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 20 }}>
                配置各业务场景对应的Oracle表或视图名。留空则该业务不启用。
              </Text>
              <Form form={form} layout="vertical" style={{ maxWidth: 600 }}>
                <Divider orientation="left" orientationMargin={0}>分诊数据</Divider>
                <Form.Item name="primarytablename" label="一级分诊表/视图"
                  tooltip="科室大屏显示的叫号数据来源">
                  <Input placeholder="如：VW_YS_JHLSXX_FQSYYXY" />
                </Form.Item>
                <Form.Item name="secondarytablename" label="二级分诊表/视图"
                  tooltip="诊室叫号屏显示的排队详情来源">
                  <Input placeholder="如：VW_PDJH_HZBR00" />
                </Form.Item>
                <Form.Item name="secondarycounttablename" label="二级分诊人数表/视图"
                  tooltip="查询每个医生当前等候人数">
                  <Input placeholder="如：VW_PDJH_YSJZRS" />
                </Form.Item>

                <Divider orientation="left" orientationMargin={0}>检验数据</Divider>
                <Form.Item name="drawbloodtablename" label="检验/抽血表/视图"
                  tooltip="检验窗口叫号数据来源">
                  <Input placeholder="如：VW_JYCYJH" />
                </Form.Item>

                <Divider orientation="left" orientationMargin={0}>药房数据</Divider>
                <Form.Item name="pharmacytablename" label="药房表/视图"
                  tooltip="药房叫号数据来源（一级和二级共用）">
                  <Input placeholder="如：VW_PY_XSPPDJH_X" />
                </Form.Item>
              </Form>
            </Card>
          ),
        },
        {
          key: 'pacs',
          label: <Space><DatabaseOutlined />超声数据</Space>,
          children: (
            <Card bordered={false}>
              <Alert message="超声分诊通常使用独立的PACS系统账户" type="info" showIcon style={{ marginBottom: 20 }} />
              <Form form={form} layout="vertical" style={{ maxWidth: 600 }}>
                <Form.Item name="pacsusername" label="超声系统账户">
                  <Input />
                </Form.Item>
                <Form.Item name="pacspassword" label="超声系统密码">
                  <Input.Password />
                </Form.Item>
                <Form.Item name="pacssecondarytablename" label="超声排队表/视图"
                  tooltip="PACS系统中的超声排队数据表">
                  <Input placeholder="如：V_QUEUE" />
                </Form.Item>
              </Form>
            </Card>
          ),
        },
      ]} />

      <div style={{ marginTop: 16 }}>
        <Button type="primary" icon={<SaveOutlined />} onClick={doSave} loading={loading} size="large">
          保存全部配置
        </Button>
      </div>
    </div>
  )
}
