<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Users, Clock, Settings, LogOut, Activity, Database, Zap, 
  Upload, Download, Plus, Trash2, Edit3, FileDown, 
  FileText, Eye, Layers, MessageSquare, X 
} from 'lucide-vue-next'
import { useUserStore } from '../stores/useUserStore'
import { useDataStore } from '../stores/useDataStore'
import { listDevices, createSubject, updateUser, listUsers, createGroup, listGroups, updateScheme, deleteScheme, listSchemes, allSchemes, createSchemeWithStages, updateSchemeWithStages, listSubjects, updateSubject, deleteSubject, listTreatments } from '../api'
import { endTreatment, manualStartTreatment, researcherExecuteScenario } from '../api/modules/treatment'
import { listTemplates, getTemplate, createTemplate, updateTemplate,deleteTemplate } from '../api/modules/survey'
// xlsx-style 库已安装
import { getUserSurveyResults } from '../api/modules/survey'
import { downloadMergedCSV, downloadTreatmentRecordsXLSX } from '../utils/csvExport'
import { exportDatabase } from '../utils/dbExport'
import Button from '../components/ui/Button.vue'
import Card from '../components/ui/Card.vue'
import Modal from '../components/ui/Modal.vue'

const router = useRouter()
const userStore = useUserStore()
const dataStore = useDataStore()

const activeTab = ref('users')
const isUserModalOpen = ref(false)
const isSchemeModalOpen = ref(false)
const isGroupModalOpen = ref(false)
const viewLogsUserId = ref(null)
const isEditingUser = ref(false)
const editingUserId = ref(null)
const isEditingScheme = ref(false)
const editingSchemeId = ref(null)
const isExecutingScenario = ref(false)
const newGroupName = ref('')
// 正确初始化editingSurvey，避免dataStore.surveyTemplate为undefined时出现错误
const editingSurvey = ref([])

const newQuestionType = ref('range')
const surveySimplifiedMode = ref(false)
const fileInputRef = ref(null)

const userForm = ref({
  id: '',
  password: '',
  gender: '男',
  age: 30,
  group_name: '',
  subjectName: '',
  contactInfo: '',
  enrollmentDate: '',
  diagnosisResult: ''
})

// 设备类型常量
const DEVICE_TYPES = {
  NORMAL: 'normal',
  TYPE_485: '485',
  MCB: 'MCB'
}

// 设备列表（从API获取）
const devices = ref([])

// 获取设备列表
const fetchDevices = async () => {
  try {
    const data = await listDevices()
    devices.value = data
  } catch (error) {
    console.error('获取设备列表失败:', error)
    devices.value = []
  }
}

// 根据当前选中的分组筛选设备
const filteredDevices = computed(() => {
  console.log('filteredDevices computed triggered')
  console.log('schemeForm.target_group', schemeForm.value.target_group)
  console.log('dataStore.groups.length', dataStore.groups.length)
  console.log('devices.value.length', devices.value.length)
  
  // 当选择"无 (通用)"时，返回所有设备
  if (!schemeForm.value.target_group || schemeForm.value.target_group === '无 (通用)' || schemeForm.value.target_group === '无（通用）') {
    console.log('Returning all devices: no target group or "无（通用）" selected')
    const allDevices = [...devices.value]
    
    // 为每个设备添加deviceType属性（如果不存在）
    allDevices.forEach(device => {
      console.log('Processing device:', device.deviceName, 'Current deviceType:', device.deviceType)
      
      // 优先使用后端返回的deviceType
      if (device.deviceType) {
        console.log('Using backend deviceType:', device.deviceType)
        // 转换后端返回的设备类型到前端使用的格式
        if (device.deviceType === 'MCB') {
          device.deviceType = DEVICE_TYPES.MCB
        } else if (device.deviceType === '485') {
          device.deviceType = DEVICE_TYPES.TYPE_485
        } else {
          device.deviceType = DEVICE_TYPES.NORMAL
        }
      } else {
        // 根据设备名称判断类型
        if (device.deviceName.includes('天窗')) {
          device.deviceType = DEVICE_TYPES.MCB
        } else if (device.deviceName.includes('485')) {
          device.deviceType = DEVICE_TYPES.TYPE_485
        } else {
          device.deviceType = DEVICE_TYPES.NORMAL
        }
        console.log('Added deviceType based on name:', device.deviceName, device.deviceType)
      }
      
      console.log('Final deviceType for', device.deviceName, 'is:', device.deviceType)
    })
    
    console.log('Final all devices result:', allDevices.map(d => ({ name: d.deviceName, type: d.deviceType })))
    return allDevices
  }
  
  if (!dataStore.groups.length) {
    console.log('Returning empty array: no groups available')
    return []
  }
  
  // 找到当前选中的分组
  const targetGroupId = schemeForm.value.target_group
  console.log('Looking for group with id:', targetGroupId)
  
  const selectedGroup = dataStore.groups.find(group => 
    String(group.id) === String(targetGroupId) || 
    group.name === targetGroupId
  )
  
  console.log('Selected group:', selectedGroup)
  
  if (!selectedGroup || !selectedGroup.devices || !Array.isArray(selectedGroup.devices)) {
    console.log('Returning empty array: no selected group or devices')
    return []
  }
  
  // 返回分组中包含的设备
  const groupDeviceIds = selectedGroup.devices.map(device => String(device.id))
  console.log('groupDeviceIds', groupDeviceIds)
  console.log('devices.value', devices.value)
  
  const result = devices.value.filter(device => groupDeviceIds.includes(String(device.id)))
  console.log('Filtered devices result:', result)
  
  // 为每个设备添加deviceType属性（如果不存在）
  result.forEach(device => {
    console.log('Processing device:', device.deviceName, 'Current deviceType:', device.deviceType)
    
    // 优先使用后端返回的deviceType
    if (device.deviceType) {
      console.log('Using backend deviceType:', device.deviceType)
      // 转换后端返回的设备类型到前端使用的格式
      if (device.deviceType === 'MCB') {
        device.deviceType = DEVICE_TYPES.MCB
      } else if (device.deviceType === '485') {
        device.deviceType = DEVICE_TYPES.TYPE_485
      } else {
        device.deviceType = DEVICE_TYPES.NORMAL
      }
    } else {
      // 根据设备名称判断类型
      if (device.deviceName.includes('天窗')) {
        device.deviceType = DEVICE_TYPES.MCB
      } else if (device.deviceName.includes('485')) {
        device.deviceType = DEVICE_TYPES.TYPE_485
      } else {
        device.deviceType = DEVICE_TYPES.NORMAL
      }
      console.log('Added deviceType based on name:', device.deviceName, device.deviceType)
    }
    
    console.log('Final deviceType for', device.deviceName, 'is:', device.deviceType)
  })
  
  console.log('Final filteredDevices result:', result.map(d => ({ name: d.deviceName, type: d.deviceType })))
  
  return result
})

// 将值转换为百分比显示
const toPercentage = (value) => {
  return value === null || value === undefined ? '' : Number(value)
}

// 将百分比值转换为保存值
const toRawValue = (percentage) => {
  const num = Number(percentage)
  return isNaN(num) ? null : num
}

const schemeForm = ref({
  name: '',
  description: '',
  target_group: '',
  stages: []
})

watch(() => dataStore.groups?.length, (newLength) => {
  if (newLength > 0 && !userForm.value.group_name) {
    userForm.value.group_name = dataStore.groups?.[0]?.name
  }
}, { immediate: true })

const searchUserId = ref('')
const userPageSize = ref(10)
const userCurrentPage = ref(1)

const users = computed(() => {
  const all = dataStore.users ?? []
  const allUsers = Array.isArray(all) ? all : []
  
  let filteredUsers = [...allUsers]
  
  // 按用户ID查询
  if (searchUserId.value) {
    const searchValue = searchUserId.value.toLowerCase()
    filteredUsers = filteredUsers.filter(user => 
      user.id?.toString().toLowerCase().includes(searchValue) || 
      user.subjectCode?.toLowerCase().includes(searchValue)
    )
  }
  return filteredUsers
})

const paginatedUsers = computed(() => {
  const startIndex = (userCurrentPage.value - 1) * userPageSize.value
  const endIndex = startIndex + userPageSize.value
  return (Array.isArray(users.value) ? users.value : []).slice(startIndex, endIndex)
})

const fetchUsers = async () => {
  try {
    const response = await listSubjects(0, 100) // Fetch strict subjects list
    if (response && response.content && Array.isArray(response.content)) {
      dataStore.users = response.content
    } else {
      dataStore.users = Array.isArray(response) ? response : []
    }
  } catch (error) {
    console.error('获取受试者列表失败:', error)
  }
}

// 先定义所有需要的fetch函数
const fetchScales = async () => {
  try {
    const response = await listTemplates()
    dataStore.scales = Array.isArray(response) ? response : []
  } catch (error) {
    console.error('获取量表列表失败:', error)
    // 不弹出错误提示，避免影响用户体验
  }
}

const fetchGroups = async () => {
  try {
    const response = await listGroups()
    // Handle paginated response (response.content) vs flat array
    if (response && response.content && Array.isArray(response.content)) {
      dataStore.groups = response.content
    } else {
      dataStore.groups = Array.isArray(response) ? response : []
    }
  } catch (error) {
    console.error('获取分组列表失败:', error)
    // 不弹出错误提示，避免影响用户体验
  }
}

const fetchSchemes = async () => {
  try {
    const response = await allSchemes()
    if (response && response.content && Array.isArray(response.content)) {
      dataStore.schemes = response.content
    } else {
      dataStore.schemes = []
    }
    // 排序：GLOBAL 类型的方案显示在前面
    dataStore.schemes.sort((a, b) => {
      if (a.scope === 'GLOBAL' && b.scope !== 'GLOBAL') return -1
      if (a.scope !== 'GLOBAL' && b.scope === 'GLOBAL') return 1
      return 0
    })
    console.log('dataStore.schemes', dataStore.schemes)
  } catch (error) {
    console.error('获取方案列表失败:', error)
    // 不弹出错误提示，避免影响用户体验
  }
}

// 组件初始化时获取数据
// 使用onMounted钩子确保所有函数都已定义
import { onMounted } from 'vue'

onMounted(async () => {
  // 只调用量表API，其他API需要ADMIN角色
  await fetchScales()
  
  // 如果需要调用其他API，可以添加角色判断
  // 如果需要调用其他API，可以添加角色判断
  if (true) { // 暂时强制加载，方便调试
    await fetchUsers()
    await fetchGroups()
    await fetchSchemes()
    await fetchDevices()
  }
})

const userTotalPages = computed(() => {
  return Math.ceil(users.value.length / userPageSize.value) || 1
})

const goToUserPage = (page) => {
  userCurrentPage.value = Math.max(1, Math.min(page, userTotalPages.value))
}

const userNextPage = () => {
  if (userCurrentPage.value < userTotalPages.value) {
    userCurrentPage.value++
  }
}

const userPrevPage = () => {
  if (userCurrentPage.value > 1) {
    userCurrentPage.value--
  }
}

// 搜索时重置页码
watch(searchUserId, () => {
  userCurrentPage.value = 1
})
const schemes = computed(() => {
  const allSchemes = (dataStore.schemes ?? [])
  // 确保GLOBAL类型的方案显示在前面
  return [...allSchemes].sort((a, b) => {
    if (a.scope === 'GLOBAL' && b.scope !== 'GLOBAL') return -1
    if (a.scope !== 'GLOBAL' && b.scope === 'GLOBAL') return 1
    return 0
  })
})
const listschemes = computed(() => {
  const filteredSchemes = (dataStore.schemes ?? []).filter(scheme => scheme.name !== 'Manual Scheme')
  // 确保GLOBAL类型的方案显示在前面
  return filteredSchemes.sort((a, b) => {
    if (a.scope === 'GLOBAL' && b.scope !== 'GLOBAL') return -1
    if (a.scope !== 'GLOBAL' && b.scope === 'GLOBAL') return 1
    return 0
  })
})
const groups = computed(() => dataStore.groups ?? [])
const searchLogUserId = ref('')
const logDateRange = ref({ start: '', end: '' })
const logPageSize = ref(10)
const logCurrentPage = ref(1)

const logs = ref([])
const logsTotal = ref(0)
const logTotalPages = ref(0) // 直接使用后端返回的分页总数

const mapSessionToLog = (s) => {
  const u = users.value.find(u => u.id === s.subjectId)
  const sch = schemes.value.find(sc => sc.id === s.schemeId)
  
  let duration = '-'
  if (s.startTime && s.endTime) {
     const st = new Date(s.startTime)
     const et = new Date(s.endTime)
     const diff = Math.max(0, Math.ceil((et - st) / 60000))
     duration = diff + ' 分钟'
  }
  
  // Format Date YYYY-MM-DD
  const dateStr = s.startTime ? s.startTime.split('T')[0] : ''
  const timeStart = s.startTime ? s.startTime.split('T')[1]?.substring(0, 5) : ''
  const timeEnd = s.endTime ? s.endTime.split('T')[1]?.substring(0, 5) : ''

  return {
    id: s.id,
    user_id: u ? (u.subjectCode || u.id) : s.subjectId, 
    user_name: u?.name || '',
    date: dateStr,
    start_time: timeStart + (timeEnd ? ' - ' + timeEnd : ''),
    end_time: timeEnd,
    scheme_name: sch?.name || '手动模式', 
    duration_actual: duration,
    status: s.status,
    metrics: s.metricsJson ? JSON.parse(s.metricsJson) : {}
  }
}

const fetchLogs = async () => {
  try {
    console.log('searchLogUserId.value', searchLogUserId.value)
    const subjectId = searchLogUserId.value ? 
         (users.value.find(u => u.subjectCode === searchLogUserId.value || u.id == searchLogUserId.value)?.id || searchLogUserId.value) 
         : null
         
    const start = logDateRange.value?.start ? new Date(logDateRange.value.start).toISOString().split('T')[0] : null
    const end = logDateRange.value?.end ? new Date(logDateRange.value.end).toISOString().split('T')[0] : null
    
    // API Call
    // Note: searchLogUserId.value might be "UserCode" string, but API expects ID. 
    // Logic above tries to resolve it. If failed, passes as is (likely fails if not Long).
    // Better logic: if searchLogUserId input matches a user in `users`, use ID. Else assume it is ID?
    
    // Use resolved ID if possible. If subjectId is string (not numeric), API might 400. 
    // Assuming Input is ID for MVP or Code.
    
    let targetUid = null
    if (searchLogUserId.value) {
        // Try to find user by subjectCode or ID string
        const target = users.value.find(u => String(u.subjectCode) === String(searchLogUserId.value) || String(u.id) === String(searchLogUserId.value))
        targetUid = target ? target.id : ( !isNaN(searchLogUserId.value) ? searchLogUserId.value : null )
    }

    const res = await listTreatments(targetUid, null, start, end, logCurrentPage.value - 1, logPageSize.value)
    if (res && res.content) {
       logs.value = res.content.map(mapSessionToLog)
       logsTotal.value = res.totalElements
       // 直接使用后端返回的totalPages，而不是前端计算
       logTotalPages.value = res.totalPages
    } else {
       logs.value = []
       logsTotal.value = 0
       logTotalPages.value = 0
    }
    
    // 获取问卷反馈数据
    try {
      if (targetUid) {
        // 如果查询了特定用户，获取该用户的问卷反馈
        const surveyResults = await getUserSurveyResults(targetUid)
        if (surveyResults && Array.isArray(surveyResults)) {
          // 更新dataStore中的scales数据
          if (dataStore) {
            dataStore.scales = surveyResults
          }
          console.log('获取问卷反馈成功:', surveyResults)
        }
      } else {
        // 如果没有查询特定用户，暂时不获取问卷反馈
        // 因为可能会返回所有用户的问卷反馈，数据量过大
        console.log('未查询特定用户，跳过获取问卷反馈')
      }
    } catch (surveyError) {
      console.error('获取问卷反馈失败:', surveyError)
    }
  } catch (e) {
    console.error('Fetch logs failed', e)
    logs.value = []
    logsTotal.value = 0
    logTotalPages.value = 0
  }
}

const paginatedLogs = computed(() => logs.value)

// Navigation
const goToLogPage = (page) => {
  logCurrentPage.value = Math.max(1, Math.min(page, logTotalPages.value))
}
const logNextPage = () => { if (logCurrentPage.value < logTotalPages.value) logCurrentPage.value++ }
const logPrevPage = () => { if (logCurrentPage.value > 1) logCurrentPage.value-- }

const getSmartPagination = (current, total) => {
  if (total <= 7) return Array.from({length: total}, (_, i) => i + 1)
  const pages = [1]
  if (current > 4) pages.push('...')
  let start = Math.max(2, current - 1)
  let end = Math.min(total - 1, current + 1)
  if (current <= 4) end = 5
  if (current >= total - 3) start = total - 4
  for (let i = start; i <= end; i++) pages.push(i)
  if (current < total - 3) pages.push('...')
  if (total > 1) pages.push(total)
  return pages
}

const visibleLogPages = computed(() => getSmartPagination(logCurrentPage.value, logTotalPages.value))
const visibleUserPages = computed(() => getSmartPagination(userCurrentPage.value, userTotalPages.value))

// Watchers
watch([logCurrentPage, logPageSize], fetchLogs)
watch([searchLogUserId, () => logDateRange.value.start, () => logDateRange.value.end], () => {
    logCurrentPage.value = 1
    fetchLogs()
})

// Initial Fetch invoked when users/schemes loaded
watch(() => [users.value.length, schemes.value.length], ([uLen, sLen]) => {
    if (uLen > 0 || sLen > 0) fetchLogs()
}, { immediate: true })
const scales = computed(() => dataStore.scales ?? [])
const surveyResponses = computed(() => dataStore.surveyResponses ?? [])
const surveyTemplate = computed(() => dataStore.surveyTemplate ?? [])

const currentUserLogs = computed(() => {
  const allLogs = Array.isArray(logs.value) ? logs.value : []
  if (!viewLogsUserId.value) return []
  return allLogs.filter(l => l.user_id === viewLogsUserId.value).sort((a, b) => (b.id ?? 0) - (a.id ?? 0))
})

const viewingUser = computed(() => {
  if (!viewLogsUserId.value) return null
  return users.value.find(u => String(u.subjectCode) === String(viewLogsUserId.value) || String(u.id) === String(viewLogsUserId.value))
})

// Scales Management
const activeScaleId = ref(null)
const activeScale = computed(() => scales.value.find(s => s.id === activeScaleId.value) || null)

// Set initial active scale
watch(() => scales.value, (newScales) => {
  if (!activeScaleId.value && newScales.length > 0) {
    activeScaleId.value = newScales[0].id
  }
}, { immediate: true })

// Sync editingSurvey with activeScale
watch(activeScaleId, async (newId) => {
  if (!newId) return
  if (String(newId).startsWith('temp_')) {
    // 对于本地临时量表，直接从本地缓存中查找
    const scale = scales.value.find(s => s.id === newId)
    if (scale && scale.questions) {
      editingSurvey.value = JSON.parse(JSON.stringify(scale.questions))
    } else {
      editingSurvey.value = []
    }
  } else {
    // 对于从数据库获取的量表，调用API获取最新数据
    try {
      // 注意：这里需要添加一个获取单个量表详情的API函数
      // 假设API函数名为getTemplate
      const scaleDetail = await getTemplate(newId)
      if (scaleDetail && scaleDetail.questions) {
        // 更新本地缓存
        dataStore.updateScale(newId, { questions: scaleDetail.questions })
        // 更新editingSurvey，需要进行后端数据 -> 前端格式的转换
        editingSurvey.value = scaleDetail.questions.map(q => {
          let frontendType = 'text'
          let optionsStr = ''
          let minVal = 0
          let maxVal = 10
          
          // 映射类型
          if (q.type === 'SCALE') frontendType = 'range'
          else if (q.type === 'SINGLE' || q.type === 'MULTI') frontendType = 'select'
          else frontendType = 'text'
          
          // 处理选项和范围
          if (frontendType === 'select') {
            if (Array.isArray(q.options)) {
              optionsStr = q.options.join(',')
            }
          } else if (frontendType === 'range') {
            if (Array.isArray(q.options) && q.options.length > 0) {
              const nums = q.options.map(Number).filter(n => !isNaN(n))
              if (nums.length > 0) {
                minVal = Math.min(...nums)
                maxVal = Math.max(...nums)
              }
            }
          }
          
          return {
            id: q.questionNo || `q_${Date.now()}_${Math.random()}`,
            type: frontendType,
            label: q.title || '',
            required: q.required === undefined ? true : q.required,
            options: optionsStr,
            min: minVal,
            max: maxVal
          }
        })
      } else {
        editingSurvey.value = []
      }
    } catch (error) {
      console.error('获取量表详情失败:', error)
      // 如果API调用失败，回退到本地缓存（本地缓存可能存储的是原生的后端格式，也可能没有）
      const scale = scales.value.find(s => s.id === newId)
      // 简单防错，如果本地也没有，就置空
      editingSurvey.value = []
    }
  }
}, { immediate: true })

// Auto-save editingSurvey to dataStore when questions change
watch(editingSurvey, (newQuestions) => {
  if (activeScaleId.value) {
    dataStore.updateScale(activeScaleId.value, { questions: JSON.parse(JSON.stringify(newQuestions)) })
  }
}, { deep: true })

const handleAddScale = () => {
  // Create local scale without API call
  const newScale = {
    id: `temp_${Date.now().toString()}`, // Local ID for new scales
    name: '新建量表',
    description: '新量表描述',
    questions: []
  }
  
  // Directly modify the scales array to preserve the temp ID
  dataStore.scales.push(newScale)
  activeScaleId.value = newScale.id
  
  console.log('✅ 新建量表已添加到本地:', newScale)
}

const handleRemoveScale = async (id) => {
  if (scales.value.length <= 1) return alert('至少保留一个量表')
  if (confirm('确定删除此量表吗？')) {
    try {
      // 调用删除量表API
      // 注意：survey.js中没有导出deleteTemplate函数，需要先检查是否存在
      // await deleteTemplate(id)      
      await deleteTemplate(id)
      alert('删除成功')
      
      // 从本地数据中移除量表
      dataStore.deleteScale(id)
      
      // 如果删除的是当前激活的量表，重置activeScaleId
      if (activeScaleId.value === id) {
        activeScaleId.value = scales.value.length > 0 ? scales.value[0].id : null
      }
    } catch (error) {
      console.error('删除量表失败:', error)
      alert(`删除量表失败: ${error.message}`)
    }
  }
}

const handleSaveCurrentScale = async () => {
  if (!activeScale.value) return
  
  try {
    console.log('✅ 开始保存当前量表:', activeScaleId.value)
    console.log('当前editingSurvey内容:', editingSurvey.value)
    // 1. 先将editingSurvey中的修改保存到dataStore.scales数组中
    dataStore.updateScale(activeScaleId.value, { questions: JSON.parse(JSON.stringify(editingSurvey.value)) })
    
    // 2. 转换问题格式，两种情况都需要
    const questions = editingSurvey.value.map((q, index) => {
      // 转换问题类型：前端 -> 后端
      let backendType
      switch(q.type) {
        case 'range':
          backendType = 'SCALE'
          break
        case 'select':
          backendType = 'SINGLE'
          break
        case 'text':
          backendType = 'TEXT'
          break
        default:
          backendType = 'TEXT'
      }
      
      // 转换选项格式：后端期望 options 数组 (List<Object>)
      let optionsArray = null
      if (q.type === 'select') {
        // 确保q.options是字符串类型
        if (typeof q.options === 'string' && q.options.trim() !== '') {
          // split string into array
          optionsArray = q.options.split(',').map(opt => opt.trim())
        } else {
          optionsArray = []
        }
      } else if (q.type === 'range') {
        // 评分题：生成 [min, min+1, ..., max] 的数组
        const min = parseInt(q.min) || 0
        const max = parseInt(q.max) || 10
        optionsArray = []
        if (min <= max) {
            for (let i = min; i <= max; i++) {
                optionsArray.push(i)
            }
        }
      }
      
      return {
        questionNo: index + 1, // 问题序号从1开始
        type: backendType,
        title: q.label, // 前端使用label，后端期望title
        required: q.required === undefined ? true : q.required,
        options: optionsArray // 后端期望 options 字段，类型为 List<Object>
      }
    })
    
    let response
    if (String(activeScaleId.value).startsWith('temp_')) {
      // New scale - 使用SaveTemplateReq格式
      const saveTemplateData = {
        // ===== 量表相关字段 =====
        scaleCode: `SCALE_${Date.now()}`, // 生成唯一量表编码
        scaleName: activeScale.value.name,
        scaleDescription: activeScale.value.description,
        
        // ===== 模板相关字段 =====
        templateName: activeScale.value.name,
        version: 1, // 默认版本1
        templateDescription: activeScale.value.description,
        
        // ===== 问题 =====
        questions: questions
      }
      
      console.log('📤 保存量表数据（创建）:', saveTemplateData)
      console.log('📤 调用 createTemplate API')
      response = await createTemplate(saveTemplateData)
    } else {
      // Existing scale - 使用UpdateTemplateReq格式
      const updateTemplateData = {
        name: activeScale.value.name,
        description: activeScale.value.description,
        status: 'ACTIVE', // 默认激活状态
        questions: questions
      }
      
      console.log('📤 保存量表数据（更新）:', updateTemplateData)
      console.log('📤 调用 updateTemplate API，ID:', activeScaleId.value)
      response = await updateTemplate(activeScaleId.value, updateTemplateData)
    }
    
    console.log('✅ 保存量表成功，响应:', response)
    await fetchScales()
    
    // 保存成功后，更新活动量表ID
    if (String(activeScaleId.value).startsWith('temp_')) {
      // 对于新建的量表，查找API返回的新量表ID
      if (response && response.id) {
        // 如果API返回了新量表的ID，直接使用该ID
        activeScaleId.value = response.id
      } else {
        // 如果API没有返回ID，查找名称和描述匹配的最新量表
        const newScale = dataStore.scales.find(scale => 
          scale.name === activeScale.value.name && 
          scale.description === activeScale.value.description
        )
        if (newScale) {
          activeScaleId.value = newScale.id
        } else if (dataStore.scales.length > 0) {
          // 如果找不到，设置为第一个量表
          activeScaleId.value = dataStore.scales[0].id
        }
      }
    } else {
      // 对于已存在的量表，检查ID是否仍然存在
      const scaleExists = dataStore.scales.some(scale => scale.id === activeScaleId.value)
      if (!scaleExists && dataStore.scales.length > 0) {
        // 如果不存在，设置为第一个量表
        activeScaleId.value = dataStore.scales[0].id
      }
    }
    
    alert('量表配置已保存')
  } catch (error) {
    console.error('❌ 保存量表失败:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status,
      headers: error.response?.headers
    })
    alert(`保存量表失败: ${error.response?.data?.message || error.message}`)
  }
}

// Group Management with Scale Linking
const newGroupScaleIds = ref([]) // ken260129-修改内容：将单个量表ID改为量表ID列表
const newGroupDeviceIds = ref([])

const handleAddGroup = async () => {
  if (!newGroupName.value) return alert("请输入分组名称")
  if (newGroupScaleIds.value.length === 0) return alert("请选择关联量表")
  
  try {
    await createGroup({
      name: newGroupName.value,
      surveyTemplateIds: newGroupScaleIds.value.map(id => parseInt(id)), // ken260129-修改内容：使用surveyTemplateIds字段传递多个量表ID
      deviceIds: newGroupDeviceIds.value.length > 0 ? newGroupDeviceIds.value.map(id => parseInt(id)) : undefined
    })
    await fetchGroups()
    newGroupName.value = ''
    newGroupScaleIds.value = [] // ken260129-修改内容：清空量表ID列表
    newGroupDeviceIds.value = []
    // isGroupModalOpen.value = false // Keep open to add more or close manually
  } catch (error) {
    console.error('创建分组失败:', error)
    alert(`创建分组失败: ${error.message}`)
  }
}

const getScaleName = (scaleId) => {
  console.log('scaleId:', scaleId)
  console.log('scales.value:', scales.value)
  const s = scales.value.find(s => s.id === scaleId)
  return s ? s.name : '未知量表'
}

const handleEditUser = (user) => {
  editingUserId.value = user.id // Subject ID
  isEditingUser.value = true
  // Map Subject List DTO to Form
  userForm.value = { 
    id: user.subjectCode,
    subjectId: user.id,
    password: '',
    gender: user.gender,
    age: user.age,
    group_name: user.groupName,
    subjectName: user.subjectName || '', 
    contactInfo: user.contactInfo || '',
    enrollmentDate: user.enrollmentDate || '',
    diagnosisResult: user.diagnosisResult || ''
  }
  isUserModalOpen.value = true
}

const handleDeleteScheme = async (id) => {
  if (!confirm('确定要删除该方案吗？')) return
  try {
    await deleteScheme(id)
    await fetchSchemes()
    alert('删除成功')
  } catch (e) {
    console.error(e)
    alert('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

const handleDeleteSubject = async (id) => {
  if(!confirm('确定要删除该受试者吗？')) return
  try {
    await deleteSubject(id)
    await fetchUsers()
    alert('删除成功')
  } catch (e) {
    console.error(e)
    alert('删除失败')
  }
}

const handleSaveUser = async () => {
  if (!userForm.value.id) return alert("ID必填")
  if (!userForm.value.group_name) return alert("请选择分组")
  
  try {
    if (isEditingUser.value) {
      const selectedGroup = groups.value.find(g => g.name === userForm.value.group_name)
      await updateSubject(editingUserId.value, {
        subjectCode: userForm.value.id,
        gender: userForm.value.gender,
        age: userForm.value.age,
        groupId: selectedGroup ? selectedGroup.id : null,
        subjectName: userForm.value.subjectName,
        contactInfo: userForm.value.contactInfo,
        enrollmentDate: userForm.value.enrollmentDate,
        diagnosisResult: userForm.value.diagnosisResult
      })
    } else {
      if (!userForm.value.password) return alert("密码必填")
      
      // Find Group ID
      const selectedGroup = groups.value.find(g => g.name === userForm.value.group_name)
      if (!selectedGroup) return alert("无效的分组")

      await createSubject({
        username: userForm.value.id,
        password: userForm.value.password,
        subjectCode: userForm.value.id, // 使用ID作为编号
        displayName: userForm.value.subjectName || userForm.value.id,
        gender: userForm.value.gender,
        age: userForm.value.age,
        groupId: selectedGroup.id,
        subjectName: userForm.value.subjectName,
        contactInfo: userForm.value.contactInfo,
        enrollmentDate: userForm.value.enrollmentDate,
        diagnosisResult: userForm.value.diagnosisResult
      })
    }
    await fetchUsers()
    
    isUserModalOpen.value = false
    isEditingUser.value = false
    editingUserId.value = null
    userForm.value = { 
      id: '', 
      password: '', 
      gender: '男', 
      age: 30, 
      group_name: groups.value[0]?.name || '', 
      subjectName: '', 
      contactInfo: '', 
      enrollmentDate: '', 
      diagnosisResult: '' 
    }
  } catch (error) {
    alert(`保存失败: ${error.message}`)
  }
}

const handleEditScheme = async (scheme) => {
  editingSchemeId.value = scheme.id
  isEditingScheme.value = true
  
  // 确保设备数据已加载
  if (devices.value.length === 0) {
    await fetchDevices()
  }
  
  // 转换阶段数据，特别处理设备配置
    const mappedStages = (scheme.stages || []).map(s => {
      // 初始化设备配置对象
      const devicesConfig = {}
      // 保存设备配置的id映射，key为deviceSn
      const deviceConfigIdMap = {}
      
      // 处理后端返回的deviceConfigs，转换为前端需要的devices格式
      if (s.deviceConfigs && s.deviceConfigs.length > 0) {
        // 遍历后端返回的deviceConfigs
        s.deviceConfigs.forEach(config => {
          // 保存设备配置的id映射
          deviceConfigIdMap[config.deviceSn] = config.id
          
          // 查找对应的设备
          const device = devices.value.find(d => d.deviceSn === config.deviceSn || String(d.id) === config.deviceSn)
          if (device) {
            const deviceIdStr = String(device.id)
            // 根据设备类型设置不同的配置项
            if (device.deviceType === DEVICE_TYPES.TYPE_485 || device.deviceType === DEVICE_TYPES.MCB) {
              devicesConfig[deviceIdStr] = {
                id: config.id, // 保存设备配置的id
                sun_brightness: config.lightIntensity || 50,
                sky_brightness: config.skyLightIntensity || 40,
                temp: config.lightColorTemp || 4500
              }
            } else {
              devicesConfig[deviceIdStr] = {
                id: config.id, // 保存设备配置的id
                brightness: config.lightIntensity || 50,
                temp: config.lightColorTemp || 4500
              }
            }
          }
        })
      }
      
      return {
        duration: s.durationMinutes,
        brightness: s.lightIntensity,
        temp: s.lightColorTemp,
        devices: devicesConfig
      }
    })
  
  schemeForm.value = { 
    name: scheme.name,
    description: scheme.description,
    target_group: '',
    stages: mappedStages
  }
  
  // Try to match target_group by ID references
  // 对于GLOBAL方案，不应该有组信息，默认显示"无（通用）"
  if (scheme.scope === 'GLOBAL') {
    // 全局方案，设置target_group为空，显示"无（通用）"
    schemeForm.value.target_group = ''
  } else if (scheme.groupIds && scheme.groupIds.length > 0) {
     // 使用组ID作为值，转换为字符串以匹配表单字段类型
     schemeForm.value.target_group = String(scheme.groupIds[0])
  } else if (scheme.groupId) {
     // 使用组ID作为值，转换为字符串以匹配表单字段类型
     schemeForm.value.target_group = String(scheme.groupId)
  } else if (scheme.target_group) {
     // 如果后端返回的是组名，尝试查找对应的组ID
     const g = groups.value.find(grp => grp.name === scheme.target_group)
     if (g) schemeForm.value.target_group = String(g.id)
  }
  
  // 确保设备数据已加载
  if (devices.value.length === 0) {
    await fetchDevices()
  }
  
  isSchemeModalOpen.value = true
}

const handleEndTreatment = async (log) => {
  try {
    if (confirm(`确定要结束受试者 ${log.user_id} 的治疗吗？`)) {
      await endTreatment(log.id, {
        status: '手动结束'
      })
      // 刷新日志列表
      fetchLogs()
      alert('治疗已成功结束')
    }
  } catch (error) {
    console.error('结束治疗失败:', error)
    alert(`结束治疗失败: ${error.message}`)
  }
}

// 新建方案
const handleNewScheme = async () => {
  // 重置schemeForm
  schemeForm.value = {
    name: '',
    description: '',
    target_group: '',
    stages: []
  }
  
  // 重置编辑状态
  editingSchemeId.value = null
  isEditingScheme.value = false
  
  // 确保设备数据已加载
  if (devices.value.length === 0) {
    await fetchDevices()
  }
  
  // 添加一个默认阶段
  addStage()
  
  // 打开模态框
  isSchemeModalOpen.value = true
}

// 更新阶段时长
const handleStageDurationChange = (idx, duration) => {
  const newStages = [...schemeForm.value.stages]
  newStages[idx].duration = parseInt(duration)
  schemeForm.value.stages = newStages
}

// 更新设备配置
const handleDeviceConfigChange = (stageIdx, deviceId, configKey, value) => {
  const newStages = [...schemeForm.value.stages]
  // 确保devices对象存在
  if (!newStages[stageIdx].devices) {
    newStages[stageIdx].devices = {}
  }
  if (!newStages[stageIdx].devices[deviceId]) {
    newStages[stageIdx].devices[deviceId] = {} 
  }
  
  // 处理空值或无效值
  const processedValue = value === null || isNaN(value) ? null : Number(value)
  
  newStages[stageIdx].devices[deviceId][configKey] = processedValue
  schemeForm.value.stages = newStages
}

const addStage = () => {
  // 为新阶段创建默认设备配置
  const defaultDevices = {}
  filteredDevices.value.forEach(device => {
    const deviceIdStr = String(device.id)
    if (device.deviceType === DEVICE_TYPES.TYPE_485 || device.deviceType === DEVICE_TYPES.MCB) {
      defaultDevices[deviceIdStr] = { sun_brightness: 50, sky_brightness: 40, temp: 4500 }
    } else {
      defaultDevices[deviceIdStr] = { brightness: 50, temp: 4500 }
    }
  })
  
  console.log('addStage: defaultDevices created:', defaultDevices)
  console.log('addStage: filteredDevices length:', filteredDevices.value.length)
  
  schemeForm.value.stages.push({ 
    duration: 10, // 阶段总时长
    devices: defaultDevices 
  })
}

const removeStage = (idx) => {
  if (schemeForm.value.stages.length === 1) return alert("至少保留一个阶段")
  schemeForm.value.stages = schemeForm.value.stages.filter((_, i) => i !== idx)
}

// 执行场景函数
const handleExecuteScenario = async (stageIdx) => {
  try {
    // 记录开始时间
    const startTime = new Date()
    console.log(`[执行场景] 开始执行阶段 ${stageIdx + 1}，时间：${startTime.toISOString()}`)
    
    // 获取当前阶段配置
    const stage = schemeForm.value.stages[stageIdx]
    if (!stage) {
      throw new Error('阶段配置不存在')
    }
    
    // 构建设备控制参数
    const deviceControls = []
    const deviceIds = new Set()
    
    // 遍历所有设备，为每个设备创建控制参数
    filteredDevices.value.forEach(device => {
      // 去重处理
      if (deviceIds.has(device.id)) {
        console.warn('重复设备已跳过:', device.id, device.deviceName || device.deviceSn)
        return
      }
      deviceIds.add(device.id)
      
      // 获取设备配置
      let brightness, temp, sunBrightness, skyBrightness
      
      if (device.deviceType === DEVICE_TYPES.TYPE_485 || device.deviceType === DEVICE_TYPES.MCB) {
        brightness = stage.devices?.[device.id]?.sun_brightness || 50
        skyBrightness = stage.devices?.[device.id]?.sky_brightness || 40
        temp = stage.devices?.[device.id]?.temp || 4500
      } else {
        brightness = stage.devices?.[device.id]?.brightness || 50
        temp = stage.devices?.[device.id]?.temp || 4500
      }
      
      // 构建控制参数
      const control = {
        deviceSn: device.deviceSn || device.sn || device.id.toString(),
        dim: brightness,
        cctK: temp
      }
      
      // 对于485或MCB设备，添加天空亮度
      if (device.deviceType === DEVICE_TYPES.TYPE_485 || device.deviceType === DEVICE_TYPES.MCB) {
        control.skyDim = skyBrightness
      }
      
      deviceControls.push(control)
    })
    
    // 构建请求参数（移除subjectId）
    const requestParams = {
      deviceControls: deviceControls
    }
    
    console.log(`[执行场景] 请求参数：`, requestParams)
    
    // 调用研究者专用API
    const response = await researcherExecuteScenario(requestParams)
    
    // 记录响应结果
    console.log(`[执行场景] 响应结果：`, response)
    console.log(`[执行场景] 执行成功，耗时：${(new Date() - startTime) / 1000}秒`)
    
    // 显示成功提示
    alert('执行场景成功')
    
  } catch (error) {
    // 记录错误
    console.error(`[执行场景] 执行失败：`, error)
    
    // 显示错误提示
    alert(`执行场景失败：${error.message || '未知错误'}`)
    
  } finally {
    // 记录结束时间
    console.log(`[执行场景] 执行结束，时间：${new Date().toISOString()}`)
  }
}

const handleSaveScheme = async () => {
  if (!schemeForm.value.name) return alert("方案名称必填")
  
  try {
    const totalDuration = schemeForm.value.stages.reduce((acc, curr) => acc + Number(curr.duration), 0)
    
        console.log("schemeForm.value.stages", schemeForm.value.stages)
    // Convert stages to backend format
    const mappedStages = schemeForm.value.stages.map((s, index) => {
      // 将设备配置转换为后端格式
      const deviceConfigs = []
      
      console.log('Processing stage', index + 1, 'with devices:', s.devices)
      console.log('filteredDevices.value:', filteredDevices.value.map(d => ({ id: d.id, name: d.deviceName })))
      
      // 遍历所有相关设备，确保每个deviceId只出现一次
      const deviceMap = new Map()
      filteredDevices.value.forEach(device => {
        // 使用deviceId作为唯一键，确保每个deviceId只出现一次
        deviceMap.set(device.id, device)
      })
      
      const uniqueDevices = Array.from(deviceMap.values())
      
      // 创建一个Map来确保每个deviceId只出现一次
      const deviceIdMap = new Map()
      
      uniqueDevices.forEach(device => {
        const deviceIdStr = String(device.id)
        const config = s.devices ? s.devices[deviceIdStr] : null
        
        console.log('Processing device:', deviceIdStr, 'with config:', config, 'deviceSn:', device.deviceSn)
        
        // 确保deviceSn不为空
        if (!device.deviceSn) {
          console.error('Device has no deviceSn:', device)
          return
        }
        
        // 确保每个deviceId只出现一次
        if (deviceIdMap.has(device.id)) {
          console.error('Duplicate deviceId in stage:', device.id)
          return
        }
        deviceIdMap.set(device.id, true)
        
        // 为485设备创建特殊配置
        if (device.deviceType === DEVICE_TYPES.TYPE_485 || device.deviceType === DEVICE_TYPES.MCB) {
          deviceConfigs.push({
            id: config?.id, // 包含设备配置的id
            deviceSn: device.deviceSn,
            lightIntensity: config?.sun_brightness || 50,
            skyLightIntensity: config?.sky_brightness || 40,
            lightColorTemp: config?.temp || 4500
          })
        } else {
          // 普通设备配置
          deviceConfigs.push({
            id: config?.id, // 包含设备配置的id
            deviceSn: device.deviceSn,
            lightIntensity: config?.brightness || 50,
            lightColorTemp: config?.temp || 4500
          })
        }
      })
      
      console.log('Generated deviceConfigs:', deviceConfigs)
      
      return {
        stageNo: index + 1,
        name: `阶段${index + 1}`, // Default name
        durationMinutes: Math.max(parseInt(s.duration) || 0, 1), // Ensure at least 1 minute
        lightIntensity: s.brightness || 50, // 前端的brightness对应后端的lightIntensity
        lightColorTemp: s.temp || 4500, // 前端的temp对应后端的lightColorTemp
        description: '',
        deviceConfigs: deviceConfigs // 包含所有设备配置
      }
    })

    // Get Target Group ID (assuming single selection for now, though backend supports list)
    // If schemeForm.target_group is a name, find ID. If not set, keep empty for GLOBAL schemes
    let targetGroupIds = []
    if (schemeForm.value.target_group) {
        const g = groups.value.find(grp => grp.name === schemeForm.value.target_group || grp.id === schemeForm.value.target_group)
        if (g) targetGroupIds.push(Number(g.id)) // Convert to number type to match backend Long
    }
    // Remove duplicates to avoid constraint violation
    targetGroupIds = [...new Set(targetGroupIds)]

    if (isEditingScheme.value) {
      // 使用完整更新接口，更新所有信息
      await updateSchemeWithStages(editingSchemeId.value, {
        name: schemeForm.value.name,
        description: schemeForm.value.description,
        status: 'ACTIVE', // 添加状态字段
        groupId: targetGroupIds,
        stages: mappedStages
      })
    } else {
      await createSchemeWithStages({
        name: schemeForm.value.name,
        description: schemeForm.value.description,
        groupId: targetGroupIds,
        stages: mappedStages
      })
    }
    
    await fetchSchemes()
    
    isSchemeModalOpen.value = false
    isEditingScheme.value = false
    editingSchemeId.value = null
    schemeForm.value = { 
      name: '', 
      description: '', 
      stages: [{ brightness: 50, temp: 4500, duration: 30 }] 
    }
  } catch (error) {
    console.error('保存方案失败:', error)
    alert(`保存方案失败: ${error.message}`)
  }
}



const addQuestion = () => {
  const newQ = { 
    id: `q${Date.now()}`, 
    type: newQuestionType.value, 
    label: '新问题标题', 
    required: true,
    ...(newQuestionType.value === 'range' ? { min: 1, max: 10 } : {}),
    ...(newQuestionType.value === 'select' ? { options: ['选项A', '选项B'] } : {})
  }
  editingSurvey.value.push(newQ)
}

const removeQuestion = (id) => {
  editingSurvey.value = editingSurvey.value.filter(q => q.id !== id)
}

const updateQuestion = (id, field, val) => {
  editingSurvey.value = editingSurvey.value.map(q => 
    q.id === id ? { ...q, [field]: val } : q
  )
}

const handleSaveSurvey = () => {
  dataStore.updateSurvey(editingSurvey.value)
  alert("问卷配置已更新，下次受试者治疗时生效。")
}

const handleExportUserLogs = (userId) => {
  const userLogs = logs.value.filter(l => l.user_id === userId)
  if (userLogs.length === 0) {
    alert(`受试者 ${userId} 暂无诊疗记录。`)
    return
  }
  const userScales = scales.value.filter(s => s.user_id === userId)
  downloadMergedCSV(userLogs, userScales, `${userId}_诊疗与反馈记录`)
}

const handleExportAllLogs = async () => {
  try {
     const start = logDateRange.value?.start ? new Date(logDateRange.value.start).toISOString().split('T')[0] : null
     const end = logDateRange.value?.end ? new Date(logDateRange.value.end).toISOString().split('T')[0] : null
     
     // Fetch all rows
     const res = await listTreatments(null, null, start, end, 0, 10000)
     if (!res || !res.content || res.content.length === 0) {
        alert("暂无诊疗记录可导出")
        return
     }
     
     const mapped = res.content.map(mapSessionToLog)
     downloadTreatmentRecordsXLSX(mapped, `所有受试者诊疗与反馈记录_${new Date().toISOString().split('T')[0]}`)
  } catch (e) {
     console.error(e)
     alert("导出失败")
  }
}

const handleExportClinicalReport = (userId) => {
  const user = users.value.find(u => u.id === userId)
  if (!user) return alert("未找到该受试者信息")
  
  const userLogs = logs.value.filter(l => l.user_id === userId)
  const userScales = surveyResponses.value.filter(s => s.user_id === userId)
  
  if (userLogs.length === 0) {
    alert(`受试者 ${userId} 暂无诊疗记录，无法生成临床报告`)
    return
  }
  
  let htmlContent = `
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
      <meta charset="UTF-8">
      <title>临床报告 - ${user.id}</title>
      <style>
        body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }
        h1 { text-align: center; color: #2c3e50; }
        h2 { color: #34495e; border-bottom: 1px solid #eee; padding-bottom: 5px; }
        .section { margin: 20px 0; }
        .info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin: 15px 0; }
        .info-item { padding: 10px; background: #f8f9fa; border-radius: 5px; }
        .info-label { font-weight: bold; color: #666; }
        table { width: 100%; border-collapse: collapse; margin: 15px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background: #f2f2f2; }
        tr:nth-child(even) { background: #f9f9f9; }
        .report-header { text-align: center; margin-bottom: 30px; }
        .report-date { color: #666; font-style: italic; }
      </style>
    </head>
    <body>
      <div class="report-header">
        <h1>杨浦区精神卫生中心 · 光疗临床报告</h1>
        <p class="report-date">生成日期：${new Date().toLocaleString()}</p>
      </div>
      
      <div class="section">
        <h2>一、受试者基本信息</h2>
        <div class="info-grid">
          <div class="info-item"><span class="info-label">受试者ID：</span>${user.id}</div>
          <div class="info-item"><span class="info-label">性别：</span>${user.gender}</div>
          <div class="info-item"><span class="info-label">年龄：</span>${user.age}岁</div>
          <div class="info-item"><span class="info-label">实验分组：</span>${user.group_name}</div>
          <div class="info-item"><span class="info-label">主试姓名：</span>${user.subjectName || '未填写'}</div>
          <div class="info-item"><span class="info-label">联系方式：</span>${user.contactInfo || '未填写'}</div>
          <div class="info-item"><span class="info-label">入组时间：</span>${user.enrollmentDate || '未填写'}</div>
          <div class="info-item"><span class="info-label">诊断结果：</span>${user.diagnosisResult || '未填写'}</div>
          <div class="info-item"><span class="info-label">创建时间：</span>${user.created_at}</div>
          <div class="info-item"><span class="info-label">最后活跃：</span>${user.last_active}</div>
        </div>
      </div>
      
      <div class="section">
        <h2>二、治疗记录统计</h2>
        <div class="info-grid">
          <div class="info-item"><span class="info-label">总治疗次数：</span>${userLogs.length}次</div>
          <div class="info-item"><span class="info-label">总治疗时长：</span>${userLogs.reduce((sum, log) => sum + log.duration_actual, 0)}分钟</div>
          <div class="info-item"><span class="info-label">首次治疗：</span>${userLogs[userLogs.length - 1]?.date || '无'}</div>
          <div class="info-item"><span class="info-label">最近治疗：</span>${userLogs[0]?.date || '无'}</div>
        </div>
      </div>
      
      <div class="section">
        <h2>三、详细治疗记录</h2>
        <table>
          <thead>
            <tr>
              <th>日期</th>
              <th>时间范围</th>
              <th>治疗方案</th>
              <th>实际时长</th>
              <th>治疗状态</th>
            </tr>
          </thead>
          <tbody>
            ${userLogs.map(log => `
              <tr>
                <td>${log.date}</td>
                <td>${log.start_time} - ${log.end_time}</td>
                <td>${log.scheme_name}</td>
                <td>${log.duration_actual}分钟</td>
                <td>${log.status}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
      
      <div class="section">
        <h2>四、量表评估结果</h2>
        ${userScales.length > 0 
          ? userScales.map((scale, index) => {
              const relatedLog = userLogs.find(log => log.id === scale.treatment_log_id)
              return `
                <div style="margin: 15px 0; padding: 15px; background: #f8f9fa; border-radius: 5px;">
                  <h3 style="margin-top: 0; color: #3498db;">评估 ${index + 1} - ${relatedLog?.date || scale.date}</h3>
                  <table style="width: 100%; margin: 10px 0;">
                    <thead>
                      <tr>
                        <th>评估项目</th>
                        <th>得分</th>
                      </tr>
                    </thead>
                    <tbody>
                      ${scale.answers 
                        ? Object.entries(scale.answers).map(([key, val]) => `
                          <tr>
                            <td>${key}</td>
                            <td>${val}</td>
                          </tr>
                        `).join('')
                        : '<tr><td colspan="2">无数据</td></tr>'
                      }
                    </tbody>
                  </table>
                </div>
              `
            }).join('')
          : '<p style="color: #666; text-align: center; padding: 20px;">暂无量表评估数据</p>'
        }
      </div>
      
      <div class="section">
        <h2>五、总结</h2>
        <p>受试者 ${user.id} 共完成 ${userLogs.length} 次光疗治疗，总时长 ${userLogs.reduce((sum, log) => sum + log.duration_actual, 0)} 分钟。</p>
        <p>详细治疗记录和评估结果见上文。</p>
      </div>
      
      <div style="margin-top: 50px; text-align: center; color: #999; font-size: 12px;">
        <p>本报告由杨浦区精神卫生中心光疗系统自动生成</p>
      </div>
    </body>
    </html>
  `
  
  const blob = new Blob([htmlContent], { type: 'text/html' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${user.id}_临床报告_${new Date().toISOString().split('T')[0]}.html`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  
  alert(`临床报告导出成功，文件名：${user.id}_临床报告_${new Date().toISOString().split('T')[0]}.html`)
}
const getDeviceName = (deviceId) => {
  const device = devices.value.find(d => String(d.id) === String(deviceId))
  return device ? device.deviceName : `设备 ${deviceId}`
}
const getDeviceNameBySn = (deviceSn) => {
  // 确保设备数据已加载
  if (devices.value.length === 0) {
    return `设备 ${deviceSn}`
  }
  const device = devices.value.find(d => String(d.deviceSn) === String(deviceSn))
  return device ? device.deviceName : `设备 ${deviceSn}`
}
const isCeilingLight = (deviceConfig) => {
  // 优先根据设备配置判断
  if (deviceConfig.skyLightIntensity === undefined || deviceConfig.skyLightIntensity === null) {
    return true
  }
  // 其次根据设备名称判断
  const deviceName = getDeviceNameBySn(deviceConfig.deviceSn)
  return deviceName.includes('天花') || deviceName.includes('Ceiling')
}
const isSkyLight = (deviceConfig) => {
  // 优先根据设备配置判断
  if (deviceConfig.skyLightIntensity !== undefined && deviceConfig.skyLightIntensity !== null) {
    return true
  }
  // 其次根据设备名称判断
  const deviceName = getDeviceNameBySn(deviceConfig.deviceSn)
  return deviceName.includes('天窗') || deviceName.includes('Sky')
}
// 直接返回百分比值，不再转换
const luxToPercentage = (value) => {
  const percentage = Number(value)
  return Math.min(100, Math.max(0, isNaN(percentage) ? 0 : percentage))
}
const percentageToLux = (percentage) => {
  // 直接返回百分比值，不再转换
  return Number(percentage)
}

const handleFileImport = (e) => {
  const file = e.target.files[0]
  if (!file) return
  if (file.type !== 'application/json' && !file.name.endsWith('.json')) {
    return alert("请选择 JSON 格式的备份文件")
  }
  
  const reader = new FileReader()
  reader.onload = (evt) => {
    try {
      const importedData = JSON.parse(evt.target.result)
      const requiredFields = ['users', 'schemes', 'groups', 'logs', 'scales', 'surveyTemplate']
      const hasRequiredFields = requiredFields.some(field => field in importedData)
      
      if (!hasRequiredFields) {
        return alert("备份文件格式不正确，缺少必要的数据字段")
      }
      
      dataStore.importDb(importedData)
      alert("数据库导入成功！页面将刷新以应用新数据。")
      window.location.reload()
    } catch (err) {
      if (err instanceof SyntaxError) {
        alert("JSON 解析失败，请检查文件格式是否正确")
      } else {
        alert(`导入失败：${err.message}`)
      }
    }
  }
  
  reader.onerror = () => {
    alert("文件读取失败，请检查文件是否损坏")
  }
  
  reader.readAsText(file)
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const closeUserModal = () => {
  isUserModalOpen.value = false
  isEditingUser.value = false
  editingUserId.value = null
  userForm.value = { 
    id: '', 
    password: '', 
    gender: '男', 
    age: 30, 
    group_name: groups.value[0]?.name || '', 
    subjectName: '', 
    contactInfo: '', 
    enrollmentDate: '', 
    diagnosisResult: '' 
  }
}

const closeSchemeModal = () => {
  isSchemeModalOpen.value = false
  isEditingScheme.value = false
  editingSchemeId.value = null
  schemeForm.value = { 
    name: '', 
    description: '', 
    target_group: '',
    stages: [{ brightness: 50, temp: 4500, duration: 30 }] 
  }
}
</script>

<template>
  <div class="h-screen overflow-hidden bg-gray-100 flex flex-col">
    <header class="bg-indigo-900 text-white shadow-md w-full">
      <div class="w-full px-4 h-16 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <Activity class="text-indigo-300" />
          <span class="font-bold text-lg">杨浦区精神卫生中心 · 物联网光照 · 后台管理系统</span>
        </div>
        <div class="flex items-center gap-4">
          <span class="text-xs text-indigo-300">Admin Mode</span>
          <button @click="handleLogout" class="text-sm bg-indigo-800 hover:bg-indigo-700 px-3 py-1 rounded">退出</button>
        </div>
      </div>
    </header>

    <div class="flex flex-1 w-full py-6 px-4 gap-6 overflow-hidden">
      <aside class="w-64 flex-shrink-0 space-y-2">
        <button
          v-for="item in [
            { id: 'users', icon: Users, label: '受试者管理' }, 
            { id: 'records', icon: Clock, label: '治疗记录' },
            { id: 'schemes', icon: Settings, label: '方案配置' }, 
            { id: 'survey', icon: MessageSquare, label: '量表管理' },
            { id: 'spectrum', icon: Activity, label: '光谱仪(CS)' },
            { id: 'data', icon: Database, label: '数据与备份' }
          ]"
          :key="item.id"
          @click="activeTab = item.id"
          :class="[
            'w-full flex items-center gap-3 px-4 py-3 rounded-lg font-medium transition-colors',
            activeTab === item.id ? 'bg-white text-indigo-600 shadow-sm' : 'text-gray-600 hover:bg-white/50'
          ]"
        >
          <component :is="item.icon" :size="20" />
          {{ item.label }}
        </button>
      </aside>

      <main class="flex-1 bg-white rounded-xl shadow-sm p-6 flex flex-col min-h-0 h-full">
        <div v-if="activeTab === 'users'" class="h-full flex flex-col">
          <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-6 flex-none">
            <h2 class="text-xl font-bold text-gray-800">受试者</h2>
            <div class="flex items-center gap-2">
              <div class="relative">
                <Users class="absolute left-3 top-3 text-gray-400" :size="18" />
                <input
                  v-model="searchUserId"
                  type="text"
                  class="pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 ring-blue-500 outline-none"
                  placeholder="搜索用户ID..."
                />
              </div>
              <Button variant="secondary" @click="isGroupModalOpen = true">
                <template #icon><Layers :size="18" /></template>
                管理分组
              </Button>
              <Button @click="isUserModalOpen = true">
                <template #icon><Plus :size="18" /></template>
                新增账户
              </Button>
            </div>
          </div>
          <div class="border rounded-lg overflow-hidden flex-1 min-h-0 flex flex-col">
            <div class="flex-1 overflow-y-auto min-h-0">
              <table class="w-full text-left text-sm">
                <thead class="bg-gray-50 border-b text-gray-500 sticky top-0 z-10">
                  <tr>
                    <th class="p-4">ID</th>
                    <th class="p-4">分组</th>
                    <th class="p-4">性别/年龄</th>
                    <th class="p-4 text-center">操作</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                  <tr v-for="user in paginatedUsers" :key="user.id" class="hover:bg-gray-50">
                    <td class="p-4 font-mono font-medium">{{ user.subjectCode }}</td>
                    <td class="p-4">
                      <span class="bg-indigo-50 text-indigo-700 px-2 py-1 rounded text-xs">{{ user.groupName }}</span>
                    </td>
                    <td class="p-4">{{ user.gender }} / {{ user.age }}</td>
                    <td class="p-4 flex items-center justify-center gap-3">
                      <button @click="handleEditUser(user)" class="text-blue-500 hover:text-blue-700" title="编辑用户信息">
                        <Edit3 :size="18" />
                      </button>
                      <button @click="viewLogsUserId = user.id" class="text-gray-500 hover:text-indigo-600" title="在线查看记录">
                        <Eye :size="18" />
                      </button>

                      <button @click="handleDeleteSubject(user.id)" class="text-red-400 hover:text-red-600" title="删除用户">
                        <Trash2 :size="18" />
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            
            <!-- 分页控件 -->
            <div class="flex items-center justify-between mt-auto px-4 pb-4 pt-4 border-t">
              <div class="text-sm text-gray-500">
                <!-- Safe navigation for length just in case -->
                共 {{ users?.length || 0 }} 条记录，第 {{ userCurrentPage }}/{{ userTotalPages }} 页
              </div>
              <div class="flex items-center gap-2">
                <Button size="sm" variant="secondary" @click="userPrevPage()" :disabled="userCurrentPage === 1">
                  上一页
                </Button>
                <div class="flex items-center gap-1">
                  <template v-for="(page, idx) in visibleUserPages" :key="idx">
                    <span v-if="page === '...'" class="px-2 text-gray-400">...</span>
                    <button
                      v-else
                      @click="goToUserPage(page)"
                      :class="[
                        'px-3 py-1 rounded text-sm transition-colors',
                        userCurrentPage === page ? 'bg-blue-600 text-white' : 'bg-gray-100 hover:bg-gray-200'
                      ]"
                    >
                      {{ page }}
                    </button>
                  </template>
                </div>
                <Button size="sm" variant="secondary" @click="userNextPage()" :disabled="userCurrentPage === userTotalPages">
                  下一页
                </Button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'schemes'" class="h-full flex flex-col">
          <div class="flex justify-between items-center mb-6 flex-none">
            <h2 class="text-xl font-bold text-gray-800">多阶段诊疗方案库</h2>
            <Button @click="handleNewScheme">
              <template #icon><Plus :size="18" /></template>
              新建方案
            </Button>
          </div>
          <div class="flex-1 overflow-y-auto min-h-0 custom-scrollbar pr-2">
            <div class="space-y-4 pb-4">
              <Card v-for="scheme in listschemes" :key="scheme.id" className="p-5 border hover:border-indigo-300 transition-colors">
                <div class="flex justify-between items-start mb-4">
                  <div>
                    <div class="flex items-center gap-2">
                      <h3 class="font-bold text-gray-800 text-lg">{{ scheme.name }}</h3>
                      <span class="bg-gray-100 text-gray-600 px-2 py-1 rounded text-xs">总时长: {{ scheme.stages?.reduce((acc, s) => acc + (s.durationMinutes || 0), 0) }} min</span>
                    </div>
                    <p class="text-gray-500 text-sm mt-1">{{ scheme.description }}</p>
                  </div>
                  <div class="flex gap-2">
                    <button @click="handleEditScheme(scheme)" class="text-blue-500 hover:text-blue-700" title="编辑方案">
                      <Edit3 :size="18" />
                    </button>
                    <button @click="handleDeleteScheme(scheme.id)" class="text-gray-400 hover:text-red-500" title="删除方案">
                      <Trash2 :size="18" />
                    </button>
                  </div>
                </div>
                <div class="bg-gray-50 p-4 rounded-xl">
                  <div class="flex gap-4 overflow-x-auto pb-2 pt-2 px-2">
                    <!-- Ken260122-修改：优化阶段卡片布局，增加宽度和悬停效果 -->
                    <div v-for="(stage, idx) in scheme.stages" :key="idx" class="flex-shrink-0 bg-white border border-gray-200 p-3 rounded-lg shadow-sm w-72 md:w-80 text-left relative transition-transform hover:scale-105 hover:shadow-md">
                      <div class="absolute -top-2 -left-2 w-6 h-6 bg-indigo-600 text-white rounded-full flex items-center justify-center text-xs font-bold">
                        {{ idx + 1 }}
                      </div>
                      <div class="text-indigo-600 font-bold mb-2">{{ stage.durationMinutes }} min</div>
                      <div class="text-xs space-y-2">
                        <!-- 调试信息 -->
                        <div v-if="false" class="bg-yellow-50 p-1 rounded text-red-600">
                          {{ JSON.stringify(stage) }}
                        </div>
                        <!-- Ken260122-修改：添加详细的设备配置信息显示 -->
                        <!-- 设备配置 -->
                        <template v-if="stage.deviceConfigs && stage.deviceConfigs.length > 0">
                          <div v-for="(deviceConfig, deviceIdx) in stage.deviceConfigs" :key="deviceIdx" class="space-y-1">
                            <!-- 识别设备类型 -->
                            <!-- Ken260122-修改：将亮度单位改为百分比，布局改为横向 -->
                            <div v-if="isCeilingLight(deviceConfig)" class="bg-blue-50 p-2 rounded">
                              <div class="font-medium text-blue-700 mb-1">发光天花</div>
                              <div class="flex gap-4 text-gray-600">
                                <div class="flex-1 flex items-center justify-center gap-1">
                                  <div>亮度</div>
                                  <div class="font-medium">{{ luxToPercentage(deviceConfig.lightIntensity) }}%</div>
                                </div>
                                <div class="flex-1 flex items-center justify-center gap-1">
                                  <div>色温</div>
                                  <div class="font-medium">{{ deviceConfig.lightColorTemp }} K</div>
                                </div>
                              </div>
                            </div>
                            <div v-else-if="isSkyLight(deviceConfig)" class="bg-green-50 p-2 rounded">
                              <div class="font-medium text-green-700 mb-1">天窗灯</div>
                              <div class="flex gap-2 text-gray-600 flex-wrap">
                                <div class="flex-1 min-w-[80px] flex items-center justify-center gap-1">
                                  <div>太阳亮度</div>
                                  <div class="font-medium">{{ luxToPercentage(deviceConfig.lightIntensity) }}%</div>
                                </div>
                                <div class="flex-1 min-w-[80px] flex items-center justify-center gap-1">
                                  <div>天空亮度</div>
                                  <div class="font-medium">{{ luxToPercentage(deviceConfig.skyLightIntensity) }}%</div>
                                </div>
                                <div class="flex-1 min-w-[80px] flex items-center justify-center gap-1">
                                  <div>色温</div>
                                  <div class="font-medium">{{ deviceConfig.lightColorTemp }} K</div>
                                </div>
                              </div>
                            </div>
                            <!-- Ken260122-修改：将亮度单位改为百分比 -->
                            <div v-else class="bg-gray-50 p-2 rounded">
                              <div class="font-medium text-gray-700 mb-1">{{ getDeviceNameBySn(deviceConfig.deviceSn) }}</div>
                              <div class="flex gap-2 text-gray-600 flex-wrap">
                                <div v-if="deviceConfig.lightIntensity" class="flex-1 min-w-[80px] flex items-center justify-center gap-1">
                                  <div>亮度</div>
                                  <div class="font-medium">{{ luxToPercentage(deviceConfig.lightIntensity) }}%</div>
                                </div>
                                <div v-if="deviceConfig.skyLightIntensity" class="flex-1 min-w-[80px] flex items-center justify-center gap-1">
                                  <div>天空亮度</div>
                                  <div class="font-medium">{{ luxToPercentage(deviceConfig.skyLightIntensity) }}%</div>
                                </div>
                                <div v-if="deviceConfig.lightColorTemp" class="flex-1 min-w-[80px] flex items-center justify-center gap-1">
                                  <div>色温</div>
                                  <div class="font-medium">{{ deviceConfig.lightColorTemp }} K</div>
                                </div>
                              </div>
                            </div>
                          </div>
                        </template>
                        <!-- 兼容旧数据格式 -->
                        <template v-else>
                          <!-- Ken260122-修改：将亮度单位改为百分比，布局改为横向 -->
                          <!-- 发光天花配置 -->
                          <div v-if="stage.lightIntensity" class="bg-blue-50 p-2 rounded">
                            <div class="font-medium text-blue-700 mb-1">发光天花</div>
                            <div class="flex gap-4 text-gray-600">
                              <div class="flex-1 flex items-center justify-center gap-1">
                                <div>亮度</div>
                                <div class="font-medium">{{ luxToPercentage(stage.lightIntensity) }}%</div>
                              </div>
                              <div class="flex-1 flex items-center justify-center gap-1">
                                <div>色温</div>
                                <div class="font-medium">{{ stage.lightColorTemp }} K</div>
                              </div>
                            </div>
                          </div>
                          <!-- 天窗灯配置 -->
                          <div v-if="stage.skyLightIntensity" class="bg-green-50 p-2 rounded">
                            <div class="font-medium text-green-700 mb-1">天窗灯</div>
                            <div class="flex gap-2 text-gray-600 flex-wrap">
                              <div class="flex-1 min-w-[80px] flex items-center justify-center gap-1">
                                <div>太阳亮度</div>
                                <div class="font-medium">{{ luxToPercentage(stage.lightIntensity) }}%</div>
                              </div>
                              <div class="flex-1 min-w-[80px] flex items-center justify-center gap-1">
                                <div>天空亮度</div>
                                <div class="font-medium">{{ luxToPercentage(stage.skyLightIntensity) }}%</div>
                              </div>
                              <div class="flex-1 min-w-[80px] flex items-center justify-center gap-1">
                                <div>色温</div>
                                <div class="font-medium">{{ stage.lightColorTemp }} K</div>
                              </div>
                            </div>
                          </div>
                          <!-- Ken260122-修改：添加无设备配置信息时的提示 -->
                          <!-- 无设备配置信息 -->
                          <div v-else class="bg-gray-100 p-2 rounded text-gray-500 text-center">
                            无设备配置信息
                          </div>
                        </template>
                      </div>
                    </div>
                  </div>
                </div>
              </Card>
            </div>
          </div>
        </div>
        
        <div v-if="activeTab === 'survey'" class="h-full flex flex-col">
          <div class="flex-none mb-4">
            <h2 class="text-xl font-bold text-gray-800">量表管理</h2>
          </div>
          
          <div class="flex-1 min-h-0 flex gap-6">
            <!-- Left: Scale List -->
            <div class="w-1/4 bg-white border rounded-xl flex flex-col h-full shadow-sm">
              <div class="p-4 border-b flex justify-between items-center bg-gray-50 rounded-t-xl">
                <span class="font-bold text-gray-700">量表列表</span>
                <Button size="sm" @click="handleAddScale">
                  <template #icon><Plus :size="16" /></template>
                  新建
                </Button>
              </div>
              <div class="flex-1 overflow-y-auto p-2 space-y-2">
                <div 
                  v-for="scale in scales" 
                  :key="scale.id"
                  @click="activeScaleId = scale.id"
                  :class="[
                    'p-3 rounded-lg cursor-pointer transition-colors border group relative',
                    activeScaleId === scale.id 
                      ? 'bg-blue-50 border-blue-200 text-blue-700' 
                      : 'hover:bg-gray-50 border-transparent hover:border-gray-200'
                  ]"
                >
                  <div class="font-bold text-sm truncate pr-6">{{ scale.name }}</div>
                  <div class="text-xs text-gray-400 mt-1 truncate">{{ scale.questions?.length || 0 }} 个问题</div>
                  
                  <button 
                    v-if="scales.length > 1"
                    @click.stop="handleRemoveScale(scale.id)" 
                    class="absolute top-3 right-2 text-gray-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <Trash2 :size="14" />
                  </button>
                </div>
              </div>
            </div>

            <!-- Right: Scale Editor -->
            <div class="flex-1 bg-white border rounded-xl flex flex-col h-full shadow-sm overflow-hidden" v-if="activeScale">
              <!-- Header -->
              <div class="p-6 border-b flex justify-between items-start bg-gray-50">
                <div class="flex-1 mr-8 space-y-2">
                  <input 
                    type="text" 
                    :value="activeScale.name" 
                    @input="e => dataStore.updateScale(activeScaleId, { name: e.target.value })"
                    class="text-xl font-bold bg-transparent border-b border-transparent hover:border-gray-300 focus:border-blue-500 focus:outline-none w-full px-1"
                    placeholder="输入量表名称"
                  />
                  <input 
                    type="text" 
                    :value="activeScale.description" 
                    @input="e => dataStore.updateScale(activeScaleId, { description: e.target.value })"
                    class="text-sm text-gray-500 bg-transparent border-b border-transparent hover:border-gray-300 focus:border-blue-500 focus:outline-none w-full px-1"
                    placeholder="添加描述..."
                  />
                </div>
                <Button @click="handleSaveCurrentScale">
                  <template #icon><FileText :size="18" /></template>
                  保存量表
                </Button>
              </div>

              <!-- Question Editor -->
              <div class="flex-1 flex flex-col min-h-0 bg-gray-50/50">
                 <!-- Toolbar -->
                <div class="flex flex-wrap gap-4 items-center p-4 border-b bg-white flex-none">
                  <select v-model="newQuestionType" class="border rounded p-2 text-sm bg-gray-50">
                    <option value="range">评分题 (1-10)</option>
                    <option value="select">选择题</option>
                    <option value="text">文本题</option>
                  </select>
                  <Button size="sm" variant="secondary" @click="addQuestion">
                    <template #icon><Plus :size="16" /></template>
                    添加问题
                  </Button>
                  <div class="ml-auto flex items-center gap-2">
                    <span class="text-xs text-gray-400">题目数: {{ editingSurvey.length }}</span>
                  </div>
                </div>

                <!-- Questions List -->
                <div class="flex-1 overflow-y-auto p-6 space-y-4 custom-scrollbar">
                  <div v-if="editingSurvey.length === 0" class="text-center text-gray-400 py-10 border-2 border-dashed rounded-xl">
                    暂无题目，请点击上方添加
                  </div>
                  <div v-else v-for="(q, idx) in editingSurvey" :key="q.id" class="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex gap-4 items-start group hover:border-blue-200 transition-colors">
                    <div class="bg-gray-100 w-8 h-8 flex items-center justify-center rounded-full font-bold text-gray-500 flex-none group-hover:bg-blue-100 group-hover:text-blue-600 transition-colors">
                      {{ idx + 1 }}
                    </div>
                    <div class="flex-1 space-y-3">
                      <div class="flex gap-4">
                        <input 
                          type="text" 
                          v-model="q.label"
                          class="flex-1 border-b p-1 text-sm font-bold focus:border-blue-500 outline-none" 
                          placeholder="输入题目内容" 
                        />
                        <span class="text-xs bg-gray-100 px-2 py-1 rounded text-gray-500 uppercase font-mono tracking-wider">{{ q.type }}</span>
                      </div>
                      
                      <input 
                        v-if="q.type === 'select'"
                        type="text" 
                        v-model="q.options"
                        class="w-full bg-gray-50 border p-2 rounded text-xs text-gray-600 focus:bg-white focus:ring-1 ring-blue-500 outline-none" 
                        placeholder="选项用逗号分隔，例如：好,一般,差" 
                      />
                      
                      <div v-if="q.type === 'range'" class="flex gap-4 items-center text-xs bg-gray-50 p-2 rounded">
                        <div class="flex gap-2 items-center flex-1">
                          <span class="text-gray-500">范围:</span>
                          <input 
                            type="number" 
                            v-model.number="q.min"
                            @change="updateQuestion(q.id, 'min', q.min)"
                            class="w-16 p-1 border rounded text-center text-xs"
                            placeholder="最小值"
                          />
                          <span class="text-gray-500">-</span>
                          <input 
                            type="number" 
                            v-model.number="q.max"
                            @change="updateQuestion(q.id, 'max', q.max)"
                            class="w-16 p-1 border rounded text-center text-xs"
                            placeholder="最大值"
                          />
                        </div>
                      </div>
                    </div>
                    <button @click="removeQuestion(q.id)" class="text-gray-300 hover:text-red-500 p-2 transition-colors">
                      <Trash2 :size="18" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'records'" class="h-full flex flex-col">
          <div class="space-y-4 flex-none mb-6">
            <div class="flex justify-between items-center mb-6">
              <h2 class="text-xl font-bold text-gray-800 flex items-center gap-2">
                <Clock :size="24" class="text-indigo-600" /> 治疗记录
              </h2>
              <Button @click="handleExportAllLogs" variant="secondary">
                <template #icon><Download :size="18" /></template>
                导出所有记录
              </Button>
            </div>
            
            <!-- 查询条件 -->
            <div class="flex flex-col md:flex-row gap-4 items-start md:items-center">
              <div class="relative">
                <Users class="absolute left-3 top-3 text-gray-400" :size="18" />
                <input
                  v-model="searchLogUserId"
                  type="text"
                  class="pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 ring-blue-500 outline-none"
                  placeholder="搜索用户ID..."
                />
              </div>
              
              <div class="flex gap-4 items-center">
                <div class="flex items-center gap-2">
                  <label class="text-sm text-gray-500">开始日期:</label>
                  <input
                    type="date"
                    v-model="logDateRange.start"
                    class="border border-gray-300 rounded-lg p-2 text-sm focus:ring-2 ring-blue-500 outline-none"
                  />
                </div>
                <div class="flex items-center gap-2">
                  <label class="text-sm text-gray-500">结束日期:</label>
                  <input
                    type="date"
                    v-model="logDateRange.end"
                    class="border border-gray-300 rounded-lg p-2 text-sm focus:ring-2 ring-blue-500 outline-none"
                  />
                </div>
                <Button size="sm" variant="secondary" @click="searchLogUserId = ''; logDateRange.start = ''; logDateRange.end = ''">
                  重置筛选
                </Button>
              </div>
            </div>
          </div>
          
          <p v-if="logs.length === 0" class="text-center text-gray-500 py-10">暂无治疗记录。</p>
          
          <Card v-else className="overflow-hidden flex-1 flex flex-col min-h-0">
            <div class="flex-1 overflow-y-auto min-h-0">
              <table class="w-full text-left text-sm">
                <thead class="bg-gray-50 border-b text-gray-500 sticky top-0 z-10">
                  <tr>
                    <th class="p-4">受试者ID</th>
                    <th class="p-4">日期</th>
                    <th class="p-4">时间</th>
                    <th class="p-4">方案名称</th>
                    <th class="p-4">时长</th>
                    <th class="p-4">状态</th>
                    <th class="p-4">操作</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                  <tr v-for="log in paginatedLogs" :key="log.id" class="hover:bg-gray-50">
                    <td class="p-4 font-mono font-medium">{{ log.user_id }}</td>
                    <td class="p-4">{{ log.date }}</td>
                    <td class="p-4">{{ log.start_time }}</td>
                    <td class="p-4">{{ log.scheme_name }}</td>
                    <td class="p-4">{{ log.duration_actual }}</td>
                    <td class="p-4">
                      <span :class="[
                        'px-2 py-1 rounded text-xs',
                        log.status === '自动完成' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                      ]">
                        {{ log.status }}
                      </span>
                    </td>
                    <td class="p-4 flex items-center gap-2">
                      <button @click="viewLogsUserId = log.user_id" class="text-gray-500 hover:text-indigo-600" title="查看详情">
                        <Eye :size="16" />
                      </button>
                      <!-- 只对未结束的会话显示结束按钮 -->
                      <button 
                        v-if="log.status !== 'FINISHED' && log.status !== 'CANCELLED' && log.status !== 'FAILED'"
                        @click="handleEndTreatment(log)" 
                        class="text-red-500 hover:text-red-700"
                        title="结束治疗"
                      >
                        <LogOut :size="16" />
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            
            <!-- 分页控件 -->
            <div class="flex items-center justify-between mt-auto px-4 pb-4 pt-4 border-t">
              <div class="text-sm text-gray-500">
                共 {{ logsTotal }} 条记录，第 {{ logCurrentPage }}/{{ logTotalPages }} 页
              </div>
              <div class="flex items-center gap-2">
                <Button size="sm" variant="secondary" @click="logPrevPage()" :disabled="logCurrentPage === 1">
                  上一页
                </Button>
                <div class="flex items-center gap-1">
                <template v-for="(page, idx) in visibleLogPages" :key="idx">
                  <span v-if="page === '...'" class="px-2 text-gray-400">...</span>
                  <button
                    v-else
                    @click="goToLogPage(page)"
                    :class="[
                      'px-3 py-1 rounded text-sm transition-colors',
                      logCurrentPage === page ? 'bg-blue-600 text-white' : 'bg-gray-100 hover:bg-gray-200'
                    ]"
                  >
                    {{ page }}
                  </button>
                </template>
                </div>
                <Button size="sm" variant="secondary" @click="logNextPage()" :disabled="logCurrentPage === logTotalPages">
                  下一页
                </Button>
              </div>
            </div>
          </Card>
        </div>
        
        <div v-if="activeTab === 'spectrum'" class="h-full flex items-center justify-center">
          <div class="text-center max-w-md p-8 bg-gray-50 rounded-2xl border-2 border-dashed border-gray-200">
            <div class="w-16 h-16 bg-indigo-50 text-indigo-500 rounded-full flex items-center justify-center mx-auto mb-4">
              <Activity :size="32" />
            </div>
            <h2 class="text-xl font-bold text-gray-800 mb-2">光谱仪 (CS计算)</h2>
            <p class="text-gray-500 mb-6">
              此处将展示光谱仪的实时数据采集与 CS (Circadian Stimulus) 计算分析模块。
            </p>
            <div class="text-sm font-bold text-indigo-600 bg-indigo-50 px-3 py-1 rounded-full inline-block">
              功能开发中
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'data'" class="space-y-8">
          <div>
            <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
              <Database :size="24" class="text-indigo-600" /> 数据库文件管理 (JSON)
            </h2>
            <div class="grid md:grid-cols-2 gap-4">
              <Card className="p-6 text-center hover:shadow-md transition-shadow">
                <Download :size="32" class="mx-auto text-indigo-500 mb-3" />
                <h3 class="font-bold text-gray-800">数据库备份导出</h3>
                <p class="text-sm text-gray-500 mb-4 mt-2">将当前所有用户、方案、日志打包为JSON文件下载。</p>
                <Button @click="exportDatabase(dataStore.getFullDbState())" variant="secondary" className="w-full">
                  下载 .json 数据库
                </Button>
              </Card>
              <Card className="p-6 text-center hover:shadow-md transition-shadow relative">
                <Upload :size="32" class="mx-auto text-green-500 mb-3" />
                <h3 class="font-bold text-gray-800">数据库恢复导入</h3>
                <p class="text-sm text-gray-500 mb-4 mt-2">上传之前备份的JSON文件以恢复数据（覆盖当前数据）。</p>
                <Button variant="secondary" className="w-full relative">
                  选择文件并导入
                  <input 
                    type="file" 
                    accept=".json" 
                    ref="fileInputRef"
                    @change="handleFileImport"
                    class="absolute inset-0 opacity-0 cursor-pointer" 
                  />
                </Button>
              </Card>
            </div>
          </div>
        </div>
      </main>
    </div>

    <Modal :is-open="isGroupModalOpen" @close="isGroupModalOpen = false" title="实验分组管理">
      <div class="space-y-4">
        <div class="space-y-3 p-4 bg-gray-50 rounded-lg border">
          <label class="block text-sm font-bold text-gray-700">添加新分组</label>
          <div class="flex gap-2">
            <input 
              type="text" 
              v-model="newGroupName"
              class="flex-1 border p-2 rounded text-sm" 
              placeholder="输入分组名称..." 
            />
            <Button @click="handleAddGroup">
              <template #icon><Plus :size="18" /></template>
              添加
            </Button>
          </div>
          <div class="flex flex-col gap-1">
             <label class="text-sm text-gray-500 whitespace-nowrap">关联量表:</label>
             <select v-model="newGroupScaleIds" multiple class="flex-1 border p-2 rounded text-sm bg-white h-32 overflow-y-auto">
               <option v-for="scale in scales" :key="scale.id" :value="scale.id">
                 {{ scale.name }}
               </option>
             </select>
          </div>
          <div class="flex flex-col gap-1">
             <label class="text-sm text-gray-500 whitespace-nowrap flex justify-between">
               <span>关联设备:</span>
               <span class="text-xs text-gray-400">(多选，不选则默认添加所有设备)</span>
             </label>
             <select v-model="newGroupDeviceIds" multiple class="flex-1 border p-2 rounded text-sm bg-white h-32 overflow-y-auto">
               <option v-for="device in devices" :key="device.id" :value="device.id">
                 {{ device.deviceName || device.deviceSn }}
               </option>
             </select>
          </div>
        </div>

        <div class="border rounded-lg overflow-hidden max-h-[400px] overflow-y-auto">
          <div class="bg-gray-50 p-2 text-xs font-bold text-gray-500 flex justify-between px-4">
             <span>分组名</span>
             <span>关联量表</span>
          </div>
          <div v-for="(group, idx) in groups" :key="idx" class="p-3 border-b last:border-0 hover:bg-gray-50 flex justify-between items-start group">
            <div class="font-medium text-gray-700">{{ group.name }}</div>
            <div class="flex flex-col items-end gap-2">
              <div class="flex flex-wrap gap-1">
                <template v-if="group.surveyTemplateIds && group.surveyTemplateIds.length > 0">
                  <span 
                    v-for="templateId in group.surveyTemplateIds" 
                    :key="templateId"
                    class="text-sm text-blue-600 bg-blue-50 px-2 py-1 rounded"
                  >
                    {{ getScaleName(templateId) }}
                  </span>
                </template>
                <template v-else-if="group.surveyTemplateId">
                  <span class="text-sm text-blue-600 bg-blue-50 px-2 py-1 rounded">
                    {{ getScaleName(group.surveyTemplateId) }}
                  </span>
                </template>
                <template v-else>
                  <span class="text-sm text-gray-400 bg-gray-50 px-2 py-1 rounded">
                    未关联量表
                  </span>
                </template>
              </div>
              <button @click="dataStore.deleteGroup(group.name)" class="text-gray-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity">
                <Trash2 :size="16" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </Modal>

    <Modal 
      :is-open="isUserModalOpen" 
      @close="closeUserModal"
      :title="isEditingUser ? '编辑受试者账户' : '新增受试者账户'"
    >
      <div class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-sm font-medium">账户ID</label>
            <input 
              type="text" 
              v-model="userForm.id"
              class="w-full border p-2 rounded" 
              placeholder="SUB-xxx"
            />
          </div>
          <div>
            <label class="text-sm font-medium">登录密码</label>
            <input 
              type="text" 
              v-model="userForm.password"
              class="w-full border p-2 rounded" 
              placeholder="设置初始密码"
            />
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-sm font-medium">性别</label>
            <select v-model="userForm.gender" class="w-full border p-2 rounded">
              <option>男</option>
              <option>女</option>
            </select>
          </div>
          <div>
            <label class="text-sm font-medium">年龄</label>
            <input 
              type="number" 
              v-model.number="userForm.age"
              class="w-full border p-2 rounded"
            />
          </div>
        </div>
        <div>
          <label class="text-sm font-medium text-indigo-600">实验分组</label>
          <select v-model="userForm.group_name" class="w-full border border-indigo-200 bg-indigo-50 p-2 rounded">
            <option v-for="g in groups" :key="g.name" :value="g.name">{{ g.name }}</option>
          </select>
        </div>
        <div class="border-t pt-4">
          <h4 class="text-sm font-bold text-gray-700 mb-3">档案信息</h4>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium">主试姓名</label>
              <input 
                type="text" 
                v-model="userForm.subjectName"
                class="w-full border p-2 rounded"
              />
            </div>
            <div>
              <label class="text-sm font-medium">联系方式</label>
              <input 
                type="text" 
                v-model="userForm.contactInfo"
                class="w-full border p-2 rounded"
              />
            </div>
            <div>
              <label class="text-sm font-medium">入组时间</label>
              <input 
                type="date" 
                v-model="userForm.enrollmentDate"
                class="w-full border p-2 rounded"
              />
            </div>
            <div>
              <label class="text-sm font-medium">诊断结果</label>
              <input 
                type="text" 
                v-model="userForm.diagnosisResult"
                class="w-full border p-2 rounded"
              />
            </div>
          </div>
        </div>
        <Button @click="handleSaveUser" className="w-full mt-4">保存账户</Button>
      </div>
    </Modal>

    <Modal 
      :is-open="isSchemeModalOpen" 
      @close="closeSchemeModal"
      :title="isEditingScheme ? '编辑诊疗方案' : '配置多阶段诊疗方案'"
    >
      <div class="space-y-4">
        <div>
          <label class="text-sm font-medium">方案名称</label>
          <input 
            type="text" 
            v-model="schemeForm.name"
            class="w-full border p-2 rounded"
          />
        </div>
        <div>
          <label class="text-sm font-medium">描述</label>
          <input 
            type="text" 
            v-model="schemeForm.description"
            class="w-full border p-2 rounded"
          />
        </div>
        <div>
          <label class="text-sm font-medium">关联对象组</label>
          <select v-model="schemeForm.target_group" class="w-full border p-2 rounded" :disabled="isEditingScheme">
            <option value="">无 (通用)</option>
            <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
        </div>
        <div class="border-t pt-4">
          <div class="flex justify-between items-center mb-2">
            <label class="text-sm font-bold text-gray-700">阶段配置</label>
            <button @click="addStage" class="text-xs bg-blue-50 text-blue-600 px-2 py-1 rounded hover:bg-blue-100">
              + 添加阶段
            </button>
          </div>
          <div class="space-y-3 max-h-[300px] overflow-y-auto pr-1">
            <div 
              v-for="(stage, idx) in schemeForm.stages" 
              :key="idx"
              class="bg-gray-50 p-3 rounded-lg border border-gray-200 relative group"
            >
              <div class="absolute top-2 left-2 w-5 h-5 bg-gray-200 rounded-full flex items-center justify-center text-xs font-bold text-gray-600">
                {{ idx + 1 }}
              </div>
              <button @click="removeStage(idx)" class="absolute top-2 right-2 text-gray-400 hover:text-red-500">
                <X :size="16" />
              </button>
              
              <!-- 阶段时长配置 -->
              <div class="flex items-center justify-between gap-2 mt-1 pl-4 mb-3">
                <div class="flex items-center gap-2">
                  <label class="text-xs text-gray-500">阶段时长</label>
                  <input 
                    type="number" 
                    :value="stage.duration"
                    @input="handleStageDurationChange(idx, $event.target.value)"
                    class="w-16 border rounded px-2 py-1 text-sm font-bold text-blue-600"
                  />
                  <span class="text-xs text-gray-500">分钟</span>
                </div>
                <button 
                  @click="handleExecuteScenario(idx)"
                  class="text-xs bg-green-50 text-green-600 px-2 py-1 rounded hover:bg-green-100 transition-colors"
                >
                  执行场景
                </button>
              </div>
              
              <!-- 设备配置列表 -->
              <div class="space-y-3 mt-2 pl-4">
                <!-- 找到当前选中的分组或显示所有设备 -->
                <div v-if="schemeForm.target_group || schemeForm.target_group === ''">
                  <div v-for="device in filteredDevices" :key="device.id" class="border-t pt-2">
                    <div class="font-medium text-sm text-gray-700 mb-2">{{ device.deviceName }}</div>
                    
                    <!-- 普通设备配置 -->
                    <div v-if="device.deviceType !== DEVICE_TYPES.TYPE_485 && device.deviceType !== DEVICE_TYPES.MCB" class="grid grid-cols-2 gap-2">
                      <div>
                          <label class="text-xs text-gray-500">亮度（0-100%）</label>
                          <input 
                            type="number" 
                            :value="toPercentage(stage.devices[device.id]?.brightness || 50)"
                            @input="handleDeviceConfigChange(idx, device.id, 'brightness', toRawValue($event.target.value))"
                            class="w-full border rounded px-1 py-1 text-sm"
                            min="0"
                            max="100"
                          />
                        </div>
                      <div>
                        <label class="text-xs text-gray-500">色温（K）</label>
                        <input 
                          type="number" 
                          :value="stage.devices[device.id]?.temp || 4500"
                          @input="handleDeviceConfigChange(idx, device.id, 'temp', $event.target.value)"
                          class="w-full border rounded px-1 py-1 text-sm"
                        />
                      </div>
                    </div>
                    
                    <!-- 485或MCB设备配置 -->
                    <div v-else class="grid grid-cols-3 gap-2">
                      <div>
                        <label class="text-xs text-gray-500">太阳亮度（0-100%）</label>
                        <input 
                          type="number" 
                          :value="toPercentage(stage.devices[device.id]?.sun_brightness || 50)"
                          @input="handleDeviceConfigChange(idx, device.id, 'sun_brightness', toRawValue($event.target.value))"
                          class="w-full border rounded px-1 py-1 text-sm"
                          min="0"
                          max="100"
                        />
                      </div>
                      <div>
                        <label class="text-xs text-gray-500">天空亮度（0-100%）</label>
                        <input 
                          type="number" 
                          :value="toPercentage(stage.devices[device.id]?.sky_brightness || 40)"
                          @input="handleDeviceConfigChange(idx, device.id, 'sky_brightness', toRawValue($event.target.value))"
                          class="w-full border rounded px-1 py-1 text-sm"
                          min="0"
                          max="100"
                        />
                      </div>
                      <div>
                        <label class="text-xs text-gray-500">色温（K）</label>
                        <input 
                          type="number" 
                          :value="stage.devices[device.id]?.temp || 4500"
                          @input="handleDeviceConfigChange(idx, device.id, 'temp', $event.target.value)"
                          class="w-full border rounded px-1 py-1 text-sm"
                        />
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="text-gray-500 text-sm">请先选择分组</div>
              </div>
            </div>
          </div>
          <div class="text-right mt-2 text-sm text-gray-600">
            预计总时长: <span class="font-bold text-lg text-blue-600">{{ schemeForm.stages.reduce((a, c) => a + c.duration, 0) }}</span> 分钟
          </div>
        </div>
        <Button @click="handleSaveScheme" className="w-full mt-4">保存完整方案</Button>
      </div>
    </Modal>

    <Modal :is-open="!!viewLogsUserId" @close="viewLogsUserId = null" :title="`记录详情: ${viewLogsUserId}`" size="lg">
      <div class="space-y-6">
        <div class="border rounded-xl p-4 bg-blue-50">
          <h3 class="font-bold text-gray-800 mb-3 flex items-center gap-2">
            <Users :size="16" /> 受试者档案
          </h3>
            <div class="grid md:grid-cols-2 gap-4 text-sm">
              <div class="space-y-2">
                <p class="flex justify-between"><span class="text-gray-500">受试者ID:</span><span class="font-medium text-gray-900">{{ viewingUser?.subjectCode || viewingUser?.id || viewLogsUserId }}</span></p>
                <p class="flex justify-between"><span class="text-gray-500">性别:</span><span class="font-medium text-gray-900">{{ viewingUser?.gender || '未填写' }}</span></p>
                <p class="flex justify-between"><span class="text-gray-500">年龄:</span><span class="font-medium text-gray-900">{{ viewingUser?.age || '未填写' }}</span></p>
                <p class="flex justify-between"><span class="text-gray-500">分组:</span><span class="font-medium text-gray-900">{{ viewingUser?.groupName || viewingUser?.group_name || '未填写' }}</span></p>
              </div>
              <div class="space-y-2">
                  <p class="flex justify-between"><span class="text-gray-500">主试姓名:</span><span class="font-medium text-gray-900">{{ viewingUser?.subjectName || '未填写' }}</span></p>
                  <p class="flex justify-between"><span class="text-gray-500">联系方式:</span><span class="font-medium text-gray-900">{{ viewingUser?.contactInfo || '未填写' }}</span></p>
                  <p class="flex justify-between"><span class="text-gray-500">入组时间:</span><span class="font-medium text-gray-900">{{ viewingUser?.enrollmentDate || '未填写' }}</span></p>
                  <p class="flex justify-between"><span class="text-gray-500">诊断结果:</span><span class="font-medium text-gray-900">{{ viewingUser?.diagnosisResult || '未填写' }}</span></p>
                </div>
            </div>
          </div>
        
        <p v-if="currentUserLogs.length === 0" class="text-center text-gray-500 py-10">该用户暂无诊疗记录。</p>
        
        <div v-else class="space-y-6">
          <div v-for="log in currentUserLogs" :key="log.id" class="border rounded-xl p-4 bg-gray-50">
            <div class="flex justify-between items-center mb-3 pb-2 border-b border-gray-200">
              <div class="flex gap-4 items-center">
                <span class="font-bold text-indigo-700">{{ log.date }}</span>
                <span class="text-gray-500 text-sm">{{ log.start_time }} - {{ log.end_time }}</span>
              </div>
              <span :class="['px-2 py-1 rounded text-xs', log.status === '自动完成' || log.status === '正常结束治疗' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700']">{{ log.status }}</span>
            </div>
            <div class="grid md:grid-cols-2 gap-4">
              <div class="text-sm">
                <p class="text-gray-500">治疗方案: <span class="text-gray-900 font-medium">{{ log.scheme_name }}</span></p>
                <p class="text-gray-500">实际时长: <span class="text-gray-900 font-medium">{{ log.duration_actual }} 分钟</span></p>
              </div>
              <div class="text-sm bg-white p-3 rounded-lg border border-gray-100">
                <p class="font-bold text-gray-700 mb-2 flex items-center gap-2">
                  <MessageSquare :size="14" /> 问卷反馈
                </p>
                <ul v-if="scales.find(s => s.treatment_log_id === log.id)?.answers" class="space-y-1">
                  <li v-for="(val, key) in (scales.find(s => s.treatment_log_id === log.id)?.answers || {})" :key="key" class="flex justify-between">
                    <span class="text-gray-500">{{ key }}:</span>
                    <span class="font-medium text-gray-800">{{ val }}</span>
                  </li>
                </ul>
                <p v-else class="text-gray-400 italic">未填写问卷</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  </div>
</template>
