export function exportDatabase(dbState) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
  const filename = `光疗系统备份_${timestamp}.json`
  const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(dbState, null, 2))
  const downloadAnchorNode = document.createElement('a')
  downloadAnchorNode.setAttribute("href", dataStr)
  downloadAnchorNode.setAttribute("download", filename)
  document.body.appendChild(downloadAnchorNode)
  downloadAnchorNode.click()
  downloadAnchorNode.remove()
  alert(`数据库备份成功，文件名：${filename}`)
}
