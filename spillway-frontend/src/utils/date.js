export function formatDate(dateArr) {
  if (!dateArr) return ''
  
  let year, month, day, etc
  [year, month, day, ...etc] = dateArr

  const dateString = `${year}/${month}/${day}`

  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}