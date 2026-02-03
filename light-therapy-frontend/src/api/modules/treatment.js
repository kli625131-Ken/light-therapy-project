import http from '../http'

export const createTreatment = (data) => http.post('/api/treatments', data)

export const manualStartTreatment = (data) => http.post('/api/treatments/manual/start', data)
export const manualControlTreatment = (data) => http.post(`/api/treatments/manual/control`, data)

export const listTreatments = (subjectId = null, status = null, startDate = null, endDate = null, page = 0, pageSize = 10) => http.get('/api/treatments', { params: { subjectId, status, startDate, endDate, page, pageSize } })

export const getTreatment = (id) => http.get(`/api/treatments/${id}`)

export const getTreatmentbySubject = (id, status = null, startDate = null, endDate = null, page = 0, pageSize = 10) => http.get(`/api/treatments/subject/${id}`, { params: { status, startDate, endDate, page, pageSize } })

export const startTreatment = (id) => http.post(`/api/treatments/${id}/start`)

export const endTreatment = (id, data) => http.post(`/api/treatments/${id}/end`, data)

export const pauseTreatment = (id) => http.post(`/api/treatments/${id}/pause`)

export const resumeTreatment = (id) => http.post(`/api/treatments/${id}/resume`)

export const failTreatment = (id, reason = 'failed') => http.post(`/api/treatments/${id}/fail`, null, { params: { reason } })

export const cancelTreatment = (id, reason = 'cancelled') => http.post(`/api/treatments/${id}/cancel`, null, { params: { reason } })

export const addTreatmentEvent = (id, data) => http.post(`/api/treatments/${id}/events`, data)

export const listTreatmentEvents = (id) => http.get(`/api/treatments/${id}/events`)

export const createAndStartTreatment = (data) => http.post('/api/treatments/start-now', data)
