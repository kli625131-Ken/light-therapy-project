/**
 * 分组管理 API
 */

import http from '../http'

// 分组列表
export const listGroups = (page = 0, pageSize = 20) => {
  return http.get('/api/groups', {
    params: { page, pageSize }
  })
}

// 创建分组
// ken260129-修改内容：支持传递多个量表ID，使用surveyTemplateIds字段
export const createGroup = (data) => {
  return http.post('/api/groups', data)
}

// 更新分组
// ken260129-修改内容：支持传递多个量表ID，使用surveyTemplateIds字段
export const updateGroup = (id, data) => {
  return http.put(`/api/groups/${id}`, data)
}

// 删除分组
export const deleteGroup = (id) => {
  return http.delete(`/api/groups/${id}`)
}