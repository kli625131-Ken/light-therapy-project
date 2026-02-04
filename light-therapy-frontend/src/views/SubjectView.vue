<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Sun, Clock, Settings, LogOut, Play, Pause, Thermometer, List, CheckCircle, MessageSquare, Minus, Plus, Zap, Coffee, BookOpen, Music, Wind, Droplets, Volume2, SkipBack, SkipForward, Power, Activity, Eye, Download, Users, Moon, Sunrise, Sunset, Flame, Star } from 'lucide-vue-next'
import { useUserStore } from '../stores/useUserStore'
import { useDataStore } from '../stores/useDataStore'
import { listSchemesMe, listSchemeStagesMe, listTreatments } from '../api'
import { createTreatment, endTreatment, createAndStartTreatment, manualStartTreatment, getTreatmentbySubject, manualControlTreatment, pauseTreatment, resumeTreatment } from '../api/modules/treatment'
import { getTemplate, submitSurvey, getMySurveyTemplates, getMySurveyResults, getSurveyResultBySession } from '../api/modules/survey'
import { listDevices } from '../api/modules/device'
import { getToken } from '../utils/token'
import { listGroups } from '../api/modules/group'
import { downloadMergedCSV } from '../utils/csvExport'
import Button from '../components/ui/Button.vue'
import Card from '../components/ui/Card.vue'
import Modal from '../components/ui/Modal.vue'

const router = useRouter()
const userStore = useUserStore()
const dataStore = useDataStore()

const activeTab = ref('control')
const scenes = ref([
  { id: 1, name: '清晨', description: '模拟日出晨光，唤醒身心活力', icon: 'Sunrise' },
  { id: 2, name: '正午', description: '模拟正午阳光，提振精神专注', icon: 'Sun' },
  { id: 3, name: '傍晚', description: '模拟日落余晖，营造放松氛围', icon: 'Sunset' },
  { id: 4, name: '烛光', description: '温暖摇曳烛光，舒缓情绪压力', icon: 'Flame' },
  { id: 5, name: '星空', description: '静谧深邃星空，助你安然入梦', icon: 'Star' }
])
const selectedScene = ref(null)
const activeScheme = ref(null)
const currentStageIdx = ref(0)
const isRunning = ref(false)
const stageTimeLeft = ref(0)
const totalElapsedTime = ref(0)
const manualMode = ref(false)
const manualConfig = ref({}) // 存储每个设备的手动配置
const isPaused = ref(false)
const devices = ref([])
const isLoadingDevices = ref(false)
const subjectSurveyTemplate = ref({ name: '治疗反馈', questions: [] })
const surveyTemplateId = ref(null)
// ken260129-修改内容：添加多个量表相关的状态变量
const surveyTemplates = ref([]) // 存储分组关联的所有量表
const selectedSurveyTemplate = ref(null) // 存储当前选择的量表
const surveyProgress = ref(0) // 存储量表填写进度
// 新增：合并多个量表的相关状态变量
const mergedQuestions = ref([]) // 存储合并后的所有题目
const surveyTemplatesMap = ref({}) // 存储量表模板映射，用于快速查找
const submissionStatus = ref({}) // 跟踪各量表的提交状态
// 手动激活相关的状态变量
const manualActivation = ref(false)
const isActivating = ref(false)
const activationError = ref('')
const sliderLongPress = ref({}) // 存储每个滑块的长按状态
const sliderValues = ref({}) // 存储每个滑块的释放值
// 初始化设备配置
const initDeviceConfig = () => {
  if (!devices.value || devices.value.length === 0) return
  
  const config = {}
  const sliderVals = {}
  devices.value.forEach(device => {
    config[device.id] = {
      brightness: 50, // 亮度 (0-100%)
      temp: 4500,     // 色温 (与后端默认配置一致)
      sunBrightness: 50, // 太阳亮度 (0-100%)
      skyBrightness: 50  // 天空亮度 (0-100%)
    }
    sliderVals[device.id] = {
      brightness: 50,
      temp: 4500,
      sunBrightness: 50,
      skyBrightness: 50
    }
  })
  manualConfig.value = config
  sliderValues.value = sliderVals
  console.log('设备配置初始化:', manualConfig.value)
  console.log('滑块值初始化:', sliderValues.value)
} // ken260122-恢复了sliderValues初始化，用于手动激活功能

// 恢复默认配置
const restoreDefault = () => {
  if (!devices.value || devices.value.length === 0) return
  
  const config = {}
  const sliderVals = {}
  devices.value.forEach(device => {
    config[device.id] = {
      brightness: 50, // 亮度 (0-100%)
      temp: 4500,     // 色温 (与后端默认配置一致)
      sunBrightness: 50, // 太阳亮度 (0-100%)
      skyBrightness: 50  // 天空亮度 (0-100%)
    }
    sliderVals[device.id] = {
      brightness: 50,
      temp: 4500,
      sunBrightness: 50,
      skyBrightness: 50
    }
  })
  manualConfig.value = config
  sliderValues.value = sliderVals
  console.log('设备配置已恢复默认值:', manualConfig.value)
}

// 监听设备变化，重新初始化配置
watch(devices, () => {
  initDeviceConfig()
}, { deep: true })

const showScaleModal = ref(false)
const showThankYouModal = ref(false)
const lastLogId = ref(null)
const surveyAnswers = ref({})

let interval = null
let treatmentId = null

const currentUser = computed(() => userStore.currentUser || null)
const schemes = computed(() => {
  const schemesData = dataStore?.schemes // ken260122-修改内容：添加可选链操作符，确保dataStore为undefined时不崩溃
  if (!schemesData) return []
  
  // 获取当前用户的group_id
  const userGroupId = currentUser.value?.group_id || currentUser.value?.subject?.group_id || currentUser.value?.subject?.groupid
  
  console.log('schemesData:', schemesData)
  console.log('userGroupId:', userGroupId)
  
  return schemesData.filter(s => {
    // GLOBAL方案：无论用户属于哪个分组，都应该显示
    if (s.scope === 'GLOBAL') {
      return true
    }
    // GROUP方案：直接显示，不再检查groupIds
    if (s.scope === 'GROUP') {
      return true
    }
    // 其他情况：不显示
    return false
  })
})
const logs = computed(() => dataStore?.logs || []) // ken260122-修改内容：添加可选链操作符，确保dataStore为undefined时不崩溃
const is485OrMCBDevice = computed(() => {
  if (!devices.value || devices.value.length === 0) return false
  // 检查是否有设备类型为485或MCB的设备
  return devices.value.some(device => 
    device.deviceType === '485' || device.deviceType === 'MCB'
  )
})
const surveyTemplate = computed(() => {
  const user = currentUser.value
  if (!user || !user.id) return []
  
  // 从dataStore.users中获取完整的用户信息，包括group_name
  const fullUser = dataStore?.users?.find(u => u.id === user.id) // ken260122-修改内容：添加可选链操作符，确保dataStore为undefined时不崩溃
  if (!fullUser || !fullUser.group_name) return []
  
  const group = dataStore?.groups?.find(g => g.name === fullUser.group_name) // ken260122-修改内容：添加可选链操作符，确保dataStore为undefined时不崩溃
  if (!group || !group.scaleId) return []
  
  const scale = dataStore?.scales?.find(s => s.id === group.scaleId) // ken260122-修改内容：添加可选链操作符，确保dataStore为undefined时不崩溃
  return scale ? scale.questions : []
})

// 获取设备信息
const fetchDevices = async () => {
  try {
    isLoadingDevices.value = true
    const response = await listDevices()
    console.log('设备API响应:', response)
    if (response) {
      devices.value = response
      console.log('设备列表:', devices.value)
      console.log('设备类型:', devices.value.map(device => device.deviceType))
      console.log('is485OrMCBDevice:', is485OrMCBDevice.value)
      // 初始化设备配置
      initDeviceConfig()
    }
  } catch (error) {
    console.error('获取设备列表失败:', error)
  } finally {
    isLoadingDevices.value = false
  }
}

// 获取方案列表
const fetchSchemes = async () => {
  try {
    // Fetch all schemes for mapping (groupId=null)
    const globalResponse = await listSchemesMe('GLOBAL', null, 0, 100)

    let schemes = []
    if (globalResponse && globalResponse.content && Array.isArray(globalResponse.content)) {
      schemes = globalResponse.content
    } else if (Array.isArray(globalResponse)) {
      schemes = globalResponse
    }
    
    // Fetch group schemes if user has group_id
    const userGroupId = currentUser.value?.group_id || currentUser.value?.subject?.group_id || currentUser.value?.subject?.groupid
    if (userGroupId) {
      const groupResponse = await listSchemesMe('GROUP', userGroupId, 0, 100)

      if (groupResponse && groupResponse.content && Array.isArray(groupResponse.content)) {
        // Add group schemes to global schemes
        schemes = [...schemes, ...groupResponse.content]
      }
    }
    
    // Process schemes with stages
    const schemesWithStages = schemes.map((scheme) => {
      // Check if stages are already included in the scheme data
      if (scheme.stages && Array.isArray(scheme.stages)) {
        // Calculate total duration
        scheme.total_duration = scheme.stages.reduce((total, stage) => total + (stage.durationMinutes || 0), 0)
      } else {
        scheme.stages = []
        scheme.total_duration = 0
      }
      return scheme
    })
    
    // 排序：GLOBAL类型的方案显示在前面
    schemesWithStages.sort((a, b) => {
      if (a.scope === 'GLOBAL' && b.scope !== 'GLOBAL') return -1
      if (a.scope !== 'GLOBAL' && b.scope === 'GLOBAL') return 1
      return 0
    })
    
    if (dataStore) { // ken260122-修改内容：添加dataStore存在性检查，确保dataStore为undefined时不崩溃
      dataStore.schemes = schemesWithStages
    } else {
      console.error('dataStore 未定义，无法存储方案列表') // ken260122-修改内容：添加错误日志，帮助调试问题
    }
  } catch (error) {
    console.error('获取方案列表失败:', error)
  }
}

// 获取分组信息
const fetchGroups = async () => {
  try {
    const response = await listGroups()
    // Handle paginated response (response.content) vs flat array
    if (dataStore) { // ken260122-修改内容：添加dataStore存在性检查，确保dataStore为undefined时不崩溃
      if (response && response.content && Array.isArray(response.content)) {
        dataStore.groups = response.content
      } else {
        dataStore.groups = Array.isArray(response) ? response : []
      }
      console.log('SubjectView groups:', dataStore.groups)
    } else {
      console.error('dataStore 未定义，无法存储分组列表') // ken260122-修改内容：添加错误日志，帮助调试问题
    }
    // 获取分组后，获取量表模板
    await fetchSubjectSurveyTemplate()
  } catch (error) {
    console.error('获取分组列表失败:', error)
  }
}
const getStageColor = (stageIndex, totalStages) => {
  const colors = [
    'bg-blue-300',
    'bg-green-300',
    'bg-orange-300',
    'bg-purple-300',
    'bg-pink-300',
    'bg-teal-300',
    'bg-red-300',
    'bg-indigo-300'
  ]
  
  return colors[stageIndex % colors.length]
}
// 获取受试者量表模板
const fetchSubjectSurveyTemplate = async () => {
  try {
    console.log('SubjectView currentUser:', currentUser)
    const user = currentUser.value
    if (!user || !user.id) return
    
    const groupid = currentUser.value.subject?.groupid
    console.log('SubjectView groupid:', groupid)
    // 从dataStore.users中获取完整的用户信息，包括group_name    
    if (!dataStore) { // ken260122-修改内容：添加dataStore存在性检查，确保dataStore为undefined时不崩溃
      console.error('dataStore 未定义，无法获取分组信息') // ken260122-修改内容：添加错误日志，帮助调试问题
      return
    }
    
    const group = dataStore.groups?.find(g => g.id === parseInt(groupid)) // ken260122-修改内容：添加可选链操作符，确保dataStore为undefined时不崩溃
    console.log('SubjectView group:', dataStore.groups)
    console.log('SubjectView group:', group)
    // 优先检查多个量表ID的数组，如果存在则使用第一个ID作为默认值
    let templateId = null
    if (group && group.surveyTemplateIds && group.surveyTemplateIds.length > 0) {
      templateId = group.surveyTemplateIds[0]
    } else if (group && group.surveyTemplateId) {
      templateId = group.surveyTemplateId
    } else {
      return
    }
    
    console.log('SubjectView fetching template with id:', templateId)
    surveyTemplateId.value = templateId
    const template = await getTemplate(surveyTemplateId.value)
    console.log('SubjectView fetched template:', template)
    
    // 转换模板数据结构，适配前端表单
    if (template && template.questions) {
      const formattedQuestions = template.questions.map((q, index) => {
        let frontendType = 'text'
        let minVal = 0
        let maxVal = 10
        let options = []
        
        // 映射类型
        if (q.type === 'SCALE') frontendType = 'range'
        else if (q.type === 'SINGLE' || q.type === 'MULTI') frontendType = 'select'
        else frontendType = 'text'
        
        // 处理选项和范围
        if (frontendType === 'select') {
          if (Array.isArray(q.options)) {
            options = q.options
          } else if (typeof q.options === 'string') {
            options = q.options.split(',').map(opt => opt.trim())
          }
        } else if (frontendType === 'range') {
          if (q.minValue !== undefined) minVal = q.minValue
          if (q.maxValue !== undefined) maxVal = q.maxValue
        }
        
        return {
          id: q.id || `q_${index}`,
          label: q.title || q.label || `问题 ${index + 1}`,
          type: frontendType,
          min: minVal,
          max: maxVal,
          options: options,
          required: q.required !== undefined ? q.required : true
        }
      })
      subjectSurveyTemplate.value = {
        ...template,
        questions: formattedQuestions
      }
      console.log('SubjectView formatted template:', subjectSurveyTemplate.value)
    } else {
      subjectSurveyTemplate.value = {}
    }
  } catch (error) {
    console.error('获取量表模板失败:', error)
    subjectSurveyTemplate.value = {}
  }
}

// 合并多个量表的题目
const mergeSurveyQuestions = async () => {
  try {
    // 重置状态
    mergedQuestions.value = []
    surveyTemplatesMap.value = {}
    
    // 检查是否有量表模板
    if (!surveyTemplates.value || surveyTemplates.value.length === 0) {
      console.log('No survey templates to merge')
      return
    }
    
    // 遍历所有量表模板
    for (const template of surveyTemplates.value) {
      // 存储量表模板到映射中
      surveyTemplatesMap.value[template.id] = template
      
      // 获取量表详情，确保有完整的题目信息
      const templateDetails = await fetchSurveyTemplateDetails(template.id)
      
      if (templateDetails && templateDetails.questions && templateDetails.questions.length > 0) {
        // 为每个题目添加量表ID和唯一标识
        const questionsWithTemplateId = templateDetails.questions.map((q, index) => ({
          ...q,
          templateId: template.id,
          templateName: template.name,
          uniqueId: `${template.id}_${q.id || index}`
        }))
        
        // 添加到合并题目列表
        mergedQuestions.value = [...mergedQuestions.value, ...questionsWithTemplateId]
      }
    }
    
    console.log('Merged questions:', mergedQuestions.value)
    console.log('Survey templates map:', surveyTemplatesMap.value)
  } catch (error) {
    console.error('合并量表题目失败:', error)
    mergedQuestions.value = []
  }
}

// 获取受试者分组关联的所有量表模板
const fetchSubjectSurveyTemplates = async () => {
  try {
    console.log('SubjectView fetching survey templates...')
    const response = await getMySurveyTemplates()
    console.log('SubjectView fetched survey templates:', response)
    
    // 检查响应结构，适配不同的API返回格式
    if (response) {
      if (response.data) {
        // API返回格式：{ "ok": true, "data": [...] }
        surveyTemplates.value = response.data
      } else if (Array.isArray(response)) {
        // 直接返回数组格式
        surveyTemplates.value = response
      } else {
        // 其他情况，默认为空数组
        surveyTemplates.value = []
      }
      console.log('SubjectView survey templates:', surveyTemplates.value)
    }
    
    // 新增：获取完模板后自动合并题目
    await mergeSurveyQuestions()
  } catch (error) {
    console.error('获取量表模板列表失败:', error)
    surveyTemplates.value = []
    mergedQuestions.value = []
  }
}

// 获取单个量表模板详情
const fetchSurveyTemplateDetails = async (templateId) => {
  try {
    console.log('SubjectView fetching template with id:', templateId)
    const template = await getTemplate(templateId)
    console.log('SubjectView fetched template:', template)
    
    // 转换模板数据结构，适配前端表单
    if (template && template.questions) {
      const formattedQuestions = template.questions.map((q, index) => {
        let frontendType = 'text'
        let minVal = 0
        let maxVal = 10
        let options = []
        
        // 映射类型
        if (q.type === 'SCALE') frontendType = 'range'
        else if (q.type === 'SINGLE' || q.type === 'MULTI') frontendType = 'select'
        else frontendType = 'text'
        
        // 处理选项和范围
        if (frontendType === 'select') {
          if (Array.isArray(q.options)) {
            options = q.options
          } else if (typeof q.options === 'string') {
            options = q.options.split(',').map(opt => opt.trim())
          }
        } else if (frontendType === 'range') {
          if (q.minValue !== undefined) minVal = q.minValue
          if (q.maxValue !== undefined) maxVal = q.maxValue
        }
        
        return {
          id: q.id || `q_${index}`,
          label: q.title || q.label || `问题 ${index + 1}`,
          type: frontendType,
          min: minVal,
          max: maxVal,
          options: options,
          required: q.required !== undefined ? q.required : true
        }
      })
      return {
        ...template,
        questions: formattedQuestions
      }
    } else {
      return {}
    }
  } catch (error) {
    console.error('获取量表模板详情失败:', error)
    return {}
  }
}

// 组件挂载时获取数据
watch(currentUser, (newUser) => {
  if (newUser) {
    console.log('SubjectView currentUser:', newUser)
    console.log('Subject Profile:', newUser.subject)
    fetchSchemes()
    fetchDevices()
    fetchGroups()
    fetchSubjectSurveyTemplates() // ken260129-修改内容：调用新的函数获取所有量表模板
  }
}, { immediate: true })

const dateRange = ref({ start: '', end: '' })
const pageSize = ref(10)
const currentPage = ref(1)
const treatmentRecords = ref([])
const totalRecords = ref(0)
const totalPages = ref(0) // 直接使用后端返回的分页总数
const viewDetailsLog = ref(null) // For modal
const currentLogSurveyResult = ref(null)

const getQuestionLabel = (uniqueId) => {
  if (!mergedQuestions.value) return uniqueId
  const q = mergedQuestions.value.find(q => q.uniqueId === uniqueId)
  return q ? q.label : uniqueId
}

watch(() => viewDetailsLog.value, async (newLog) => {
  if (newLog) {
    currentLogSurveyResult.value = null
    try {
      const res = await getSurveyResultBySession(newLog.id)
      if (res) {
        // Handle array response (multiple surveys)
        if (Array.isArray(res)) {
            currentLogSurveyResult.value = res.map(item => {
                let parsedAnswers = {}
                try {
                    parsedAnswers = typeof item.rawJson === 'string' ? JSON.parse(item.rawJson) : item.rawJson
                } catch (parseErr) {
                    console.error('Error parsing survey answers JSON:', parseErr)
                }
                return {
                    ...item,
                    answers: parsedAnswers
                }
            })
        } 
        // Handle single object response (backward compatibility)
        else if (res.rawJson) {
            let parsedAnswers = {}
            try {
                parsedAnswers = typeof res.rawJson === 'string' ? JSON.parse(res.rawJson) : res.rawJson
            } catch (parseErr) {
                console.error('Error parsing survey answers JSON:', parseErr)
            }
            currentLogSurveyResult.value = [{
                ...res,
                answers: parsedAnswers
            }]
        } else {
            currentLogSurveyResult.value = []
        }
      }
    } catch (e) {
      console.error('Failed to fetch survey details for log:', newLog.id, e)
    }
  } else {
    currentLogSurveyResult.value = []
  }
})
const scales = computed(() => dataStore?.scales || []) // ken260122-修改内容：添加可选链操作符，确保dataStore为undefined时不崩溃

const mapSessionToLog = (s) => {
  // Use dataStore.schemes to find scheme name even if not in current filtered list
  const sch = (dataStore?.schemes || []).find(sc => sc.id === s.schemeId) // ken260122-修改内容：添加可选链操作符，确保dataStore为undefined时不崩溃
  
  let duration = '-'
  if (s.startTime && s.endTime) {
     const st = new Date(s.startTime)
     const et = new Date(s.endTime)
     const diff = Math.max(0, Math.ceil((et - st) / 60000))
     duration = diff + ' 分钟'
  }
  
  const dateStr = s.startTime ? s.startTime.split('T')[0] : ''
  const timeStart = s.startTime ? s.startTime.split('T')[1]?.substring(0, 5) : ''
  const timeEnd = s.endTime ? s.endTime.split('T')[1]?.substring(0, 5) : ''

  return {
    id: s.id,
    user_id: s.subjectId, // Only used for ID match in API, display is redundant
    date: dateStr,
    start_time: timeStart,
    end_time: timeEnd,
    scheme_name: sch?.name || '手动模式',
    duration_actual: duration,
    status: s.status,
    metrics: s.metricsJson ? JSON.parse(s.metricsJson) : {}
  }
}

const fetchLogs = async () => {
  // Use getToken from utils to check token existence consistently with http interceptor
  const token = getToken(userStore.role)
  
  // 确保用户已登录且有token
  console.log('fetchLogs triggered. User:', currentUser.value?.id, 'Subject:', currentUser.value?.subject?.id, 'Token:', !!token)
  
  if (!currentUser.value?.id || !currentUser.value?.subject?.id || !token) {
    console.warn('fetchLogs aborted: missing user/subject/token')
    return
  }

  try {
    const start = dateRange.value.start ? new Date(dateRange.value.start).toISOString().split('T')[0] : null
    const end = dateRange.value.end ? new Date(dateRange.value.end).toISOString().split('T')[0] : null
    
    // Always filter by currentUser.id
    console.log('Fetching logs for subject:', currentUser.value.subject.id, 'Page:', currentPage.value - 1, 'Size:', pageSize.value)
    const res = await getTreatmentbySubject(currentUser.value.subject.id, null, start, end, currentPage.value - 1, pageSize.value)
    console.log('fetchLogs response:', res)
    
    if (res && res.content) {
      treatmentRecords.value = res.content.map(mapSessionToLog)
      totalRecords.value = res.totalElements
      // 直接使用后端返回的totalPages，而不是前端计算
      totalPages.value = res.totalPages
      console.log('Logs loaded:', treatmentRecords.value.length)
    } else {
      console.warn('Response format unexpected or empty:', res)
      treatmentRecords.value = []
      totalRecords.value = 0
      totalPages.value = 0
    }
    
    // Ken: 暂时移除问卷反馈历史获取，避免404错误干扰
    /* 
    // 获取问卷反馈数据
    try {
      const surveyResults = await getMySurveyResults()
      if (surveyResults && Array.isArray(surveyResults)) {
        // 更新dataStore中的scales数据
        if (dataStore) {
          dataStore.scales = surveyResults
        }
        console.log('获取问卷反馈成功:', surveyResults)
      }
    } catch (surveyError) {
      console.error('获取问卷反馈失败:', surveyError)
    }
    */
  } catch (e) {
    console.error('Fetch logs failed', e)
    treatmentRecords.value = []
    totalRecords.value = 0
    totalPages.value = 0
  }
}

const paginatedLogs = computed(() => treatmentRecords.value)

const goToPage = (page) => {
  currentPage.value = Math.max(1, Math.min(page, totalPages.value))
}
const nextPage = () => { if (currentPage.value < totalPages.value) currentPage.value++ }
const prevPage = () => { if (currentPage.value > 1) currentPage.value-- }

// Watchers
watch([currentPage, pageSize], fetchLogs)
watch(() => [dateRange.value.start, dateRange.value.end], () => {
  currentPage.value = 1
  fetchLogs()
})
// Initial Load and User Change
watch(() => [currentUser.value?.subject?.id, userStore.role], ([newSubjectId, newRole]) => {
  const token = getToken(newRole)
  if (newSubjectId && token) fetchLogs()
}, { immediate: true })

const handleExportMyLogs = async () => {
  if (!currentUser.value?.id || !currentUser.value?.subject?.id) return
  try {
     const start = dateRange.value.start ? new Date(dateRange.value.start).toISOString().split('T')[0] : null
     const end = dateRange.value.end ? new Date(dateRange.value.end).toISOString().split('T')[0] : null
     
     const res = await getTreatmentbySubject(currentUser.value.subject.id, null, start, end, 0, 10000)
     if (!res || !res.content || res.content.length === 0) {
        alert("暂无记录可导出")
        return
     }
     
     const mapped = res.content.map(mapSessionToLog)
     // Add user name to filename
     const name = currentUser.value.subjectCode || currentUser.value.username || currentUser.value.id
     downloadMergedCSV(mapped, scales.value, `${name}_诊疗记录_${new Date().toISOString().split('T')[0]}`)
  } catch (e) {
     console.error(e)
     alert("导出失败")
  }
}

// 结束治疗会话
const handleEndTreatment = async (log) => {
  try {
    await endTreatment(log.id, { completed: false })
    // 更新日志列表
    fetchLogs()
    // 设置最后日志ID并显示反馈模态框
    lastLogId.value = log.id
    // 初始化问卷答案
    initSurveyAnswers()
    showScaleModal.value = true
    console.log('✅ 结束治疗会话成功，显示反馈模态框')
  } catch (error) {
    console.error('❌ 结束治疗会话失败:', error)
    alert(`结束治疗会话失败: ${error.message}`)
  }
}

const getDisplayParams = () => {
  if (manualMode.value) {
    // 手动模式下，分别获取发光天花和天窗灯的配置
    let ceilingTemp = 4500
    let skylightTemp = 4500
    let brightness = 50
    let sunBrightness = 50
    let skyBrightness = 50
    
    // 遍历所有设备，获取对应的配置
    devices.value.forEach(device => {
      const config = manualConfig.value[device.id] || {}
      
      if (device) {
        // 对于发光天花设备
        if (device.deviceType !== '485' && device.deviceType !== 'MCB') {
          brightness = config.brightness || brightness
          ceilingTemp = config.temp || ceilingTemp
        }
        // 对于天窗灯设备
        else {
          sunBrightness = config.sunBrightness || sunBrightness
          skyBrightness = config.skyBrightness || skyBrightness
          skylightTemp = config.temp || skylightTemp
        }
      }
    })
    
    // 使用发光天花的色温作为默认temp值
    const temp = ceilingTemp
    
    return {
      brightness,
      temp,
      ceilingTemp,
      skylightTemp,
      sunBrightness,
      skyBrightness
    }
  }
  if (activeScheme.value && activeScheme.value.stages[currentStageIdx.value]) {
    // ken-2601-修改内容：为推荐方案的阶段参数添加默认值，确保显示正常
    const stage = activeScheme.value.stages[currentStageIdx.value]
    
    // 初始化默认值
    let brightness = 50
    let temp = 4500
    let ceilingTemp = 4500
    let skylightTemp = 4500
    let sunBrightness = 50
    let skyBrightness = 50
    
    // 尝试从deviceConfigs获取设备配置
    if (stage.deviceConfigs && Array.isArray(stage.deviceConfigs)) {
      // 分别获取发光天花和天窗灯的配置
      const ceilingConfig = stage.deviceConfigs.find(config => config.skyLightIntensity === undefined || config.skyLightIntensity === null)
      const skylightConfig = stage.deviceConfigs.find(config => config.skyLightIntensity !== undefined && config.skyLightIntensity !== null)
      
      // 处理发光天花配置
      if (ceilingConfig) {
        brightness = ceilingConfig.lightIntensity || brightness
        ceilingTemp = ceilingConfig.lightColorTemp || ceilingTemp
      }
      
      // 处理天窗灯配置
      if (skylightConfig) {
        sunBrightness = skylightConfig.lightIntensity || sunBrightness
        skyBrightness = skylightConfig.skyLightIntensity || skyBrightness
        skylightTemp = skylightConfig.lightColorTemp || skylightConfig.skyLightColorTemp || skylightConfig.colorTemp || skylightConfig.temp || skylightTemp
      }
      
      // 如果有发光天花设备，使用其色温；否则使用天窗灯的色温
      temp = ceilingTemp !== 4500 ? ceilingTemp : skylightTemp
    }
    // 如果deviceConfigs不存在或为空，使用stage的默认值
    else {
      brightness = stage.brightness || brightness
      temp = stage.temp || temp
      ceilingTemp = temp
      skylightTemp = temp
      sunBrightness = stage.sunBrightness || sunBrightness
      skyBrightness = stage.skyBrightness || skyBrightness
    }
    
    return {
      brightness,
      temp,
      ceilingTemp,
      skylightTemp,
      sunBrightness,
      skyBrightness,
      durationMinutes: stage.durationMinutes || 0
    }
  }
  return { 
    brightness: 50, 
    temp: 4500, 
    ceilingTemp: 4500, 
    skylightTemp: 4500,
    sunBrightness: 50, 
    skyBrightness: 50 
  }
}

const currentParams = computed(() => getDisplayParams())

const getLightColorStyle = () => {
  if (!isRunning.value && !manualMode.value) {
    return { backgroundColor: '#e5e7eb', opacity: 1 }
  }
  
  const { temp } = currentParams.value
  // 根据设备类型获取亮度值
  let brightness = 0
  if (is485OrMCBDevice.value) {
    // 对于485或MCB设备，使用太阳亮度作为主要亮度
    brightness = currentParams.value.sunBrightness || 0
  } else {
    // 对于其他设备，使用普通亮度
    brightness = currentParams.value.brightness || 0
  }
  
  const ratio = Math.max(0, Math.min(1, (temp - 2700) / (6500 - 2700)))
  
  let r, g, b
  if (ratio < 0.3) {
    r = Math.round(255 * (0.8 + 0.2 * (1 - ratio)))
    g = Math.round(150 + 50 * (1 - ratio))
    b = Math.round(80 * (0.5 + 0.5 * (1 - ratio)))
  } else if (ratio < 0.7) {
    r = Math.round(220 + 35 * ratio)
    g = Math.round(200 + 40 * ratio)
    b = Math.round(180 + 60 * ratio)
  } else {
    r = Math.round(200 - 30 * (ratio - 0.7) / 0.3)
    g = Math.round(230 - 10 * (ratio - 0.7) / 0.3)
    b = Math.round(255 * (0.8 + 0.2 * ratio))
  }
  
  // 调整亮度计算，从百分比转换为0-1范围
  const opacity = 0.3 + (brightness / 100) * 0.7
  
  return { 
    backgroundColor: `rgb(${r}, ${g}, ${b})`, 
    opacity: opacity,
    boxShadow: `0 0 30px rgba(${r}, ${g}, ${b}, 0.4)`
  }
}

watch([isRunning, stageTimeLeft, currentStageIdx, activeScheme, manualMode, isPaused], () => {
  console.log('👁️  watch监听器触发:', {
    isRunning: isRunning.value,
    isPaused: isPaused.value,
    manualMode: manualMode.value,
    activeScheme: activeScheme.value?.name,
    stageTimeLeft: stageTimeLeft.value,
    currentStageIdx: currentStageIdx.value
  })
  
  if (interval) clearInterval(interval)
  
  if (isRunning.value && !isPaused.value) {
    console.log('⏰ 创建新的计时器')
    interval = setInterval(() => {
      totalElapsedTime.value++
      
      if (!manualMode.value && activeScheme.value) {
        console.log('🔄 执行阶段计时:', {
          stageTimeLeft: stageTimeLeft.value,
          currentStageIdx: currentStageIdx.value,
          totalStages: activeScheme.value.stages.length
        })
        
        if (stageTimeLeft.value > 0) {
          stageTimeLeft.value--
        } else {
          console.log('🎭 阶段结束，检查是否有下一阶段')
          if (currentStageIdx.value < activeScheme.value.stages.length - 1) {
            const nextStage = currentStageIdx.value + 1
            currentStageIdx.value = nextStage
            stageTimeLeft.value = activeScheme.value.stages[nextStage].durationMinutes * 60
            console.log('🔄 切换到下一阶段:', nextStage + 1)
          } else {
            console.log('🏁 所有阶段完成，结束治疗')
            handleStop(true)
          }
        }
      }
    }, 1000)
  }
}, { deep: true })

const handleStartScheme = async (scheme) => {
  try {
    console.log('🔄 开始执行handleStartScheme')
    // 先初始化所有必要的变量
    activeScheme.value = scheme
    manualMode.value = false
    currentStageIdx.value = 0
    stageTimeLeft.value = scheme.stages[0].durationMinutes * 60
    totalElapsedTime.value = 0
    isPaused.value = false
    
    console.log('✅ 初始化变量完成:', {
      schemeName: scheme.name,
      stages: scheme.stages,
      firstStageDuration: scheme.stages[0].durationMinutes,
      stageTimeLeft: stageTimeLeft.value,
      activeScheme: activeScheme.value
    })
    // 调用API创建并开始治疗会话
    const response = await createAndStartTreatment({
      subjectId: currentUser.value.subject.id,
      schemeId: scheme.id,
      stageId: scheme.stages[0]?.id,
      // ✅ 新增：多设备
      deviceSns: devices.value.map(device => device.deviceSn),
      startTime: new Date().toISOString()
    })
    
    treatmentId = response.id // ken260122-修改内容：直接从响应对象中获取id，因为现在返回的是完整的响应对象
    console.log('✅ 治疗会话创建并开始成功:', response)
    
    // API调用成功后再设置isRunning.value = true，避免watch监听器过早触发
    console.log('📢 即将设置isRunning为true')
    isRunning.value = true
    console.log('✅ 设置isRunning为true完成')
  } catch (error) {
    console.error('❌ 创建治疗会话失败:', error)
    alert(`创建治疗会话失败: ${error.message}`)
    isRunning.value = false
    if (interval) clearInterval(interval)
  }
}

const handleStartManual = async () => {
  try {
    manualMode.value = true
    activeScheme.value = null
    totalElapsedTime.value = 0
    isRunning.value = true
    isPaused.value = false
    
    // 启动计时器
    clearInterval(interval)
    interval = setInterval(() => {
      if (isRunning.value && !isPaused.value) {
        totalElapsedTime.value++
      }
    }, 1000)
    
    // 构建设备控制参数，支持多个设备
    const deviceControls = []
    
    // 用于去重的设备ID集合
    const deviceIds = new Set()
    
    // 遍历所有设备，为每个设备创建控制参数
    devices.value.forEach(device => {
      // 去重处理，避免重复设备导致SQL约束违反
      if (deviceIds.has(device.id)) {
        console.warn('重复设备已跳过:', device.id, device.deviceName || device.deviceSn)
        return
      }
      deviceIds.add(device.id)
      
      // 获取设备配置，如果不存在则使用默认值
      const config = manualConfig.value[device.id] || {
        brightness: 50, 
        temp: 4500, 
        sunBrightness: 50, 
        skyBrightness: 50 
      }
      
      // 根据设备类型设置不同的配置值
      let dimValue = config.brightness
      let skyDimValue = null // 默认值 // ken260122-修改内容：修改默认值为null，符合发光天花的skydim参数要求
      
      if (device.deviceType === '485' || device.deviceType === 'MCB') {
        dimValue = config.sunBrightness
        skyDimValue = config.skyBrightness
      } else if (device.deviceType === 'LONTRI') {
        // 发光天花的skydim参数默认值为null
        skyDimValue = null // ken260122-修改内容：明确设置发光天花的skydim参数为null
      }
      
      const control = {
        deviceSn: device.deviceSn || device.sn || device.id.toString(), // 兼容不同的设备序列号字段名
        dim: dimValue, // 亮度
        cctK: config.temp, // 色温
        skyDim: skyDimValue, // 天空亮度
        skyCctK: config.temp, // 天空色温（后端必填）
        sumDim: device.deviceType === '485' || device.deviceType === 'MCB' ? config.sunBrightness : null // 总亮度
      }
      
      deviceControls.push(control)
    })
    
    console.log('设备控制参数:', deviceControls)
    // 调用API创建并开始手动治疗会话
    const subjectId = currentUser.value?.subject?.id // ken260122-修改内容：添加可选链操作符，确保subject为undefined时不崩溃
    if (!subjectId) {
      throw new Error('无法获取受试者ID，请联系管理员') // ken260122-修改内容：添加明确的错误提示，帮助用户理解问题
    }
    
    const response = await manualStartTreatment({
      subjectId: subjectId,
      deviceControls: deviceControls,
      // startTime: new Date().toISOString()
    })
    
    treatmentId = response.id // ken260122-修改内容：直接从响应对象中获取id，因为现在返回的是完整的响应对象
    console.log('✅ 手动治疗会话创建并开始成功:', response)
  } catch (error) {
    console.error('❌ 创建手动治疗会话失败:', error)
    alert(`创建手动治疗会话失败: ${error.message}`)
    isRunning.value = false
    clearInterval(interval)
  }
}

const handlePause = async () => {
  try {
    if (treatmentId) {
      await pauseTreatment(treatmentId)
      console.log('✅ 暂停治疗成功')
    }
    isPaused.value = true
  } catch (error) {
    console.error('❌ 暂停治疗失败:', error)
    alert(`暂停治疗失败: ${error.message}`)
  }
}

const handleResume = async () => {
  try {
    if (treatmentId) {
      await resumeTreatment(treatmentId)
      console.log('✅ 恢复治疗成功')
    }
    isPaused.value = false
  } catch (error) {
    console.error('❌ 恢复治疗失败:', error)
    alert(`恢复治疗失败: ${error.message}`)
  }
}

const handleRestart = () => {
  if (manualMode.value) {
    handleStartManual()
  } else if (activeScheme.value) {
    handleStartScheme(activeScheme.value)
  }
}

const updateSurveyProgress = () => {
  const answeredCount = Object.keys(surveyAnswers.value).filter(key => surveyAnswers.value[key] !== '' && surveyAnswers.value[key] !== undefined).length
  surveyProgress.value = Math.round((answeredCount / mergedQuestions.value.length) * 100)
}

const initSurveyAnswers = () => {
  // 无论是否有有效的治疗记录，都初始化问卷答案
  surveyAnswers.value = {}
  // 为合并后的题目初始化默认答案
  if (mergedQuestions.value.length > 0) {
    mergedQuestions.value.forEach(q => {
      // Ensure we use uniqueId
      const key = q.uniqueId
      if (q.type === 'range') {
        surveyAnswers.value[key] = q.min
      } else if (q.type === 'select') {
        surveyAnswers.value[key] = q.options && q.options.length > 0 ? q.options[0] : ''
      } else {
        surveyAnswers.value[key] = ''
      }
    })
  }
}

const handleStop = async (completed = false) => {
  isRunning.value = false
  isPaused.value = false
  clearInterval(interval)
  const durationMin = Math.ceil(totalElapsedTime.value / 60)
  
  // 初始化问卷答案
  initSurveyAnswers()
  
  // 调用API结束治疗会话（如果有有效的治疗记录）
  if (durationMin > 0 && treatmentId) {
    try {
      console.log('treatmentId:', treatmentId)
      // 调用API结束治疗会话
      await endTreatment(treatmentId, {
        endTime: new Date().toISOString(),
        durationActual: durationMin,
        status: completed ? '自动完成' : '手动停止'
      })
      
      console.log('✅ 治疗会话结束成功')
      
      // 直接使用treatmentId作为日志ID
      lastLogId.value = treatmentId
      
      // 刷新治疗记录列表，确保新生成的治疗记录立即显示
      await fetchLogs()
      
      console.log('✅ 治疗会话结束成功')
      
      // 只有成功结束才显示模态框
      showScaleModal.value = true
    } catch (error) {
      console.error('❌ 结束治疗会话失败:', error)
      alert(`结束治疗会话失败: ${error.message}`)
      // 即使API调用失败，也保持lastLogId.value不变
    }
  }
  
  // 手动模式结束时恢复默认配置
  if (manualMode.value) {
    restoreDefault()
  }
  
  // 重置量表相关状态
  selectedSurveyTemplate.value = null
  surveyProgress.value = 0
  submissionStatus.value = {}
}

const selectSurveyTemplate = async (template) => {
  try {
    selectedSurveyTemplate.value = await fetchSurveyTemplateDetails(template.id)
    // 初始化问卷答案
    surveyAnswers.value = {}
    if (selectedSurveyTemplate.value && selectedSurveyTemplate.value.questions) {
      selectedSurveyTemplate.value.questions.forEach((q, index) => {
        // 使用uniqueId作为key，如果没有则生成一个
        const uniqueId = `${template.id}_${q.id || index}`
        if (q.type === 'range') {
          surveyAnswers.value[uniqueId] = Math.ceil((q.max + q.min) / 2)
        } else if (q.type === 'select') {
          if (q.label === '昨晚睡眠质量') {
            surveyAnswers.value[uniqueId] = '良好'
          } else {
            surveyAnswers.value[uniqueId] = q.options && q.options.length > 0 ? q.options[0] : ''
          }
        } else {
          surveyAnswers.value[uniqueId] = ''
        }
      })
    }
    // 设置进度
    surveyProgress.value = 0
  } catch (error) {
    console.error('选择量表失败:', error)
  }
}

const handleSubmitScale = async () => {
  if (!lastLogId.value) {
    alert('治疗记录ID不存在，无法提交反馈。请联系管理员。') // ken260122-修改内容：添加明确的错误提示，帮助用户理解问题
    return
  }
  
  try {
    // 验证必填项
    const missingRequiredFields = mergedQuestions.value.filter(q => {
      return q.required && (surveyAnswers.value[q.uniqueId] === undefined || surveyAnswers.value[q.uniqueId] === '')
    })
    
    if (missingRequiredFields.length > 0) {
      alert('请填写所有必填项（带*号的问题）')
      return
    }
    
    // 按量表分组答案
    const answersByTemplate = {}
    
    // 遍历所有问题，按量表ID分组答案
    mergedQuestions.value.forEach(q => {
      if (!answersByTemplate[q.templateId]) {
        answersByTemplate[q.templateId] = {}
      }
      
      // 只添加有答案的问题
      if (surveyAnswers.value[q.uniqueId] !== undefined && surveyAnswers.value[q.uniqueId] !== '') {
        answersByTemplate[q.templateId][q.label] = surveyAnswers.value[q.uniqueId]
      }
    })
    
    // 重置提交状态
    submissionStatus.value = {}
    
    // 先获取当前会话的所有问卷结果记录，以便找到对应的 Result ID
    let sessionResults = []
    try {
      const res = await getSurveyResultBySession(lastLogId.value)
      if (Array.isArray(res)) {
        sessionResults = res
      } else if (res && res.id) {
        sessionResults = [res]
      }
    } catch (e) {
      console.error('获取会话问卷记录失败:', e)
      // Fallback: if fetch fails, we can't submit to specific IDs easily.
      // But we should try to continue or abort.
      // For now, let's alert user.
      alert('无法获取问卷记录ID，提交失败')
      return
    }

    // 逐一提交每个量表的答案
    const submissionPromises = Object.entries(answersByTemplate).map(([templateId, answers]) => {
      // Find the survey result record for this template
      const targetResult = sessionResults.find(r => String(r.templateId) === String(templateId))
      
      console.log(`[Submit Debug] TemplateId: ${templateId}`)
      
      if (!targetResult) {
        console.error(`❌ 未找到模板 ${templateId} 对应的问卷记录 (surveyResultId not found)`)
        console.log('Available Session Results:', sessionResults)
        return Promise.resolve() 
      }

      console.log(`[Submit Debug] SurveyResultId: ${targetResult.id}`)
      const payload = {
        rawJson: JSON.stringify(answers),
        score: 0
      }
      console.log(`[Submit Debug] URL: /api/surveys/${targetResult.id}/submit`)
      console.log(`[Submit Debug] Payload:`, payload)

      return submitSurvey(targetResult.id, payload).then(() => {
        submissionStatus.value[templateId] = 'success'
        console.log(`✅ 量表 ${templateId} 提交成功`)
      }).catch(error => {
        submissionStatus.value[templateId] = 'error'
        console.error(`❌ 量表 ${templateId} 提交失败:`, error)
        throw error // 继续抛出错误，确保整个提交过程失败
      })
    })
    
    // 等待所有提交完成
    await Promise.all(submissionPromises)
    
    console.log('✅ 所有问卷提交成功')
    showScaleModal.value = false
    showThankYouModal.value = true
  } catch (error) {
    console.error('❌ 问卷提交失败:', error)
    alert(`问卷提交失败: ${error.message}`)
  }
}

const handleReturnToMain = () => {
  showThankYouModal.value = false
  activeTab.value = 'control'
}

const handleFinalLogout = () => {
  showThankYouModal.value = false
  userStore.logout()
  router.push('/login')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

// ken260122-移除了手动激活相关的函数
// // 处理滑块长按开始
// const handleSliderPress = (deviceId, param) => {
//   sliderLongPress.value[`${deviceId}_${param}`] = true
//   console.log(`滑块长按开始: ${deviceId}_${param}`)
// }
// 
// // 处理滑块释放
// const handleSliderRelease = (deviceId, param) => {
//   const key = `${deviceId}_${param}`
//   if (sliderLongPress.value[key]) {
//     // 存储释放时的值
//     sliderValues.value[deviceId][param] = manualConfig.value[deviceId][param]
//     console.log(`滑块释放，存储值: ${deviceId}_${param} = ${sliderValues.value[deviceId][param]}`)
//     
//     // 如果手动激活已开启，发送API请求
//     if (manualActivation.value) {
//       sendManualControlUpdate()
//     }
//   }
//   sliderLongPress.value[key] = false
// }
// 
// // 发送手动控制更新
// const sendManualControlUpdate = async () => {
//   if (!treatmentId) return
//   
//   try {
//     isActivating.value = true
//     activationError.value = ''
//     
//     // 构建设备控制参数
//     const deviceControls = []
//     
//     // 用于去重的设备ID集合
//     const deviceIds = new Set()
//     
//     // 遍历所有设备，为每个设备创建控制参数
//     devices.value.forEach(device => {
//       // 去重处理
//       if (deviceIds.has(device.id)) {
//         console.warn('重复设备已跳过:', device.id, device.deviceName || device.deviceSn)
//         return
//       }
//       deviceIds.add(device.id)
//       
//       // 获取设备配置，如果不存在则使用默认值
//       const config = sliderValues.value[device.id] || {
//         brightness: 50, 
//         temp: 4500, 
//         sunBrightness: 50, 
//         skyBrightness: 50 
//       }
//       
//       // 根据设备类型设置不同的配置值
//       let dimValue = config.brightness
//       let skyDimValue = 100 // 默认为100
//       
//       if (device.deviceType === '485' || device.deviceType === 'MCB') {
//         dimValue = config.sunBrightness
//         skyDimValue = config.skyBrightness
//       }
//       
//       const control = {
//         deviceSn: device.deviceSn || device.sn || device.id.toString(),
//         dim: dimValue,
//         cctK: config.temp,
//         skyDim: skyDimValue
//       }
//       
//       deviceControls.push(control)
//     })
//     
//     // 构建请求体
//     const requestBody = {
//       sessionId: parseInt(treatmentId),
//       deviceControls: deviceControls,
//       source: 'manual',
//       note: '手动控制更新'
//     }
//     
//     console.log('发送手动控制请求:', requestBody)
//     
//     // 调用API
//     await manualControlTreatment(requestBody)
//     
//     console.log('✅ 手动控制更新成功')
//   } catch (error) {
//     console.error('❌ 手动控制更新失败:', error)
//     activationError.value = `控制更新失败: ${error.message}`
//   } finally {
//     isActivating.value = false
//   }
// }
// 
// // 处理手动激活
// const handleManualActivation = async () => {
//   if (manualActivation.value) {
//     // 关闭手动激活
//     manualActivation.value = false
//     console.log('手动激活已关闭')
//   } else {
//     // 开启手动激活
//     if (!isRunning.value) {
//       // 如果治疗未运行，先启动手动治疗
//       await handleStartManual()
//     }
//     
//     manualActivation.value = true
//     console.log('手动激活已开启')
//     
//     // 立即发送当前参数
//     await sendManualControlUpdate()
//   }
// }

// 处理滑块长按开始
const handleSliderPress = (deviceId, param) => {
  sliderLongPress.value[`${deviceId}_${param}`] = true
  console.log(`滑块长按开始: ${deviceId}_${param}`)
}

// 处理滑块释放
const handleSliderRelease = (deviceId, param) => {
  const key = `${deviceId}_${param}`
  if (sliderLongPress.value[key]) {
    // 存储释放时的值
    sliderValues.value[deviceId][param] = manualConfig.value[deviceId][param]
    console.log(`滑块释放，存储值: ${deviceId}_${param} = ${sliderValues.value[deviceId][param]}`)
    
    // 如果手动激活已开启，发送API请求，并传递具体变化的设备ID和参数名
    if (manualActivation.value) {
      sendManualControlUpdate(deviceId, param)
    }
  }
  sliderLongPress.value[key] = false
}

// 发送手动控制更新
const sendManualControlUpdate = async (targetDeviceId = null, targetParam = null) => {
  if (!treatmentId) return
  
  try {
    isActivating.value = true
    activationError.value = ''
    
    let deviceControls = []
    let note = ''

    // 场景1：单设备控制（当用户拖动某个滑块释放时）
    if (targetDeviceId && targetParam) {
      const device = devices.value.find(d => d.id === targetDeviceId)
      if (!device) return

      // 获取当前设备配置值
      const config = sliderValues.value[targetDeviceId] || {
        brightness: 50, temp: 4500, sunBrightness: 50, skyBrightness: 50 
      }

      // 基础控制对象
      const control = {
        deviceSn: device.deviceSn || device.sn || device.id.toString()
      }

      // 根据设备类型构建不同的控制参数
      if (device.deviceType === 'LONTRI') {
        // 发光天花：虽然需求说"可以只传一个"，但后端报错500，且给出的示例格式包含所有字段
        // 因此这里发送完整结构：dim和cctK使用当前值，skyDim显式为null
        if (targetParam === 'brightness') {
          control.dim = config.brightness
          control.cctK = null
          control.skyDim = null
          control.sumDim = null
          control.skyCctk = null
          note = `手动控制更新(LONTRI): ${targetParam}`
        } else if (targetParam === 'temp') {
          control.dim = null
          control.cctK = config.temp
          control.skyDim = null
          control.sumDim = null
          control.skyCctK = null
          note = `手动控制更新(LONTRI): ${targetParam}`
        }
      } else if (device.deviceType === 'MCB') {
        // 天窗灯：必须传所有参数（亮度、色温、天空亮度）作为一组JSON
        control.dim = null // 映射：太阳亮度 -> dim
        control.cctK = null
        control.skyDim = config.skyBrightness
        control.sumDim = config.sunBrightness
        control.skyCctK = config.temp
        note = `手动控制更新(MCB全参)`
      } else {
        // 其他设备：默认传主要参数
        control.dim = config.brightness
        control.cctK = config.temp
        // control.skyDim = null // 确保不发送null
        note = `手动控制更新(其他)`
      }
      
      deviceControls.push(control)
    } 
    // 场景2：批量控制（例如点击"开启手动激活"时）
    else {
      note = '手动控制更新(批量)'
      const deviceIds = new Set()
      
      devices.value.forEach(device => {
        if (deviceIds.has(device.id)) return
        deviceIds.add(device.id)
        
        const config = sliderValues.value[device.id] || {
          brightness: 50, temp: 4500, sunBrightness: 50, skyBrightness: 50 
        }
        
        const control = {
          deviceSn: device.deviceSn || device.sn || device.id.toString()
        }
        
        if (device.deviceType === 'LONTRI') {
          control.dim = config.brightness
          control.cctK = config.temp
          control.skyDim = null
        } else if (device.deviceType === 'MCB') {
          control.dim = config.sunBrightness
          control.cctK = config.temp
          control.skyDim = config.skyBrightness
        } else {
          control.dim = config.brightness
          control.cctK = config.temp
          control.skyDim = null
        }
        
        deviceControls.push(control)
      })
    }
    
    // 构建请求体
    const requestBody = {
      sessionId: parseInt(treatmentId),
      deviceControls: deviceControls,
      source: 'manual',
      note: note
    }
    
    console.log('发送设备控制请求:', requestBody)
    
    // 调用API
    await manualControlTreatment(requestBody)
    
    console.log('✅ 设备控制更新成功')
  } catch (error) {
    console.error('❌ 手动控制更新失败:', error)
    activationError.value = `控制更新失败: ${error.message}`
  } finally {
    isActivating.value = false
  }
} // ken260122-修改内容：修改为在同一个API请求中传输所有设备参数，包括发光天花和天窗灯

// 处理手动激活
const handleManualActivation = async () => {
  if (manualActivation.value) {
    // 关闭手动激活
    manualActivation.value = false
    console.log('手动激活已关闭')
  } else {
    // 开启手动激活
    if (!isRunning.value) {
      // 如果治疗未运行，先启动手动治疗
      await handleStartManual()
      // 启动成功后自动设置手动激活状态
      manualActivation.value = true
      console.log('手动激活已开启')
    } else {
      // 如果治疗已运行，直接开启手动激活
      manualActivation.value = true
      console.log('手动激活已开启')
      
      // 立即发送当前参数
      await sendManualControlUpdate()
    }
  }
}

onUnmounted(() => {
  if (interval) clearInterval(interval)
})
</script>

<template>
  <div class="h-screen overflow-hidden bg-gray-100 flex flex-col">
    <header class="bg-indigo-900 text-white shadow-md w-full">
      <div class="w-full px-4 h-16 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <Sun class="text-indigo-300" />
          <span class="font-bold text-lg">杨浦区精神卫生中心 · 物联网光照 · 光疗系统</span>
        </div>
        <div class="flex items-center gap-4">
          <span class="text-xs text-indigo-300">{{ currentUser?.subjectCode || currentUser?.username || '' }}</span>
          <button @click="handleLogout" class="text-sm bg-indigo-800 hover:bg-indigo-700 px-3 py-1 rounded">退出</button>
        </div>
      </div>
    </header>

    <div class="flex flex-1 w-full py-6 px-4 gap-6 overflow-hidden">
      <aside class="w-64 flex-shrink-0 flex flex-col h-full overflow-hidden">
        <div class="space-y-2 flex-1 overflow-y-auto custom-scrollbar pr-1">
          <button
            v-for="item in [
              { id: 'control', icon: Sun, label: '光疗控制' }, 
              { id: 'scenes', icon: List, label: '场景控制' }, 
              { id: 'records', icon: Clock, label: '历史记录' }
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
        </div>

        <!-- Environmental Status (Placeholder) -->
        <div class="bg-white/60 rounded-xl p-4 mt-4 border border-gray-100 backdrop-blur-sm shadow-sm flex-none">
          <h3 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-1">
            <Activity :size="14" /> 环境状态
          </h3>
          <div class="space-y-3">
            <div class="flex items-center justify-between text-sm group cursor-not-allowed opacity-60">
              <span class="flex items-center gap-2 text-gray-500"><Thermometer :size="16" /> 温度</span>
              <span class="font-mono text-gray-400">-- °C</span>
            </div>
            <div class="flex items-center justify-between text-sm group cursor-not-allowed opacity-60">
              <span class="flex items-center gap-2 text-gray-500"><Droplets :size="16" /> 湿度</span>
              <span class="font-mono text-gray-400">-- %</span>
            </div>
            <div class="flex items-center justify-between text-sm group cursor-not-allowed opacity-60">
              <span class="flex items-center gap-2 text-gray-500"><Volume2 :size="16" /> 噪声</span>
              <span class="font-mono text-gray-400">-- dB</span>
            </div>
            <div class="text-[10px] text-center text-gray-300 pt-2 border-t border-gray-100 mt-2">
              环境监测模块二期接入
            </div>
              </div>
              <!-- 手动激活错误提示 -->
              <div v-if="activationError" class="mb-4 p-2 bg-red-50 text-red-600 text-xs rounded border border-red-100">
                {{ activationError }}
              </div>
        </div>
      </aside>

      <main class="flex-1 bg-white rounded-xl shadow-sm p-6 flex flex-col min-h-0 h-full">
        <div v-if="activeTab === 'scenes'" class="h-full flex flex-col gap-6">
          <div class="flex-none">
            <h2 class="text-xl font-bold text-gray-800 mb-2">场景控制</h2>
            <p class="text-gray-500">选择预设的场景模式，快速调整灯光环境</p>
          </div>
          
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 flex-1 overflow-y-auto">
            <Card 
              v-for="scene in scenes" 
              :key="scene.id"
              :class="['p-4 transition-all cursor-pointer hover:shadow-md', selectedScene?.id === scene.id ? 'ring-2 ring-blue-500' : '']"
              @click="selectedScene = scene"
            >
              <div class="flex flex-col items-center text-center gap-3">
                <div class="w-16 h-16 rounded-full bg-blue-100 flex items-center justify-center">
                  <component :is="scene.icon === 'Sunrise' ? Sunrise : scene.icon === 'Sun' ? Sun : scene.icon === 'Sunset' ? Sunset : scene.icon === 'Flame' ? Flame : Star" :size="32" class="text-blue-600" />
                </div>
                <h3 class="font-bold text-gray-800">{{ scene.name }}</h3>
                <p class="text-gray-500 text-sm">{{ scene.description }}</p>
                <Button size="sm" variant="primary" class="mt-2">
                  应用场景
                </Button>
              </div>
            </Card>
          </div>
          
          <div v-if="selectedScene" class="flex-none p-4 bg-blue-50 rounded-xl border border-blue-100">
            <h3 class="font-bold text-gray-800 mb-2">场景详情</h3>
            <p class="text-gray-700">{{ selectedScene.description }}</p>
            <!-- 预留场景切换的接口 -->
          </div>
        </div>
        
        <div v-if="activeTab === 'control'" class="h-full flex flex-col gap-6">
          <Card className="relative flex-none overflow-hidden h-52 sm:h-64 md:h-80 flex items-center justify-center border-0 shadow-lg">
            <div class="absolute inset-0 transition-all duration-1000" :style="getLightColorStyle()" />
            <div class="relative z-10 text-center bg-white/90 backdrop-blur-sm p-4 sm:p-6 md:p-8 rounded-xl sm:rounded-2xl shadow-xl min-w-[320px] sm:min-w-[400px] max-w-[90%]">
              <div v-if="isRunning" class="animate-pulse-slow">
                <div class="text-sm font-bold text-gray-400 uppercase tracking-widest mb-2">
                  {{ manualMode ? '手动模式运行中' : `${activeScheme?.name} · 阶段 ${currentStageIdx + 1}/${activeScheme?.stages.length}` }}
                </div>
                <div class="text-6xl font-mono font-bold text-gray-800 mb-2">
                  {{ Math.floor(totalElapsedTime / 60).toString().padStart(2, '0') }}:{{ (totalElapsedTime % 60).toString().padStart(2, '0') }}
                </div>
                <div class="flex justify-center gap-4 text-sm font-medium text-gray-600 mt-4">
                  <span class="flex items-center gap-1">
                    <Sun :size="14" /> 
                    发光天花 亮度:{{ currentParams.brightness }}% 色温:{{ currentParams.ceilingTemp }}K   天窗灯 太阳亮度:{{ currentParams.sunBrightness }}% 天空亮度:{{ currentParams.skyBrightness }}% 色温:{{ currentParams.skylightTemp }}K
                  </span>
                  <!-- <span class="flex items-center gap-1">
                    <Thermometer :size="14" /> {{ currentParams.temp }} K
                  </span> -->
                </div>
                <div class="flex gap-2 sm:gap-3 mt-6 flex-wrap justify-center">
                  <Button 
                    v-if="!isPaused"
                    @click="handlePause" 
                    variant="warning" 
                    className="flex-1 min-w-[80px] whitespace-nowrap gap-1"
                  >
                    <template #icon><Pause :size="18" class="text-current" /></template>
                    暂停
                  </Button>
                  <!-- <Button 
                    v-if="isPaused"
                    @click="handleRestart" 
                    variant="secondary" 
                    className="flex-1 min-w-[80px] whitespace-nowrap gap-1"
                  >
                    <template #icon><Zap :size="18" class="text-current" /></template>
                    重开
                  </Button> -->
                  <Button 
                    v-if="isPaused"
                    @click="handleResume" 
                    variant="success" 
                    className="flex-1 min-w-[80px] whitespace-nowrap gap-1"
                  >
                    <template #icon><Play :size="18" class="text-current" /></template>
                    继续
                  </Button>
                  <Button 
                    @click="() => handleStop(false)" 
                    variant="danger" 
                    className="flex-1 min-w-[100px] whitespace-nowrap gap-1"
                  >
                    <template #icon><LogOut :size="18" class="text-current" /></template>
                    结束治疗
                  </Button>
                </div>
              </div>
              <div v-else>
                <div class="text-gray-400 mb-2 text-sm sm:text-base">设备待机中</div>
                <div class="text-lg sm:text-xl md:text-2xl font-bold text-gray-700">请选择方案开始</div>
              </div>
            </div>
          </Card>
          
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6 flex-1 min-h-0 overflow-hidden">
            <Card :class="['p-3 sm:p-4 md:p-6 transition-all h-full overflow-hidden', manualMode && isRunning ? 'ring-2 ring-blue-500' : '']">
              <div class="h-full flex flex-col">
                <div class="flex justify-between items-center mb-3 sm:mb-4 md:mb-6 flex-none flex-wrap gap-2">
                <h3 class="font-bold text-gray-800 flex items-center gap-2 text-sm sm:text-base md:text-lg">
                  <Settings :size="18" /> 手动模式
                </h3>
                <Button 
                  size="sm" 
                  :variant="manualActivation ? 'success' : 'primary'"
                  @click="handleManualActivation" 
                  :disabled="isRunning && !manualMode"
                  :loading="isActivating"
                  class="shrink-0 text-xs sm:text-sm"
                >
                  {{ !isRunning ? '启动手动' : manualActivation ? '手动激活中' : '手动激活' }}
                </Button>
              </div>
              <!-- 手动激活错误提示 -->
              <div v-if="activationError" class="mb-4 p-2 bg-red-50 text-red-600 text-xs rounded border border-red-100">
                {{ activationError }}
              </div>
              <!-- 快捷预设 
              <div class="grid grid-cols-3 gap-3 mb-2 flex-none">
                <button 
                  @click="manualConfig = { brightness: 3000, temp: 3000 }"
                  :disabled="isRunning && !manualMode"
                  class="flex flex-col items-center justify-center p-3 rounded-xl border border-orange-100 bg-orange-50 text-orange-700 hover:bg-orange-100 transition-colors disabled:opacity-50"
                >
                  <Coffee :size="20" class="mb-1" />
                  <span class="text-xs font-bold">放松</span>
                </button>
                <button 
                  @click="manualConfig = { brightness: 6000, temp: 4500 }"
                  :disabled="isRunning && !manualMode"
                  class="flex flex-col items-center justify-center p-3 rounded-xl border border-blue-100 bg-blue-50 text-blue-700 hover:bg-blue-100 transition-colors disabled:opacity-50"
                >
                  <BookOpen :size="20" class="mb-1" />
                  <span class="text-xs font-bold">阅读</span>
                </button>
                <button 
                  @click="manualConfig = { brightness: 10000, temp: 6000 }"
                  :disabled="isRunning && !manualMode"
                  class="flex flex-col items-center justify-center p-3 rounded-xl border border-yellow-100 bg-yellow-50 text-yellow-700 hover:bg-yellow-100 transition-colors disabled:opacity-50"
                >
                  <Zap :size="20" class="mb-1" />
                  <span class="text-xs font-bold">专注</span>
                </button>
              </div>
              -->

                <div class="space-y-6 flex-1 overflow-y-auto pr-2 pb-2 custom-scrollbar">
                  <!-- 设备列表或无设备提示 -->
                  <div v-if="devices.length > 0" class="space-y-6">
                    <div v-for="device in devices" :key="device.id" class="space-y-3">
                      <!-- 设备标题 -->
                      <div class="flex justify-between items-center">
                        <h4 class="font-bold text-gray-800 text-sm sm:text-base flex items-center gap-2">
                          <Sun :size="16" /> {{ device.deviceName }} ({{ device.deviceType }})
                        </h4>
                        <span class="text-xs text-gray-500">设备ID: {{ device.id }}</span>
                      </div>
                      
                      <!-- 设备控制项 -->
                      <div class="space-y-3">
                        <!-- 485或MCB设备显示太阳亮度、天空亮度和色温 -->
                        <template v-if="device.deviceType === '485' || device.deviceType === 'MCB'">
                          <!-- 太阳亮度调节 -->
                          <div class="bg-gray-50 p-3 rounded-xl border border-gray-100">
                            <div class="flex justify-between mb-2 items-end flex-wrap gap-2">
                              <span class="text-sm font-bold text-gray-700 flex items-center gap-2">
                                <Sun :size="16" class="text-gray-400" /> 太阳亮度
                              </span>
                              <span class="text-base sm:text-lg font-mono font-bold text-blue-600 bg-white px-2 py-0.5 rounded shadow-sm border">
                                {{ manualConfig[device.id]?.sunBrightness || 50 }} <span class="text-xs text-gray-400 font-normal">%</span>
                              </span>
                            </div>
                            <div class="flex items-center gap-3">
                              <button 
                                @click="manualConfig[device.id].sunBrightness = Math.max(0, (manualConfig[device.id]?.sunBrightness || 50) - 5)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Minus :size="16" />
                              </button>
                              <input
                                type="range"
                                min="0"
                                max="100"
                                step="1"
                                v-model.number="manualConfig[device.id].sunBrightness"
                                :disabled="isRunning && !manualMode"
                                class="flex-1 h-2 bg-gray-200 rounded-lg accent-blue-600 cursor-pointer hover:accent-blue-500 transition-all"
                                @mousedown="handleSliderPress(device.id, 'sunBrightness')"
                                @mouseup="handleSliderRelease(device.id, 'sunBrightness')"
                                @touchstart="handleSliderPress(device.id, 'sunBrightness')"
                                @touchend="handleSliderRelease(device.id, 'sunBrightness')"
                              />
                              <button 
                                @click="manualConfig[device.id].sunBrightness = Math.min(100, (manualConfig[device.id]?.sunBrightness || 50) + 5)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Plus :size="16" />
                              </button>
                            </div>
                          </div>

                          <!-- 天空亮度调节 -->
                          <div class="bg-gray-50 p-3 rounded-xl border border-gray-100">
                            <div class="flex justify-between mb-2 items-end flex-wrap gap-2">
                              <span class="text-sm font-bold text-gray-700 flex items-center gap-2">
                                <Sun :size="16" class="text-gray-400" /> 天空亮度
                              </span>
                              <span class="text-base sm:text-lg font-mono font-bold text-blue-600 bg-white px-2 py-0.5 rounded shadow-sm border">
                                {{ manualConfig[device.id]?.skyBrightness || 50 }} <span class="text-xs text-gray-400 font-normal">%</span>
                              </span>
                            </div>
                            <div class="flex items-center gap-3">
                              <button 
                                @click="manualConfig[device.id].skyBrightness = Math.max(0, (manualConfig[device.id]?.skyBrightness || 50) - 5)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Minus :size="16" />
                              </button>
                              <input
                                type="range"
                                min="0"
                                max="100"
                                step="1"
                                v-model.number="manualConfig[device.id].skyBrightness"
                                :disabled="isRunning && !manualMode"
                                class="flex-1 h-2 bg-gray-200 rounded-lg accent-blue-600 cursor-pointer hover:accent-blue-500 transition-all"
                                @mousedown="handleSliderPress(device.id, 'skyBrightness')"
                                @mouseup="handleSliderRelease(device.id, 'skyBrightness')"
                                @touchstart="handleSliderPress(device.id, 'skyBrightness')"
                                @touchend="handleSliderRelease(device.id, 'skyBrightness')"
                              />
                              <button 
                                @click="manualConfig[device.id].skyBrightness = Math.min(100, (manualConfig[device.id]?.skyBrightness || 50) + 5)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Plus :size="16" />
                              </button>
                            </div>
                          </div>

                          <!-- 色温调节 -->
                          <div class="bg-gray-50 p-3 rounded-xl border border-gray-100">
                            <div class="flex justify-between mb-2 items-end flex-wrap gap-2">
                              <span class="text-sm font-bold text-gray-700 flex items-center gap-2">
                                <Thermometer :size="16" class="text-gray-400" /> 色温
                              </span>
                              <span class="text-base sm:text-lg font-mono font-bold text-orange-500 bg-white px-2 py-0.5 rounded shadow-sm border">
                                {{ manualConfig[device.id]?.temp || 4500 }} <span class="text-xs text-gray-400 font-normal">K</span>
                              </span>
                            </div>
                            <div class="flex items-center gap-3">
                              <button 
                                @click="manualConfig[device.id].temp = Math.max(device.deviceType === 'MCB' ? 0 : 2700, (manualConfig[device.id]?.temp || 4500) - 500)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Minus :size="16" />
                              </button>
                              <input
                                type="range"
                                :min="device.deviceType === 'MCB' ? 0 : 2700"
                                :max="device.deviceType === 'MCB' ? 10000 : 6500"
                                step="100"
                                v-model.number="manualConfig[device.id].temp"
                                :disabled="isRunning && !manualMode"
                                class="flex-1 h-2 rounded-lg appearance-none cursor-pointer bg-gradient-to-r from-orange-300 via-yellow-200 to-blue-300 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:border [&::-webkit-slider-thumb]:border-gray-300 [&::-webkit-slider-thumb]:hover:scale-110 [&::-webkit-slider-thumb]:transition-transform"
                                @mousedown="handleSliderPress(device.id, 'temp')"
                                @mouseup="handleSliderRelease(device.id, 'temp')"
                                @touchstart="handleSliderPress(device.id, 'temp')"
                                @touchend="handleSliderRelease(device.id, 'temp')"
                              />
                              <button 
                                @click="manualConfig[device.id].temp = Math.min(device.deviceType === 'MCB' ? 10000 : 6500, (manualConfig[device.id]?.temp || 4500) + 500)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Plus :size="16" />
                              </button>
                            </div>
                          </div>
                        </template>

                        <!-- 其他设备显示亮度和色温 -->
                        <template v-else>
                          <!-- 亮度调节 -->
                          <div class="bg-gray-50 p-3 rounded-xl border border-gray-100">
                            <div class="flex justify-between mb-2 items-end flex-wrap gap-2">
                              <span class="text-sm font-bold text-gray-700 flex items-center gap-2">
                                <Sun :size="16" class="text-gray-400" /> 亮度
                              </span>
                              <span class="text-base sm:text-lg font-mono font-bold text-blue-600 bg-white px-2 py-0.5 rounded shadow-sm border">
                                {{ manualConfig[device.id]?.brightness || 50 }} <span class="text-xs text-gray-400 font-normal">%</span>
                              </span>
                            </div>
                            <div class="flex items-center gap-3">
                              <button 
                                @click="manualConfig[device.id].brightness = Math.max(0, (manualConfig[device.id]?.brightness || 50) - 5)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Minus :size="16" />
                              </button>
                              <input
                                type="range"
                                min="0"
                                max="100"
                                step="1"
                                v-model.number="manualConfig[device.id].brightness"
                                :disabled="isRunning && !manualMode"
                                class="flex-1 h-2 bg-gray-200 rounded-lg accent-blue-600 cursor-pointer hover:accent-blue-500 transition-all"
                                @mousedown="handleSliderPress(device.id, 'brightness')"
                                @mouseup="handleSliderRelease(device.id, 'brightness')"
                                @touchstart="handleSliderPress(device.id, 'brightness')"
                                @touchend="handleSliderRelease(device.id, 'brightness')"
                              />
                              <button 
                                @click="manualConfig[device.id].brightness = Math.min(100, (manualConfig[device.id]?.brightness || 50) + 5)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Plus :size="16" />
                              </button>
                            </div>
                          </div>

                          <!-- 色温调节 -->
                          <div class="bg-gray-50 p-3 rounded-xl border border-gray-100">
                            <div class="flex justify-between mb-2 items-end flex-wrap gap-2">
                              <span class="text-sm font-bold text-gray-700 flex items-center gap-2">
                                <Thermometer :size="16" class="text-gray-400" /> 色温
                              </span>
                              <span class="text-base sm:text-lg font-mono font-bold text-orange-500 bg-white px-2 py-0.5 rounded shadow-sm border">
                                {{ manualConfig[device.id]?.temp || 4000 }} <span class="text-xs text-gray-400 font-normal">K</span>
                              </span>
                            </div>
                            <div class="flex items-center gap-3">
                              <button 
                                @click="manualConfig[device.id].temp = Math.max(device.deviceType === 'MCB' ? 0 : 2700, (manualConfig[device.id]?.temp || 4500) - 500)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Minus :size="16" />
                              </button>
                              <input
                                type="range"
                                :min="device.deviceType === 'MCB' ? 0 : 2700"
                                :max="device.deviceType === 'MCB' ? 10000 : 6500"
                                step="100"
                                v-model.number="manualConfig[device.id].temp"
                                :disabled="isRunning && !manualMode"
                                class="flex-1 h-2 rounded-lg appearance-none cursor-pointer bg-gradient-to-r from-orange-300 via-yellow-200 to-blue-300 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:border [&::-webkit-slider-thumb]:border-gray-300 [&::-webkit-slider-thumb]:hover:scale-110 [&::-webkit-slider-thumb]:transition-transform"
                                @mousedown="handleSliderPress(device.id, 'temp')"
                                @mouseup="handleSliderRelease(device.id, 'temp')"
                                @touchstart="handleSliderPress(device.id, 'temp')"
                                @touchend="handleSliderRelease(device.id, 'temp')"
                              />
                              <button 
                                @click="manualConfig[device.id].temp = Math.min(device.deviceType === 'MCB' ? 10000 : 6500, (manualConfig[device.id]?.temp || 4500) + 500)"
                                :disabled="isRunning && !manualMode"
                                class="p-1 rounded-full hover:bg-gray-200 text-gray-500 disabled:opacity-50 flex-shrink-0"
                              >
                                <Plus :size="16" />
                              </button>
                            </div>
                          </div>
                        </template>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 无设备时显示提示 -->
                <div v-else class="text-center text-gray-500 py-8">
                  <Sun :size="32" class="mx-auto mb-2 opacity-50" />
                  <p>暂无设备信息</p>
                  <p class="text-xs mt-1">请联系管理员添加设备</p>
                </div>
                
                <!-- 预留控制模块区域 -->
                <div class="grid gap-3 opacity-70 mt-6 pt-6 border-t border-gray-200">
                  <!-- 2) 灯带控制 (Disabled) -->
                  <div class="border border-dashed border-gray-200 rounded-xl p-3 bg-gray-50 cursor-not-allowed relative">
                    <div class="absolute top-2 right-2 text-[10px] bg-gray-200 text-gray-400 px-2 py-0.5 rounded-full font-bold">功能即将开放</div>
                    <div class="flex items-center gap-2 mb-2">
                      <Zap :size="16" class="text-gray-400" />
                      <span class="font-bold text-gray-400 text-sm">情绪氛围全彩灯带</span>
                    </div>
                    <!-- 模式切换占位 -->
                    <div class="flex bg-gray-200 rounded p-1 w-max mb-2">
                      <span class="px-3 py-1 bg-white rounded shadow-sm text-xs font-bold text-gray-400">常规模式</span>
                      <span class="px-3 py-1 text-xs text-gray-400">场景模式</span>
                    </div>
                     <!-- 占位滑块 -->
                    <div class="space-y-2">
                      <div class="h-2 w-full bg-gray-200 rounded-full"></div>
                      <div class="h-2 w-3/4 bg-gray-200 rounded-full"></div>
                    </div>
                  </div>

                  <!-- 3) 音响控制 (Disabled) -->
                  <div class="border border-dashed border-gray-200 rounded-xl p-3 bg-gray-50 cursor-not-allowed relative">
                    <div class="absolute top-2 right-2 text-[10px] bg-gray-200 text-gray-400 px-2 py-0.5 rounded-full font-bold">功能即将开放</div>
                    <div class="flex items-center gap-2 mb-2">
                      <Music :size="16" class="text-gray-400" />
                      <span class="font-bold text-gray-400 text-sm">白噪/音乐</span>
                    </div>
                    <div class="flex items-center justify-between bg-white/50 border border-gray-100 rounded-lg p-2">
                      <Power :size="16" class="text-gray-300" />
                      <div class="flex gap-4">
                        <SkipBack :size="16" class="text-gray-300" />
                        <Play :size="16" class="text-gray-300" />
                        <SkipForward :size="16" class="text-gray-300" />
                      </div>
                    </div>
                  </div>

                  <!-- 4) 香薰机控制 (Disabled) -->
                  <div class="border border-dashed border-gray-200 rounded-xl p-3 bg-gray-50 cursor-not-allowed relative">
                    <div class="absolute top-2 right-2 text-[10px] bg-gray-200 text-gray-400 px-2 py-0.5 rounded-full font-bold">二期功能</div>
                    <div class="flex items-center gap-2 mb-2">
                      <Wind :size="16" class="text-gray-400" />
                      <span class="font-bold text-gray-400 text-sm">香薰扩香</span>
                    </div>
                    <div class="flex items-center justify-between flex-wrap gap-2">
                      <Power :size="16" class="text-gray-300 mr-2" />
                      <div class="flex gap-1">
                        <span class="px-2 py-1 bg-gray-200 text-xs rounded text-gray-400">静香</span>
                        <span class="px-2 py-1 border text-xs rounded text-gray-300">强劲</span>
                      </div>
                      <div class="flex items-center gap-2 ml-auto bg-white/50 px-2 py-1 rounded border border-gray-100">
                        <Minus :size="12" class="text-gray-300" />
                        <span class="text-xs font-mono text-gray-400">07</span>
                        <Plus :size="12" class="text-gray-300" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </Card>
            
            <Card className="h-full min-h-0 p-3 sm:p-4 md:p-5 flex flex-col overflow-hidden">
              <div class="space-y-3 sm:space-y-4 h-full flex flex-col">
                <h3 class="font-bold text-gray-800 flex items-center gap-2 text-sm sm:text-base md:text-lg">
                  <List :size="18" /> 推荐方案
                </h3>
                <div class="space-y-2 flex-1 overflow-y-auto pr-2 custom-scrollbar">
                  <div
                    v-for="scheme in schemes"
                    :key="scheme.id"
                    :class="[
                      'bg-white p-3 sm:p-4 rounded-xl border transition-all',
                      isRunning && activeScheme?.id === scheme.id ? 'border-blue-500 ring-1 ring-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300'
                    ]"
                  >
                    <div class="flex justify-between items-start mb-2 flex-wrap gap-2">
                      <div class="flex-1 min-w-0">
                        <h4 class="font-bold text-gray-800 truncate">{{ scheme.name }}</h4>
                        <p class="text-xs text-gray-500 mt-1 line-clamp-2">{{ scheme.description }}</p>
                      </div>
                      <Button size="sm" @click="handleStartScheme(scheme)" :disabled="isRunning" class="shrink-0">
                        <template #icon><Play :size="18" /></template>
                        开始
                      </Button>
                    </div>
                    <div class="mt-3 flex gap-1 h-2 rounded-full overflow-hidden bg-gray-100">
                      <div
                        v-for="(stage, idx) in (scheme.stages || [])"
                        :key="idx"
                        :style="{ width: `${(stage.durationMinutes / scheme.total_duration) * 100}%` }"
                        :class="[getStageColor(idx, scheme.stages.length), 'opacity-80']"
                      />
                    </div>
                    <div class="flex justify-between text-xs text-gray-400 mt-1">
                      <span>{{ scheme.stages?.length || 0 }} 个阶段</span>
                      <span>总计 {{ scheme.total_duration || 0 }} 分钟</span>
                    </div>
                  </div>
                </div>
              </div>
            </Card>
          </div>
        </div>
      
      <div v-if="activeTab === 'records'" class="space-y-6 h-full flex flex-col">
        <div class="flex-none">
          <div class="flex justify-between items-center mb-4">
             <h2 class="text-xl font-bold text-gray-800">我的治疗记录</h2>
             <Button @click="handleExportMyLogs" variant="secondary" size="sm">
                <template #icon><Download :size="16" /></template>
                导出记录
             </Button>
          </div>
          
          <div class="flex gap-4 items-center mb-4 flex-wrap">
            <div class="flex items-center gap-2">
              <label class="text-sm text-gray-500">开始日期:</label>
              <input
                type="date"
                v-model="dateRange.start"
                class="border border-gray-300 rounded-lg p-2 text-sm focus:ring-2 ring-blue-500 outline-none"
              />
            </div>
            <div class="flex items-center gap-2">
              <label class="text-sm text-gray-500">结束日期:</label>
              <input
                type="date"
                v-model="dateRange.end"
                class="border border-gray-300 rounded-lg p-2 text-sm focus:ring-2 ring-blue-500 outline-none"
              />
            </div>
            <Button size="sm" variant="secondary" @click="dateRange.start = ''; dateRange.end = ''">
              重置筛选
            </Button>
          </div>
        </div>
        
        <p v-if="treatmentRecords.length === 0" class="text-center text-gray-500 py-10">暂无治疗记录。</p>
        
        <Card v-else className="flex-1 min-h-0 flex flex-col">
          <div class="flex-1 overflow-y-auto min-h-0">
            <table class="w-full text-left text-sm">
              <thead class="bg-gray-50 border-b border-gray-100 text-gray-500 sticky top-0 z-10">
                <tr>
                  <th class="p-4">受试者ID</th>
                  <th class="p-4">日期</th>
                  <th class="p-4">时间</th>
                  <th class="p-4">方案</th>
                  <th class="p-4">时长</th>
                  <th class="p-4">状态</th>
                  <th class="p-4">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="log in paginatedLogs" :key="log.id" class="hover:bg-gray-50">
                  <td class="p-4 text-gray-900 font-medium">{{ currentUser?.subject?.code || '-' }}</td>
                  <td class="p-4">{{ log.date }}</td>
                  <td class="p-4 text-gray-500">{{ log.start_time }}</td>
                  <td class="p-4 font-medium text-blue-600">{{ log.scheme_name }}</td>
                  <td class="p-4 font-bold">{{ log.duration_actual }}</td>
                  <td class="p-4">
                    <span :class="['px-2 py-1 rounded text-xs', log.status === '自动完成' || log.status === '正常结束治疗' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700']">{{ log.status }}</span>
                  </td>
                  <td class="p-4 flex gap-2">
                      <button @click="viewDetailsLog = log" class="text-gray-500 hover:text-indigo-600" title="查看详情">
                        <Eye :size="16" />
                      </button>
                      <!-- 为未结束的会话添加结束按钮 -->
                      <button 
                        v-if="log.status === 'RUNNING' || log.status === 'PAUSED'" 
                        @click="handleEndTreatment(log)" 
                        class="text-red-500 hover:text-red-600" 
                        title="结束会话"
                      >
                        <LogOut :size="16" />
                      </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- 分页控件 -->
          <div class="flex items-center justify-between mt-4 px-4 pb-4">
            <div class="text-sm text-gray-500">
              共 {{ totalRecords }} 条记录，第 {{ currentPage }}/{{ totalPages }} 页
            </div>
            <div class="flex items-center gap-2">
              <Button size="sm" variant="secondary" @click="prevPage()" :disabled="currentPage === 1">
                上一页
              </Button>
              <div class="flex items-center gap-1">
                 <span class="px-2 text-sm text-gray-600">第 {{ currentPage }} 页</span>
              </div>
              <Button size="sm" variant="secondary" @click="nextPage()" :disabled="currentPage === totalPages">
                下一页
              </Button>
            </div>
          </div>
        </Card>
      </div>
      </main>
    </div>

    <Modal :is-open="showScaleModal" :title="'治疗反馈'" prevent-close>
      <div class="space-y-6 max-h-[80vh] overflow-y-auto p-2 sm:p-0">
        <!-- 合并后的量表填写界面 -->
        <div v-if="mergedQuestions.length > 0">
          <div class="bg-blue-50 p-3 sm:p-4 rounded-lg text-sm text-blue-800">治疗已结束，请填写所有反馈问题。</div>
          
          <!-- 进度指示 -->
          <div class="w-full bg-gray-200 rounded-full h-2.5">
            <div 
              class="bg-blue-600 h-2.5 rounded-full transition-all duration-300"
              :style="{ width: `${surveyProgress}%` }"
            ></div>
          </div>
          <div class="text-xs text-gray-500 text-right mt-1">
            进度: {{ surveyProgress }}%
          </div>
          
          <!-- 问题列表 -->
          <div>
            <div v-for="(q, index) in mergedQuestions" :key="q.uniqueId">
              <label class="block text-sm font-bold text-gray-700 mb-2">
                {{ index + 1 }}. {{ q.label }} {{ q.required ? '*' : '' }}
              </label>
              <div v-if="q.type === 'range'" class="flex items-center gap-2 sm:gap-4">
                <span class="text-xs text-gray-500 w-6 text-right">{{ q.min }}</span>
                <input
                  type="range"
                  :min="q.min"
                  :max="q.max"
                  v-model.number="surveyAnswers[q.uniqueId]"
                  @input="updateSurveyProgress"
                  class="flex-1 h-2 bg-gray-200 rounded-lg accent-blue-600"
                />
                <span class="text-lg font-bold text-blue-600 w-8 text-center">{{ surveyAnswers[q.uniqueId] || q.min }}</span>
                <span class="text-xs text-gray-500 w-6">{{ q.max }}</span>
              </div>
              <div v-if="q.type === 'select'" class="flex flex-wrap gap-1 sm:gap-2">
                <button
                  v-for="opt in q.options"
                  :key="opt"
                  @click="() => {
                    surveyAnswers[q.uniqueId] = opt;
                    updateSurveyProgress();
                  }"
                  :class="[
                    'py-1.5 sm:py-2 px-2 sm:px-3 rounded-lg border text-xs sm:text-sm transition-all',
                    surveyAnswers[q.uniqueId] === opt ? 'bg-blue-600 text-white border-blue-600' : 'border-gray-200 hover:bg-gray-50'
                  ]"
                >
                  {{ opt }}
                </button>
              </div>
              <textarea
                v-if="q.type === 'text'"
                v-model="surveyAnswers[q.uniqueId]"
                @input="updateSurveyProgress"
                class="w-full border border-gray-300 rounded-lg p-2 sm:p-3 text-sm focus:ring-2 ring-blue-500 outline-none"
                rows="2"
              />
            </div>
          </div>
          
          <!-- 按钮组 -->
          <div class="flex gap-4 mt-6">
            <Button 
              @click="handleSubmitScale" 
              className="flex-1 h-10 sm:h-12"
            >
              提交反馈
            </Button>
          </div>
        </div>
        
        <!-- 无量表时显示 -->
        <div v-else-if="mergedQuestions.length === 0" class="text-center text-gray-500 py-8">
          暂无反馈问题
        </div>
      </div>
    </Modal>

    <Modal :is-open="showThankYouModal" title="完成" prevent-close>
      <div class="text-center py-8">
        <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <CheckCircle :size="40" class="text-green-500" />
        </div>
        <h3 class="text-xl font-bold text-gray-800 mb-2">感谢您的使用！</h3>
        <p class="text-gray-500 mb-8 px-4">您的数据已成功记录。</p>
        <div class="flex gap-4">
          <Button @click="handleReturnToMain" variant="secondary" className="flex-1">返回主界面</Button>
          <Button @click="handleFinalLogout" className="flex-1">安全退出系统</Button>
        </div>
      </div>
    </Modal>

    <!-- Record Details Modal -->
    <Modal :is-open="!!viewDetailsLog" @close="viewDetailsLog = null" title="记录详情" size="lg">
      <div class="space-y-6" v-if="viewDetailsLog">
        
        <div class="border rounded-xl p-4 bg-blue-50">
          <h3 class="font-bold text-gray-800 mb-3 flex items-center gap-2">
            <Users :size="16" /> 我的档案
          </h3>
           <div class="grid md:grid-cols-2 gap-4 text-sm">
             <div class="space-y-2">
               <p class="flex justify-between"><span class="text-gray-500">ID:</span><span class="font-medium text-gray-900">{{ currentUser?.subject?.code || currentUser?.id }}</span></p>
               <p class="flex justify-between"><span class="text-gray-500">性别:</span><span class="font-medium text-gray-900">{{ currentUser?.subject?.gender || '未填写' }}</span></p>
               <p class="flex justify-between"><span class="text-gray-500">年龄:</span><span class="font-medium text-gray-900">{{ currentUser?.subject?.age || '未填写' }}</span></p>
               <p class="flex justify-between"><span class="text-gray-500">分组:</span><span class="font-medium text-gray-900">{{ currentUser?.subject?.groupName || '未填写' }}</span></p>
             </div>
             <div class="space-y-2">
                <p class="flex justify-between"><span class="text-gray-500">主试姓名:</span><span class="font-medium text-gray-900">{{ currentUser?.subject?.subjectName || '未填写' }}</span></p>
                <p class="flex justify-between"><span class="text-gray-500">联系方式:</span><span class="font-medium text-gray-900">{{ currentUser?.subject?.contactInfo || '未填写' }}</span></p>
                <p class="flex justify-between"><span class="text-gray-500">入组时间:</span><span class="font-medium text-gray-900">{{ currentUser?.subject?.enrollmentDate || '未填写' }}</span></p>
                 <p class="flex justify-between"><span class="text-gray-500">诊断结果:</span><span class="font-medium text-gray-900">{{ currentUser?.subject?.diagnosisResult || '未填写' }}</span></p>
             </div>
           </div>
        </div>

        <div class="border rounded-xl p-4 bg-gray-50">
           <div class="flex justify-between items-center mb-3 pb-2 border-b border-gray-200">
             <div class="flex gap-4 items-center">
               <span class="font-bold text-indigo-700">{{ viewDetailsLog.date }}</span>
               <span class="text-gray-500 text-sm">{{ viewDetailsLog.start_time }}</span>
             </div>
             <span :class="['px-2 py-1 rounded text-xs', viewDetailsLog.status === '自动完成' || viewDetailsLog.status === '正常结束治疗' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700']">{{ viewDetailsLog.status }}</span>
           </div>
           
           <div class="grid md:grid-cols-2 gap-4">
              <div class="text-sm">
                <p class="text-gray-500">治疗方案: <span class="text-gray-900 font-medium">{{ viewDetailsLog.scheme_name }}</span></p>
                <p class="text-gray-500">实际时长: <span class="text-gray-900 font-medium">{{ viewDetailsLog.duration_actual }}</span></p>
              </div>
              <div class="text-sm bg-white p-3 rounded-lg border border-gray-100">
                <p class="font-bold text-gray-700 mb-2 flex items-center gap-2">
                  <MessageSquare :size="14" /> 问卷反馈
                </p>
                <template v-if="currentLogSurveyResult && currentLogSurveyResult.length > 0">
                  <div v-for="(survey, idx) in currentLogSurveyResult" :key="idx" class="mb-3 last:mb-0 border-b last:border-0 pb-2 last:pb-0">
                    <p class="font-bold text-gray-600 text-xs mb-1" v-if="survey.templateName">{{ survey.templateName }}</p>
                    <ul class="space-y-1">
                      <li v-for="(val, key) in survey.answers" :key="key" class="flex justify-between">
                         <span class="text-gray-500">{{ getQuestionLabel(key) || key }}:</span>
                         <span class="font-medium text-gray-800">{{ val }}</span>
                      </li>
                    </ul>
                  </div>
                </template>
                <p v-else class="text-gray-400 italic">未填写问卷</p>
              </div>
           </div>
        </div>
      </div>
    </Modal>
  </div>
</template>
