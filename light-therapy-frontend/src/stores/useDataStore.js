import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDataStore = defineStore('data', () => {
  // 初始化空数组，数据将从后端获取
  const users = ref([])
  const schemes = ref([])
  const groups = ref([])
  const logs = ref([])
  const scales = ref([])
  const surveyTemplate = ref([])
  const surveyResponses = ref([])

  // 排序方案：GLOBAL类型的方案显示在前面
  function sortSchemes() {
    schemes.value.sort((a, b) => {
      if (a.scope === 'GLOBAL' && b.scope !== 'GLOBAL') return -1
      if (a.scope !== 'GLOBAL' && b.scope === 'GLOBAL') return 1
      return 0
    })
  }

  function addUser(user) {
    users.value.push(user)
  }

  function updateUser(id, updatedUser) {
    const index = users.value.findIndex(u => u.id === id)
    if (index !== -1) {
      users.value[index] = { ...users.value[index], ...updatedUser }
    }
  }

  function deleteUser(id) {
    users.value = users.value.filter(u => u.id !== id)
  }

  function addScheme(scheme) {
    schemes.value.push({ ...scheme, id: Date.now() })
    sortSchemes()
  }

  function updateScheme(id, updatedScheme) {
    const index = schemes.value.findIndex(s => s.id === id)
    if (index !== -1) {
      schemes.value[index] = { ...schemes.value[index], ...updatedScheme }
      sortSchemes()
    }
  }

  function deleteScheme(id) {
    schemes.value = schemes.value.filter(s => s.id !== id)
    sortSchemes()
  }

  function addGroup(groupName, initialScaleId = null) {
    groups.value.push({ name: groupName, scaleId: initialScaleId })
  }

  function deleteGroup(groupName) {
    groups.value = groups.value.filter(g => g.name !== groupName)
  }

  function saveLog(log) {
    logs.value.push(log)
  }

  function addScale(scale) {
    scales.value.push({ ...scale, id: Date.now() })
  }

  function updateScale(id, updatedScale) {
    const index = scales.value.findIndex(s => s.id === id)
    if (index !== -1) {
      scales.value[index] = { ...scales.value[index], ...updatedScale }
    }
  }

  function deleteScale(id) {
    scales.value = scales.value.filter(s => s.id !== id)
  }

  function updateSurvey(newSurvey) {
    surveyTemplate.value = newSurvey
  }

  function saveSurveyResponse(response) {
    surveyResponses.value.push(response)
  }

  function importDb(data) {
    if (data.users) users.value = data.users
    if (data.schemes) {
      schemes.value = data.schemes
      sortSchemes()
    }
    if (data.groups) groups.value = data.groups
    if (data.logs) logs.value = data.logs
    if (data.scales) scales.value = data.scales
    if (data.surveyTemplate) surveyTemplate.value = data.surveyTemplate
    if (data.surveyResponses) surveyResponses.value = data.surveyResponses
  }

  function getFullDbState() {
    return {
      users: users.value,
      schemes: schemes.value,
      groups: groups.value,
      logs: logs.value,
      scales: scales.value,
      surveyTemplate: surveyTemplate.value,
      surveyResponses: surveyResponses.value
    }
  }

  return {
    users,
    schemes,
    groups,
    logs,
    scales,
    surveyTemplate,
    surveyResponses,
    addUser,
    updateUser,
    deleteUser,
    addScheme,
    updateScheme,
    deleteScheme,
    addGroup,
    deleteGroup,
    saveLog,
    addScale,
    updateScale,
    deleteScale,
    updateSurvey,
    saveSurveyResponse,
    importDb,
    getFullDbState
  }
})
