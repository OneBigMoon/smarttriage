import { useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Button, Typography, Space, Avatar, Tooltip } from 'antd'
import {
  DashboardOutlined, DesktopOutlined, ApiOutlined,
  SettingOutlined, CloudDownloadOutlined, LogoutOutlined,
  LayoutOutlined, MobileOutlined, UserOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '../../services/auth'

const { Header, Sider, Content } = Layout

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
  { key: '/preview', icon: <EyeOutlined />, label: '屏幕预览' },
  { key: '/box', icon: <DesktopOutlined />, label: '终端与分组' },
  { key: '/template', icon: <LayoutOutlined />, label: '模板管理' },
  { key: '/data', icon: <ApiOutlined />, label: '数据源' },
  { key: '/sys', icon: <SettingOutlined />, label: '系统配置' },
  { key: '/upgrade', icon: <CloudDownloadOutlined />, label: '升级包' },
  { key: '/apk', icon: <MobileOutlined />, label: 'APK管理' },
]

export default function MainLayout({ children }: { children: React.ReactNode }) {
  const nav = useNavigate()
  const loc = useLocation()
  const logout = useAuthStore((s) => s.logout)
  const user = useAuthStore((s) => s.user)

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} theme="dark" style={{
        background: 'linear-gradient(180deg, #0d1117 0%, #161b22 100%)',
        borderRight: '1px solid rgba(255,255,255,0.06)',
      }}>
        {/* Logo */}
        <div style={{ padding: '20px 20px 24px', display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 36, height: 36, borderRadius: 10,
            background: 'linear-gradient(135deg, #3b82f6, #2563eb)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 2px 8px rgba(59,130,246,0.3)',
          }}>
            <span style={{ color: '#fff', fontWeight: 700, fontSize: 16 }}>T</span>
          </div>
          <div>
            <div style={{ color: '#fff', fontSize: 15, fontWeight: 600, lineHeight: 1.2 }}>分诊管理</div>
            <div style={{ color: 'rgba(255,255,255,0.3)', fontSize: 11, marginTop: 1 }}>Triage Platform</div>
          </div>
        </div>

        <Menu
          mode="inline"
          selectedKeys={[loc.pathname === '/' ? '/dashboard' : loc.pathname]}
          items={menuItems}
          onClick={({ key }) => nav(key)}
          style={{ background: 'transparent', borderRight: 0, padding: '0 8px' }}
        />

        {/* User footer */}
        <div style={{
          position: 'absolute', bottom: 0, left: 0, right: 0,
          padding: '12px 16px', borderTop: '1px solid rgba(255,255,255,0.06)',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <Space size={10}>
            <Avatar size={30} icon={<UserOutlined />}
              style={{ background: 'linear-gradient(135deg, #3b82f6, #2563eb)' }} />
            <div>
              <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: 13, lineHeight: 1.2 }}>
                {user?.fullname || user?.username}
              </div>
              <div style={{ color: 'rgba(255,255,255,0.3)', fontSize: 11 }}>管理员</div>
            </div>
          </Space>
          <Tooltip title="退出">
            <Button type="text" size="small" icon={<LogoutOutlined />}
              onClick={logout} style={{ color: 'rgba(255,255,255,0.35)' }} />
          </Tooltip>
        </div>
      </Sider>

      <Layout style={{ background: '#0f1117' }}>
        <Header style={{
          padding: '0 24px', background: 'transparent',
          display: 'flex', justifyContent: 'flex-end', alignItems: 'center',
          borderBottom: '1px solid rgba(255,255,255,0.06)', height: 56,
        }}>
          <span style={{ color: 'rgba(255,255,255,0.3)', fontSize: 13 }}>
            {new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })}
          </span>
        </Header>
        <Content style={{ padding: 24, overflow: 'auto' }}>
          {children}
        </Content>
      </Layout>
    </Layout>
  )
}
