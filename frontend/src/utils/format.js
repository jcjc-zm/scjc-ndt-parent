export function formatDateTime(isoString) {
  if (!isoString) return '-'
  return isoString.replace('T', ' ').substring(0, 19)
}
