/**
 * 量表管理 API
 */

import http from '../http'

// 量表模板列表
export const listTemplates = () => {
  return http.get('/api/surveys/templates')
}

// 量表模板详情
export const getTemplate = (id = null) => {
  if (id) {
    return http.get(`/api/surveys/templates/${id}`)
  } else {
    // 如果没有传递id，获取默认模板
    return http.get('/api/surveys/templates/default')
  }
}

// 创建量表模板
export const createTemplate = (data) => {
  return http.post('/api/surveys/templates', data)
}

// 更新量表模板
export const updateTemplate = (id, data) => {
  return http.put(`/api/surveys/templates/${id}`, data)
}
// 更新量表模板
export const deleteTemplate = (id) => {
  return http.delete(`/api/surveys/templates/${id}`)
}
// 提交问卷
// ken260129-修改内容：支持指定量表ID
export const submitSurvey = (id, data) => {
  return http.post(`/api/surveys/results/${id}/submit`, data)
}

// 获取受试者分组关联的所有量表
// ken260129-修改内容：添加获取分组关联的所有量表的API调用
export const getMySurveyTemplates = () => {
  return http.get('/api/subject/my-survey-templates')
}

// 获取当前用户的问卷反馈历史
export const getMySurveyResults = () => {
  return http.get('/api/surveys/my')
}

// 获取指定用户的问卷反馈历史
export const getUserSurveyResults = (userId) => {
  return http.get(`/api/surveys/results/user/${userId}`)
}

// 获取指定 sessionId 的问卷反馈
export const getSurveyResultBySession = (sessionId) => {
  return http.get(`/api/surveys/session/${sessionId}`)
}