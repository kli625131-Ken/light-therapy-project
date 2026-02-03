import http from '../http'

export const listSchemeStagesMe = (schemeId) => http.get(`/api/scheme-stages/me/${schemeId}`)

export const listSchemesMe = (scope = 'GLOBAL', groupId = null, page = 0, pageSize = 20) => http.get('/api/schemes/me', { params: { scope, groupId, page, pageSize } })
export const listSchemes = (scope = 'GLOBAL', groupId = null, page = 0, pageSize = 20) => http.get('/api/schemes', { params: { scope, groupId, page, pageSize } })
export const allSchemes = (page = 0, pageSize = 100) => http.get('/api/schemes/all', { params: { page, pageSize } })

export const createScheme = (data) => http.post('/api/schemes', data)

export const updateScheme = (id, data) => http.put(`/api/schemes/${id}/basic`, data)
export const updateSchemeWithStages = (id, data) => http.put(`/api/schemes/${id}`, data)

export const deleteScheme = (id) => http.delete(`/api/schemes/${id}`)

export const listSchemeStages = (schemeId) => http.get(`/api/schemes/${schemeId}/stages`)

export const createSchemeStage = (schemeId, data) => http.post(`/api/schemes/${schemeId}/stages`, data)

export const updateSchemeStage = (id, data) => http.put(`/api/scheme-stages/${id}`, data)

export const deleteSchemeStage = (id) => http.delete(`/api/scheme-stages/${id}`)

export const createSchemeWithStages = (data) => http.post('/api/schemes/with-stages', data)
