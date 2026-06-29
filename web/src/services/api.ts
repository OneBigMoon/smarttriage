import axios from 'axios'
import { message } from 'antd'

const api = axios.create({ baseURL: '/api/v1', timeout: 30000 })

api.interceptors.request.use((cfg) => {
  const t = localStorage.getItem('token')
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export interface ApiRes<T = unknown> {
  errcode: number
  errmsg?: string
  result?: T
  user?: T
  access_token?: string
}

export const authApi = {
  login: (d: { username: string; password: string }) => api.post<ApiRes>('/user/login', d),
  logout: () => api.post<ApiRes>('/user/logout'),
}

export const boxApi = {
  query: (d: Record<string, unknown>) => api.post<ApiRes>('/boxes/query', d),
  save: (d: Record<string, unknown>) => api.post<ApiRes>('/boxes/save', d),
  move: (id: string, orgId: number) => api.post<ApiRes>('/boxes/move', null, { params: { id, orgId } }),
  remove: (id: string) => api.post<ApiRes>('/boxes/remove', null, { params: { id } }),
  power: (id: string, cmd: string) => api.post<ApiRes>('/boxes/power', null, { params: { id, cmd } }),
  groupPower: (id: number, cmd: string) => api.post<ApiRes>('/boxes/group/power', null, { params: { id, cmd } }),
  groupSave: (id: number, d: Record<string, unknown>) => api.post<ApiRes>('/boxes/group/save', d, { params: { id } }),
  toggleDataEnabled: (id: string) => api.post<ApiRes>('/boxes/dataenabled/toggle', null, { params: { id } }),
  enableAllData: () => api.post<ApiRes>('/boxes/dataenabled/enable-all'),
  uploadLog: (id: string) => api.post<ApiRes>('/boxes/upload-log', null, { params: { id } }),
  checkLog: (id: string) => api.post<ApiRes>('/boxes/check-log', null, { params: { id } }),
  /** 预览终端在 Android WebView 上的渲染效果 */
  previewUrl: (id: string) => `/api/v1/boxes/preview/${id}`,
}

export const dsApi = {
  query: (d: Record<string, unknown>) => api.post<ApiRes>('/datasources/query', d),
  save: (d: Record<string, unknown>) => api.post<ApiRes>('/datasources/save', d),
  remove: (id: string) => api.post<ApiRes>('/datasources/remove', null, { params: { id } }),
}

export const dsTypeApi = { query: () => api.post<ApiRes>('/datasourcetypes/query') }
export const orgApi = {
  query: () => api.post<ApiRes>('/orgnizations/query'),
  save: (d: Record<string, unknown>) => api.post<ApiRes>('/orgnizations/save', d),
  remove: (id: number) => api.post<ApiRes>('/orgnizations/remove', null, { params: { id } }),
}
export const styleApi = { query: () => api.post<ApiRes>('/styles/query') }
export const sysApi = {
  get: () => api.post<ApiRes>('/system'),
  save: (d: Record<string, unknown>) => api.post<ApiRes>('/system/save', d),
}
export const upApi = {
  query: () => api.post<ApiRes>('/upgrade/query'),
  remove: (id: string) => api.post<ApiRes>('/upgrade/remove', null, { params: { id } }),
}

export const discoveryApi = {
  departments: () => api.post<ApiRes>('/discovery/departments'),
  screens: () => api.post<ApiRes>('/discovery/screens'),
  queues: () => api.post<ApiRes>('/discovery/queues'),
  windows: () => api.post<ApiRes>('/discovery/windows'),
  pharmacyDepts: () => api.post<ApiRes>('/discovery/pharmacy-depts'),
  pharmacyWindows: (deptNo: number) => api.post<ApiRes>('/discovery/pharmacy-windows', { dept_no: deptNo }),
}

export default api
