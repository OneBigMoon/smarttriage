import React from 'react'
import ReactDOM from 'react-dom/client'
import { ConfigProvider, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import './global.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#1890ff',
          borderRadius: 8,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif',
          fontSize: 14,
          colorBgContainer: '#141414',
          colorBgLayout: '#0a0a0a',
          colorBgElevated: '#1f1f1f',
          colorBorder: '#303030',
          colorText: 'rgba(255,255,255,0.85)',
          colorTextSecondary: 'rgba(255,255,255,0.45)',
        },
        components: {
          Card: { colorBgContainer: '#1f1f1f' },
          Table: { colorBgContainer: '#1f1f1f' },
          Menu: { colorBgContainer: 'transparent' },
        },
      }}
    >
      <App />
    </ConfigProvider>
  </React.StrictMode>,
)
