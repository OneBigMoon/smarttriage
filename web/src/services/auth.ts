import { create } from 'zustand'
import { authApi } from './api'

interface User { id: string; username: string; fullname: string; auths: string[] }

interface AuthState {
  token: string | null
  user: User | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('token'),
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  login: async (username, password) => {
    const { data } = await authApi.login({ username, password })
    if (data.errcode !== 0) throw new Error(data.errmsg || '登录失败')
    localStorage.setItem('token', data.access_token!)
    localStorage.setItem('user', JSON.stringify(data.user))
    set({ token: data.access_token!, user: data.user as User })
  },
  logout: () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    set({ token: null, user: null })
    authApi.logout().catch(() => {})
  },
}))
