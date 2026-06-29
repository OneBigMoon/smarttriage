import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Input, Button, message, Typography } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useAuthStore } from '../../services/auth'

export default function LoginPage() {
  const [loading, setLoading] = useState(false)
  const nav = useNavigate()
  const login = useAuthStore((s) => s.login)

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: '#0f1117', position: 'relative', overflow: 'hidden',
    }}>
      {/* Background decoration */}
      <div style={{
        position: 'absolute', width: 800, height: 800, borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(59,130,246,0.06) 0%, transparent 70%)',
        top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
      }} />
      <div style={{
        position: 'absolute', inset: 0,
        backgroundImage: 'linear-gradient(rgba(59,130,246,0.02) 1px, transparent 1px), linear-gradient(90deg, rgba(59,130,246,0.02) 1px, transparent 1px)',
        backgroundSize: '60px 60px',
      }} />

      {/* Login card */}
      <div style={{
        width: 380, padding: '48px 40px', borderRadius: 16,
        background: 'rgba(26,29,39,0.9)', backdropFilter: 'blur(24px)',
        border: '1px solid rgba(255,255,255,0.06)',
        boxShadow: '0 24px 80px rgba(0,0,0,0.5)',
        position: 'relative', zIndex: 1,
      }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: 36 }}>
          <div style={{
            width: 52, height: 52, borderRadius: 14, margin: '0 auto 16px',
            background: 'linear-gradient(135deg, #3b82f6, #2563eb)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 4px 16px rgba(59,130,246,0.3)',
          }}>
            <span style={{ color: '#fff', fontWeight: 700, fontSize: 24 }}>T</span>
          </div>
          <Typography.Title level={3} style={{ margin: 0, color: '#fff', fontWeight: 700, letterSpacing: -0.5 }}>
            智能分诊管理平台
          </Typography.Title>
          <div style={{ color: 'rgba(255,255,255,0.3)', marginTop: 6, fontSize: 13, letterSpacing: 0.5 }}>
            Hospital Triage Management System
          </div>
        </div>

        <Form size="large" onFinish={async (v) => {
          setLoading(true)
          try { await login(v.username, v.password); message.success('登录成功'); nav('/dashboard') }
          catch (e: any) { message.error(e.message || '登录失败') }
          finally { setLoading(false) }
        }}>
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined style={{ color: 'rgba(255,255,255,0.2)' }} />}
              placeholder="用户名"
              style={{ background: 'rgba(255,255,255,0.03)', borderColor: 'rgba(255,255,255,0.08)', borderRadius: 8 }} />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined style={{ color: 'rgba(255,255,255,0.2)' }} />}
              placeholder="密码"
              style={{ background: 'rgba(255,255,255,0.03)', borderColor: 'rgba(255,255,255,0.08)', borderRadius: 8 }} />
          </Form.Item>
          <Form.Item style={{ marginBottom: 16 }}>
            <Button type="primary" htmlType="submit" loading={loading} block
              style={{ height: 46, borderRadius: 8, fontWeight: 600, fontSize: 15 }}>
              登 录
            </Button>
          </Form.Item>
        </Form>

        <div style={{ textAlign: 'center', color: 'rgba(255,255,255,0.2)', fontSize: 12, lineHeight: 1.6 }}>
          默认账号: root / root
        </div>
      </div>
    </div>
  )
}
