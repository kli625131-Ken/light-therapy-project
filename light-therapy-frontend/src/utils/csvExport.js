import * as XLSX from 'xlsx'

export function downloadMergedCSV(logs, scales, filename) {
  if (!logs || logs.length === 0) {
    alert("暂无数据可导出")
    return
  }

  // 优化数据转换逻辑
  const mergedData = logs.map(log => {
    const relatedScale = scales.find(s => s.treatment_log_id === log.id)
   let scaleInfo = {}
   
   if (relatedScale && relatedScale.answers) {
     Object.keys(relatedScale.answers).forEach(key => {
       scaleInfo[`问卷_${key}`] = relatedScale.answers[key]
     })
   }

   // 优化日期时间格式为 YYYY-MM-DD HH:mm:ss
   let formattedDate = log.date
   let formattedTime = log.start_time
   
   // 优化治疗状态转换为中文描述
   let formattedStatus = log.status
   const statusMap = {
     '自动完成': '自动完成',
     '手动停止': '手动停止',
     'RUNNING': '运行中',
     'PAUSED': '已暂停',
     'FINISHED': '已完成',
     'CANCELLED': '已取消',
     'FAILED': '失败'
   }
   if (statusMap[formattedStatus]) {
     formattedStatus = statusMap[formattedStatus]
   }

   return {
     ...log,
     date: formattedDate,
     start_time: formattedTime,
     status: formattedStatus,
     ...scaleInfo
   }
 })

 // 确保导出的CSV文件包含清晰的表头和格式
 const allKeys = new Set()
 mergedData.forEach(obj => Object.keys(obj).forEach(k => allKeys.add(k)))
 
 // 定义表头顺序，确保重要字段在前
 const keyOrder = ['user_id', 'user_name', 'date', 'start_time', 'end_time', 'scheme_name', 'duration_actual', 'status']
 const headers = [...keyOrder, ...Array.from(allKeys).filter(k => !keyOrder.includes(k))]

 const csvContent = [
   headers.join(','), 
   ...mergedData.map(row => headers.map(fieldName => {
     let val = row[fieldName]
     if (val === undefined || val === null) val = ''
     if (typeof val === 'object') val = JSON.stringify(val).replace(/\"/g, '\"\"') 
     if (typeof val === 'string' && (val.includes(',') || val.includes('"') || val.includes('\n'))) {
       val = `"${val.replace(/"/g, '""')}"`
     }
     return val
   }).join(','))
 ].join('\n')

 const blob = new Blob(["\ufeff" + csvContent], { type: 'text/csv;charset=utf-8;' })
 const url = URL.createObjectURL(blob)
 const link = document.createElement('a')
 link.setAttribute('href', url)
 link.setAttribute('download', `${filename}.csv`)
 document.body.appendChild(link)
 link.click()
 document.body.removeChild(link)
}

export function downloadTreatmentRecordsXLSX(logs, filename) {
  if (!logs || logs.length === 0) {
    alert("暂无数据可导出")
    return
  }

  // 状态映射
  const statusMap = {
    '自动完成': '自动完成',
    '手动停止': '手动停止',
    'RUNNING': '运行中',
    'PAUSED': '已暂停',
    'FINISHED': '已完成',
    'CANCELLED': '已取消',
    'FAILED': '失败'
  }

  // 处理 Sheet1 数据
  const sheet1Data = logs.map(log => {
    // 计算设备数量和平均值
    let deviceCount = 0
    let avgDim = null
    let avgCctK = null
    let metricsJson = ''

    if (log.metrics) {
      metricsJson = JSON.stringify(log.metrics)
      if (log.metrics.deviceConfigs && Array.isArray(log.metrics.deviceConfigs)) {
        deviceCount = log.metrics.deviceConfigs.length
        
        // 计算平均亮度和色温
        const dims = log.metrics.deviceConfigs.map(config => config.dim).filter(val => val !== undefined && val !== null)
        const cctKs = log.metrics.deviceConfigs.map(config => config.cctK).filter(val => val !== undefined && val !== null)
        
        if (dims.length > 0) {
          avgDim = dims.reduce((sum, val) => sum + val, 0) / dims.length
        }
        if (cctKs.length > 0) {
          avgCctK = cctKs.reduce((sum, val) => sum + val, 0) / cctKs.length
        }
      }
    }

    // 转换时长为分钟
    let durationMinutes = 0
    if (log.duration_actual) {
      // 从 duration_actual 字段中提取分钟数，例如 "1 分钟"
      if (typeof log.duration_actual === 'string') {
        const match = log.duration_actual.match(/\d+/)
        if (match) {
          durationMinutes = parseInt(match[0])
        }
      } else if (typeof log.duration_actual === 'number') {
        durationMinutes = log.duration_actual
      }
    }

    // 转换状态为中文
    let status = log.status
    if (statusMap[status]) {
      status = statusMap[status]
    }

    // 提取开始时间（如果格式为 "HH:MM - HH:MM"）
    let startTime = log.start_time
    if (typeof startTime === 'string' && startTime.includes(' - ')) {
      startTime = startTime.split(' - ')[0]
    }

    return {
      '记录ID': log.id,
      '受试者ID': log.user_id,
      '受试者姓名': log.user_name || '',
      '治疗日期': log.date,
      '开始时间': startTime,
      '结束时间': log.end_time,
      '时长(分钟)': durationMinutes,
      '方案名称': log.scheme_name,
      '状态': status,
      '设备数量': deviceCount,
      '平均亮度(%)': avgDim,
      '平均色温(K)': avgCctK,
      '原始metrics(JSON)': metricsJson
    }
  })

  // 处理 Sheet2 数据
  const sheet2Data = []
  logs.forEach(log => {
    if (log.metrics && log.metrics.deviceConfigs && Array.isArray(log.metrics.deviceConfigs)) {
      log.metrics.deviceConfigs.forEach((config, index) => {
        sheet2Data.push({
          '记录ID': log.id,
          '受试者ID': log.user_id,
          '治疗日期': log.date,
          '方案名称': log.scheme_name,
          '设备序号': index + 1,
          '设备SN': config.deviceSn || '',
          '亮度(%)': config.dim,
          '色温(K)': config.cctK,
          '天光亮度(%)': config.skyDim
        })
      })
    }
  })

  // 创建工作簿
  const wb = XLSX.utils.book_new()

  // 创建 Sheet1
  const sheet1Headers = ['记录ID', '受试者ID', '受试者姓名', '治疗日期', '开始时间', '结束时间', '时长(分钟)', '方案名称', '状态', '设备数量', '平均亮度(%)', '平均色温(K)', '原始metrics(JSON)']
  const sheet1Ws = XLSX.utils.json_to_sheet(sheet1Data, { header: sheet1Headers })
  XLSX.utils.book_append_sheet(wb, sheet1Ws, '治疗记录')

  // 创建 Sheet2
  let sheet2Ws = null
  if (sheet2Data.length > 0) {
    const sheet2Headers = ['记录ID', '受试者ID', '治疗日期', '方案名称', '设备序号', '设备SN', '亮度(%)', '色温(K)', '天光亮度(%)']
    sheet2Ws = XLSX.utils.json_to_sheet(sheet2Data, { header: sheet2Headers })
    XLSX.utils.book_append_sheet(wb, sheet2Ws, '设备明细')
    
    // 启用筛选
    sheet2Ws['!autofilter'] = { ref: sheet2Ws['!ref'] }
    
    // 冻结首行
    sheet2Ws['!freeze'] = { row: 1, col: 0 } // Sheet2 冻结首行
    
    // 设置列宽
    sheet2Ws['!cols'] = [
      { wch: 12 }, // 记录ID
      { wch: 12 }, // 受试者ID
      { wch: 12 }, // 治疗日期
      { wch: 15 }, // 方案名称
      { wch: 10 }, // 设备序号
      { wch: 15 }, // 设备SN
      { wch: 10 }, // 亮度(%)
      { wch: 10 }, // 色温(K)
      { wch: 12 }  // 天光亮度(%)
    ]
  }

  // 启用筛选
  sheet1Ws['!autofilter'] = { ref: sheet1Ws['!ref'] }

  // 冻结首行
  sheet1Ws['!freeze'] = { row: 1, col: 3 } // Sheet1 冻结前 3 列

  // 设置列宽
  sheet1Ws['!cols'] = [
    { wch: 12 }, // 记录ID
    { wch: 12 }, // 受试者ID
    { wch: 12 }, // 受试者姓名
    { wch: 12 }, // 治疗日期
    { wch: 10 }, // 开始时间
    { wch: 10 }, // 结束时间
    { wch: 10 }, // 时长(分钟)
    { wch: 15 }, // 方案名称
    { wch: 10 }, // 状态
    { wch: 10 }, // 设备数量
    { wch: 12 }, // 平均亮度(%)
    { wch: 12 }, // 平均色温(K)
    { wch: 40 }  // 原始metrics(JSON)
  ]

  // 导出为 xlsx 文件
  XLSX.writeFile(wb, `${filename}.xlsx`)
}
