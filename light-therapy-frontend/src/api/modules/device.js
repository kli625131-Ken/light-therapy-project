/**
 * 设备管理 API
 */

import http from '../http'

// 设备列表
export const listDevices = () => {
  return http.get('/api/devices')
}
