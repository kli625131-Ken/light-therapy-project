import http from '../http'

export const listUsers = (page = 0, pageSize = 20) => http.get('/api/users', { params: { page, pageSize } })

export const createSubject = (data) => http.post('/api/subjects', data)

export const updateUser = (id, data) => http.put(`/api/users/${id}`, data)

export const deleteUser = (id) => http.delete(`/api/users/${id}`)

export const listSubjects = (page = 0, pageSize = 20) => http.get('/api/subjects', { params: { page, pageSize } })
export const updateSubject = (id, data) => http.put(`/api/subjects/${id}`, data)
export const deleteSubject = (id) => http.delete(`/api/subjects/${id}`)
