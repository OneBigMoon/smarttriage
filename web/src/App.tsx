import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './services/auth'
import MainLayout from './components/Layout/MainLayout'
import PageTransition from './components/PageTransition'
import LoginPage from './pages/Login/LoginPage'
import DashboardPage from './pages/Dashboard/DashboardPage'
import BoxPage from './pages/Box/BoxPage'
import DataSourcePage from './pages/DataSource/DataSourcePage'
import SystemPage from './pages/System/SystemPage'
import UpgradePage from './pages/Upgrade/UpgradePage'
import TemplatePage from './pages/Template/TemplatePage'
import ApkPage from './pages/Apk/ApkPage'
import ScreenPreviewPage from './pages/ScreenPreview/ScreenPreviewPage'

function Guard({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token)
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/*" element={
          <Guard>
            <MainLayout>
              <PageTransition>
                <Routes>
                  <Route path="/dashboard" element={<DashboardPage />} />
                  <Route path="/box" element={<BoxPage />} />
                  <Route path="/template" element={<TemplatePage />} />
                  <Route path="/data" element={<DataSourcePage />} />
                  <Route path="/sys" element={<SystemPage />} />
                  <Route path="/upgrade" element={<UpgradePage />} />
                  <Route path="/apk" element={<ApkPage />} />
                  <Route path="/preview" element={<ScreenPreviewPage />} />
                  <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
              </PageTransition>
            </MainLayout>
          </Guard>
        } />
      </Routes>
    </BrowserRouter>
  )
}
