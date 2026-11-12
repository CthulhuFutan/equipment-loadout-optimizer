<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import DeleteConfirmDialog from '@/components/DeleteConfirmDialog.vue'
import RuneFormPreview from '@/components/rune/RuneFormPreview.vue'
import RuneIconSelect from '@/components/rune/RuneIconSelect.vue'
import RuneValueStepper from '@/components/rune/RuneValueStepper.vue'
import RuneActivityPicker from '@/components/rune/RuneActivityPicker.vue'
import { RUNE_SET_TYPES, STAT_TYPES } from '@/utils/constants'
import { createRune, getRune, updateRune } from '@/api/rune'
import {
  allowedMain, clampNum, effectiveName, fmtValue, grindInfo, imagePath, isActivity,
  mainCapOf, QUALITIES, statLabel, subCapOf, subDefaultOf,
  type RuneFormState,
} from '@/utils/RuneFormRules.ts'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => route.name === 'RuneEdit')
const runeId = computed(() => Number(route.params.id))

const NORMAL_DEFAULT_SUB_TYPES = ['致命伤害%', '攻击力%', '致命率%', '命中%']

function defaultNormalSubs(star: number) {
  return NORMAL_DEFAULT_SUB_TYPES.map((statType) => ({ statType, value: subDefaultOf(statType, star) }))
}

function defaultState(): RuneFormState {
  return {
    slotNumber: 1, setType: '冥想', quality: '橙色', starRating: 6, levels: 15, locked: false,
    runeName: '', actImage: '05_spear.png', mainType: '攻击力+', mainValue: 492, extra: null,
    subs: defaultNormalSubs(6),
    grind: -1,
  }
}

const form = reactive<RuneFormState>(defaultState())
const initialSnap = JSON.stringify(form)
const loaded = ref(false)
const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const showErrors = ref(false)
const loadedRune = ref<any>(null)
const subsBaseline = ref('')
const mode = ref<'manual' | 'ocr'>('manual')

const ocr = reactive({ started: false, running: false, thumb: '', fileName: '', source: '' })
const ocrFile = ref<HTMLInputElement | null>(null)
const dropOver = ref(false)

const leaveOpen = ref(false)
const reloadOpen = ref(false)
const overwriteOpen = ref(false)
let leaveAction: (() => void) | null = null
let leaveCancel: (() => void) | null = null
let pendingOcr: { file: File; source: string } | null = null

const dragFrom = ref(-1)
const dragOver = ref(-1)

const isAct = computed(() => isActivity(form.setType))
const isOcrEmpty = computed(() => !isEdit.value && mode.value === 'ocr' && !ocr.started)
const mainPercent = computed(() => form.mainType.endsWith('%'))
const mainCap = computed(() => mainCapOf(form.mainType, form.starRating))
const grind = computed(() => grindInfo(form))

// 主词条
const mainOptions = computed(() =>
  allowedMain(form.slotNumber).map((v) => ({ label: v, value: v, icon: '/runes/info/prefix_option_0101.png' }))
)
const setOptions = computed(() =>
  RUNE_SET_TYPES.map((s) => ({
    label: s.label,
    value: s.value,
    icon: isActivity(s.value) ? '/runes/special/05_spear.png' : imagePath(form.slotNumber, s.value, form.starRating),
  }))
)
// 副词条
const statOptions = computed(() =>
  STAT_TYPES.map((s) => ({ label: s.label, value: s.value, icon: '/runes/info/prefix_option_0103.png' }))
)

// 普通符文：额外/副词条隐藏其他位置已使用的类型；当前值始终保留
function extraStatOptions() {
  if (isAct.value) return statOptions.value
  const used = new Set<string>([form.mainType, ...form.subs.map((s) => s.statType)])
  const current = form.extra?.statType
  return statOptions.value.filter((o) => o.value === current || !used.has(o.value))
}

function subStatOptions(index: number) {
  if (isAct.value) return statOptions.value
  const used = new Set<string>([form.mainType])
  if (form.extra) used.add(form.extra.statType)
  form.subs.forEach((s, i) => {
    if (i !== index) used.add(s.statType)
  })
  const current = form.subs[index]?.statType
  return statOptions.value.filter((o) => o.value === current || !used.has(o.value))
}
const grindOptions = computed(() => {
  const list = [{ label: '不研磨', value: -1, icon: '/runes/info/grind_rune_icon.png' }]
  for (const i of grind.value.candidates) {
    const s = form.subs[i]
    if (!s) continue
    list.push({ label: `${statLabel(s.statType)}${fmtValue(s.statType, s.value)}`, value: i, icon: '/runes/info/grind_rune_icon.png' })
  }
  return list
})

const errorMap = computed<Record<string, string>>(() => {
  if (!showErrors.value) return {}
  const m: Record<string, string> = {}
  for (const it of validate()) if (!m[it.field]) m[it.field] = it.msg
  return m
})
const errOf = (field: string) => errorMap.value[field] || ''

function snapshot() {
  return JSON.stringify(form)
}
const cleanSnap = ref(snapshot())
function markClean() {
  cleanSnap.value = snapshot()
}
function isDirty() {
  return snapshot() !== cleanSnap.value
}
function hasUserData() {
  return snapshot() !== initialSnap
}
function subsSnapshot() {
  return JSON.stringify({ extra: form.extra, subs: form.subs })
}

function syncValues() {
  form.mainValue = mainCapOf(form.mainType, form.starRating)
  if (form.extra) form.extra.value = subDefaultOf(form.extra.statType, form.starRating)
  for (const s of form.subs) s.value = subDefaultOf(s.statType, form.starRating)
}

function normalizeGrind() {
  if (form.grind >= 0 && !grindInfo(form).ok) form.grind = -1
}

function snapMain() {
  const allowed = allowedMain(form.slotNumber)
  if (!allowed.includes(form.mainType)) {
    form.mainType = allowed[0]!
    form.mainValue = mainCapOf(form.mainType, form.starRating)
  }
}

// 槽位切换
function onSlot(n: number) {
  const oldMain = form.mainType
  form.slotNumber = n
  snapMain()
  if (oldMain !== form.mainType) {
    const changes = repairNormalStatDuplicates()
    if (changes.length) showCenterToast(`已自动调整 ${changes.length} 条重复词条`)
  }
  normalizeGrind()
}

// 切换套组选择后更新默认信息
function onSet(v: string | number) {
  const next = String(v)
  const wasAct = isActivity(form.setType)
  form.setType = next
  const act = isActivity(next)
  if (act) {
    form.quality = '橙色'
    form.starRating = 7
    form.levels = 0
    form.grind = -1
    syncValues()
  } else if (wasAct) {
    form.starRating = 6
    form.levels = 15
    form.extra = null
    form.subs = defaultNormalSubs(6)
    form.runeName = ''
    form.grind = -1
    syncValues()
    const changes = repairNormalStatDuplicates()
    showCenterToast(
      changes.length
        ? `已切换为普通符文，词条恢复至默认，并调整 ${changes.length} 条重复词条`
        : '已切换为普通符文，词条恢复至默认',
    )
  }
  normalizeGrind()
}

function onQuality(q: string) {
  if (isAct.value) return
  form.quality = q
}

function onStarChange(v: number | undefined) {
  if (isAct.value || typeof v !== 'number') return
  if (v === form.starRating) return
  form.starRating = clampNum(v, 1, 7)
  syncValues()
  normalizeGrind()
}

function onLevelChange(v: number | undefined) {
  if (isAct.value || typeof v !== 'number') return
  if (v === form.levels) return
  form.levels = clampNum(v, 0, 15)
  normalizeGrind()
}

function onMainType(v: string | number) {
  form.mainType = String(v)
  form.mainValue = mainCapOf(form.mainType, form.starRating)
  const changes = repairNormalStatDuplicates()
  if (changes.length) showCenterToast(`已自动调整 ${changes.length} 条重复词条`)
}

function onExtraType(v: string | number) {
  if (!form.extra) return
  form.extra.statType = String(v)
  form.extra.value = subDefaultOf(form.extra.statType, form.starRating)
}

function onExtraValue(v: number) {
  if (form.extra) form.extra.value = v
}

function onSubType(i: number, v: string | number) {
  const s = form.subs[i]
  if (!s) return
  s.statType = String(v)
  s.value = subDefaultOf(s.statType, form.starRating)
}

function nextUnusedStatType() {
  const used = new Set<string>([form.mainType, ...form.subs.map((s) => s.statType)])
  if (form.extra) used.add(form.extra.statType)
  return STAT_TYPES.find((s) => !used.has(s.value))?.value ?? STAT_TYPES[0]?.value ?? '攻击速度%'
}

function showCenterToast(message: string) {
  ElMessage.closeAll()
  ElMessage({ message, type: 'info', duration: 2600, customClass: 'center-toast' })
}

/** 主词条优先：替换额外/副词条中与前面重复的词条，返回变更描述 */
function repairNormalStatDuplicates(): string[] {
  if (isAct.value) return []
  const used = new Set<string>()
  if (form.mainType) used.add(form.mainType)
  if (form.extra?.statType) used.add(form.extra.statType)
  for (const s of form.subs) if (s.statType) used.add(s.statType)

  const seen = new Set<string>()
  if (form.mainType) seen.add(form.mainType)
  const changes: string[] = []

  const replace = (label: string, holder: { statType: string; value: number }) => {
    if (!holder.statType) return
    if (!seen.has(holder.statType)) {
      seen.add(holder.statType)
      return
    }
    const next = STAT_TYPES.find((s) => !used.has(s.value) && !seen.has(s.value))?.value
    if (!next) return
    changes.push(`${label}：${statLabel(holder.statType)} → ${statLabel(next)}`)
    holder.statType = next
    holder.value = subDefaultOf(next, form.starRating)
    used.add(next)
    seen.add(next)
  }

  if (form.extra) replace('额外词条', form.extra)
  form.subs.forEach((s, i) => replace(`副词条${i + 1}`, s))
  return changes
}

function addExtra() {
  const first = nextUnusedStatType()
  form.extra = { statType: first, value: subDefaultOf(first, form.starRating) }
}

function delExtra() {
  form.extra = null
}

function addSub() {
  if (form.subs.length >= 4) return
  const statType = nextUnusedStatType()
  form.subs.push({ statType, value: subDefaultOf(statType, form.starRating) })
}

function delSub(i: number) {
  if (isEdit.value && form.levels >= 12) {
    ElMessage.warning('等级 ≥12 的符文不允许删除词条')
    return
  }
  if (form.subs.length <= 1) return
  form.subs.splice(i, 1)
  if (form.grind === i) form.grind = -1
  else if (form.grind > i) form.grind -= 1
}

function onGrind(v: string | number) {
  form.grind = Number(v)
}

// 校验
function validate() {
  const out: { field: string; msg: string }[] = []
  const add = (field: string, msg: string) => out.push({ field, msg })
  const allowed = allowedMain(form.slotNumber)
  if (!allowed.includes(form.mainType)) add('main', `本槽不允许该主词条。可选词条：${allowed.join('/')}`)
  const check = (value: unknown, cap: number, field: string) => {
    const n = Number(value)
    if (String(value) === '' || !Number.isFinite(n) || n < 1) add(field, '需大于 0')
    else if (!Number.isInteger(n)) add(field, '需为整数')
    else if (n > cap) add(field, `上限 ${cap}`)
  }
  check(form.mainValue, mainCapOf(form.mainType, form.starRating), 'main')
  if (form.extra) check(form.extra.value, subCapOf(form.extra.statType, form.starRating), 'extra')
  form.subs.forEach((s, i) => check(s.value, subCapOf(s.statType, form.starRating), `sub:${i}`))

  // 普通符文：主词条 / 额外词条 / 副词条之间不允许同类型重复；活动符文允许重复
  if (!isAct.value) {
    const seen = new Map<string, string>()
    if (form.mainType) seen.set(form.mainType, '主词条')
    if (form.extra?.statType) {
      const owner = seen.get(form.extra.statType)
      if (owner) add('extra', `词条重复：${statLabel(form.extra.statType)} 已出现在${owner}（普通符文不允许重复）`)
      else seen.set(form.extra.statType, '额外词条')
    }
    form.subs.forEach((s, i) => {
      if (!s.statType) return
      const owner = seen.get(s.statType)
      if (owner) add(`sub:${i}`, `词条重复：${statLabel(s.statType)} 已出现在${owner}（普通符文不允许重复）`)
      else seen.set(s.statType, `副词条${i + 1}`)
    })
  }

  const g = grindInfo(form)
  if (form.grind >= 0 && !g.ok) add('grind', `当前符文不可研磨：${g.reason}`)
  return out
}

function buildPayload() {
  const subs: { statType: string; statValue: number; sortOrder: number }[] = []
  if (form.extra) subs.push({ statType: form.extra.statType, statValue: Number(form.extra.value), sortOrder: 0 })
  form.subs.forEach((s, i) => subs.push({ statType: s.statType, statValue: Number(s.value), sortOrder: i + 1 }))

  const imageUrl = resolveImageUrl()
  const payload: Record<string, unknown> = {
    slotNumber: form.slotNumber,
    setType: form.setType,
    quality: form.quality,
    starRating: form.starRating,
    levels: form.levels,
    locked: form.locked,
    runeName: isAct.value ? effectiveName(form) : '',
    imageUrl,
    mainStatType: form.mainType,
    mainStatValue: Number(form.mainValue),
    grindSortOrder: form.grind >= 0 ? form.grind + 1 : null,
  }
  if (!isEdit.value || subsSnapshot() !== subsBaseline.value) payload.subStats = subs
  return payload
}

function resolveImageUrl() {
  const derived = imagePath(form.slotNumber, form.setType, form.starRating, form.actImage)
  const old = loadedRune.value
  if (!old || !old.imageUrl || isAct.value) return derived
  const oldName = String(old.imageUrl).split('/').pop() || '01_sword.png'
  const oldDerived = imagePath(old.slotNumber, old.setType, old.starRating, oldName)
  return old.imageUrl === oldDerived ? derived : old.imageUrl
}

function applyRune(r: any) {
  form.slotNumber = Number(r.slotNumber) || 1
  form.setType = String(r.setType || '冥想')
  form.quality = String(r.quality || '橙色')
  form.starRating = Number(r.starRating) || 6
  form.levels = r.levels == null ? 0 : Number(r.levels)
  form.locked = !!r.locked
  form.runeName = r.runeName || ''
  form.mainType = String(r.mainStatType || '攻击力+')
  form.mainValue = Number(r.mainStatValue) || 0
  const img = String(r.imageUrl || '')
  // 普通符文不用筛选图片存入 form，重置
  form.actImage = isActivity(form.setType) ? (img.split('/').pop() || '01_sword.png') : '01_sword.png'

  const list = Array.isArray(r.subStats) ? [...r.subStats] : []
  const extra = list.find((s: any) => Number(s.sortOrder) === 0)
  form.extra = extra ? { statType: String(extra.statType), value: Number(extra.statValue) } : null
  form.subs = list
    .filter((s: any) => Number(s.sortOrder) > 0)
    .sort((a: any, b: any) => Number(a.sortOrder) - Number(b.sortOrder))
    .map((s: any) => ({ statType: String(s.statType), value: Number(s.statValue) }))
  if (!form.subs.length) form.subs = [{ statType: '攻击力%', value: subDefaultOf('攻击力%', form.starRating) }]
  form.grind = r.grindSortOrder != null && Number(r.grindSortOrder) >= 1 ? Number(r.grindSortOrder) - 1 : -1
  loadedRune.value = r
  subsBaseline.value = subsSnapshot()
}

// 编辑加载
async function loadRune(showToast = false) {
  if (!isEdit.value) return
  loading.value = true
  loadError.value = false
  try {
    const res: any = await getRune(runeId.value)
    const r = res?.data
    if (!r) throw new Error('empty')
    applyRune(r)
    loaded.value = true
    markClean()
    if (showToast) ElMessage.success('已重新加载该符文')
  } catch {
    loaded.value = false
    loadError.value = true
  } finally {
    loading.value = false
  }
}

// 同一页面内切换 /runes/:id/edit 时组件会被复用，onMounted 不再执行，需要主动重载
watch(
  () => route.fullPath,
  async () => {
    if (isEdit.value) {
      loaded.value = false
      loadError.value = false
      showErrors.value = false
      await loadRune()
      return
    }
    if (route.name === 'RuneNew') {
      Object.assign(form, defaultState())
      loadedRune.value = null
      subsBaseline.value = ''
      loaded.value = true
      loadError.value = false
      showErrors.value = false
      markClean()
    }
  }
)

function onBack() {
  if (saving.value) {
    ElMessage.warning('正在保存，请稍候')
    return
  }
  router.push({ name: 'RuneList' })
}

function onLeaveConfirm() {
  leaveOpen.value = false
  leaveCancel = null
  const go = leaveAction
  leaveAction = null
  if (go) go()
}

function onLeaveCancel() {
  leaveOpen.value = false
  leaveAction = null
  const cancel = leaveCancel
  leaveCancel = null
  if (cancel) cancel()
}

/* 非按钮关闭路由守卫 */
watch(leaveOpen, (open) => {
  if (open || !leaveCancel) return
  const cancel = leaveCancel
  leaveCancel = null
  leaveAction = null
  cancel()
})

function onResetOrReload() {
  if (saving.value) return
  if (!isEdit.value) {
    Object.assign(form, defaultState())
    showErrors.value = false
    markClean()
    return
  }
  if (isDirty()) reloadOpen.value = true
  else void doReload()
}

async function doReload() {
  reloadOpen.value = false
  await loadRune(true)
}

async function onSave() {
  if (saving.value) return
  if (isEdit.value && (!loaded.value || loadError.value)) return
  showErrors.value = true
  const issues = validate()
  if (issues.length) {
    const n = new Set(issues.map((i) => i.field)).size
    ElMessage.error(`有 ${n} 处需要修改`)
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    const submittedSnap = snapshot()
    if (isEdit.value) await updateRune(runeId.value, payload)
    else await createRune(payload)
    cleanSnap.value = submittedSnap
    ElMessage.success(isEdit.value ? '修改已保存' : '保存成功')
    // 解除 saving，避免路由守卫拦截
    saving.value = false
    await router.push({ name: 'RuneList' })
  } catch {

  } finally {
    saving.value = false
  }
}

/* ---------- OCR ---------- */
function pickFile() {
  if (saving.value) return
  ocrFile.value?.click()
}

function onFileChange(e: Event) {
  const el = e.target as HTMLInputElement
  const f = el.files?.[0]
  el.value = ''
  if (f) handleOcrFile(f, '本地截图')
}

function handleOcrFile(f: File, source: string) {
  if (saving.value) return
  if (!f.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件（PNG / JPG）')
    return
  }
  if (f.size > 3 * 1024 * 1024) {
    ElMessage.error('图片不能超过 3MB')
    return
  }
  if (hasUserData()) {
    pendingOcr = { file: f, source }
    overwriteOpen.value = true
    return
  }
  startOcr(f, source)
}

function onOverwriteConfirm() {
  overwriteOpen.value = false
  const p = pendingOcr
  pendingOcr = null
  if (p) startOcr(p.file, p.source)
}

function startOcr(f: File, source: string) {
  ocr.thumb = URL.createObjectURL(f)
  ocr.fileName = f.name
  ocr.source = source
  ocr.running = true
  ocr.started = false
  // TODO 接入真实 OCR 后，在此把识别结果写入 form
  window.setTimeout(() => {
    ocr.running = false
    ocr.started = true
    ElMessage.warning('图片识别引擎尚未接入，已保留当前表单内容，可手动填写')
  }, 700)
}

function clearOcr() {
  Object.assign(form, defaultState())
  ocr.started = false
  ocr.running = false
  ocr.thumb = ''
  ocr.fileName = ''
  ocr.source = ''
  showErrors.value = false
  markClean()
}

function onDrop(e: DragEvent) {
  dropOver.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) handleOcrFile(f, '拖拽上传')
}

function onPaste(e: ClipboardEvent) {
  if (saving.value || isEdit.value || mode.value !== 'ocr') return
  const items = e.clipboardData?.items
  if (!items) return
  for (const it of items) {
    if (it.type.startsWith('image/')) {
      const f = it.getAsFile()
      if (f) {
        handleOcrFile(f, '剪贴板')
        e.preventDefault()
        return
      }
    }
  }
}

// 浏览器刷新 / 关闭标签页时提示
function onBeforeUnload(e: BeforeUnloadEvent) {
  if (!isDirty()) return
  e.preventDefault()
}

/* ---------- drag sort ---------- */
function startDrag(i: number, e: PointerEvent) {
  dragFrom.value = i
  dragOver.value = i
  document.body.classList.add('is-row-dragging')
  e.preventDefault()
}

function onPointerMove(e: PointerEvent) {
  if (dragFrom.value < 0) return
  const el = document.elementFromPoint(e.clientX, e.clientY)
  const row = el?.closest('[data-sub-index]') as HTMLElement | null
  const idx = row ? Number(row.dataset.subIndex) : -1
  if (idx >= 0) dragOver.value = idx
}

function onPointerUp() {
  if (dragFrom.value < 0) return
  const from = dragFrom.value
  const to = dragOver.value
  dragFrom.value = -1
  dragOver.value = -1
  document.body.classList.remove('is-row-dragging')
  if (to < 0 || from === to) return
  moveSub(from, to)
}

function onPointerCancel() {
  if (dragFrom.value < 0) return
  dragFrom.value = -1
  dragOver.value = -1
  document.body.classList.remove('is-row-dragging')
}

function moveSub(from: number, to: number) {
  const arr = form.subs
  if (from < 0 || to < 0 || from >= arr.length || to >= arr.length) return
  const item = arr[from]
  if (!item) return
  arr.splice(from, 1)
  arr.splice(to, 0, item)
  const g = form.grind
  if (g === from) form.grind = to
  else if (g >= 0) {
    if (from < g && to >= g) form.grind = g - 1
    else if (from > g && to <= g) form.grind = g + 1
  }
}

onBeforeRouteLeave((to, from, next) => {
  if (saving.value) {
    ElMessage.warning('正在保存，请稍候')
    next(false)
    return
  }
  if (!isDirty()) {
    next()
    return
  }
  leaveCancel = () => next(false)
  leaveAction = () => next()
  leaveOpen.value = true
})

onMounted(async () => {
  document.addEventListener('pointermove', onPointerMove)
  document.addEventListener('pointerup', onPointerUp)
  document.addEventListener('pointercancel', onPointerCancel)
  document.addEventListener('paste', onPaste)
  window.addEventListener('beforeunload', onBeforeUnload)
  if (isEdit.value) {
    await loadRune()
  } else {
    loaded.value = true
    markClean()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('pointermove', onPointerMove)
  document.removeEventListener('pointerup', onPointerUp)
  document.removeEventListener('pointercancel', onPointerCancel)
  document.removeEventListener('paste', onPaste)
  window.removeEventListener('beforeunload', onBeforeUnload)
  document.body.classList.remove('is-row-dragging')
})
</script>

<template>
  <div class="rune-form-page">
    <div class="page-head">
      <h1>{{ isEdit ? '符文编辑' : '符文添加' }}</h1>
      <button type="button" class="back-btn" :disabled="saving" @click="onBack">
        <svg class="ico-back" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M20 12H4.5" />
          <path d="M10.5 6 4.5 12l6 6" />
        </svg>
        返回
      </button>
    </div>

    <div class="cols">
      <section class="card form-card" v-loading="loading" element-loading-text="正在加载符文数据…" :inert="saving || undefined">
        <div v-if="isEdit && loadError && !loading" class="edit-fail">
          <div class="fail-ic">!</div>
          <b>符文数据加载失败</b>
          <p>未能读取这件符文的信息，可能是网络异常、登录已过期，或这件符文已被删除</p>
          <p>请重试；若仍然失败，可先返回符文仓库列表确认这件符文是否还在</p>
          <div class="fail-ft">
            <el-button @click="onBack">返回列表</el-button>
            <el-button type="primary" @click="loadRune()">重试</el-button>
          </div>
        </div>

        <template v-else-if="!isEdit || loaded">
          <!-- OCR 未识别 -->
          <template v-if="isOcrEmpty">
            <div class="sec sec-top">
              <span>图片识别</span>
              <div class="modes">
                <button type="button" :class="{ on: mode === 'manual' }" @click="mode = 'manual'">
                  <svg class="mode-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="2.5" y="6.5" width="19" height="11" rx="2.5" />
                    <path d="M6 10h.01M9.5 10h.01M13 10h.01M16.5 10h.01M6 13.5h.01M9.5 13.5h.01M13 13.5h.01M16.5 13.5h.01M8 16.5h8" />
                  </svg>
                  手动录入
                </button>
                <button type="button" :class="{ on: mode === 'ocr' }" @click="mode = 'ocr'">
                  <svg class="mode-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="3" y="4.5" width="18" height="15" rx="2.5" />
                    <circle cx="8.5" cy="9.5" r="1.6" />
                    <path d="M4 17.6l4.6-4.6a1.8 1.8 0 0 1 2.5 0L20 20" />
                  </svg>
                  图片识别
                </button>
              </div>
            </div>
            <div v-if="ocr.running" class="loading-box"><span class="spin" />正在识别截图中的词条…</div>
            <template v-else>
              <div
                class="drop"
                :class="{ 'is-over': dropOver }"
                @click="pickFile"
                @dragover.prevent="dropOver = true"
                @dragleave="dropOver = false"
                @drop.prevent="onDrop"
              >
                <div class="plus">＋</div>
                <div class="t">拖拽符文截图到此，或点击选择 / 按 Ctrl+V 粘贴</div>
                <div class="s">建议截取符文详情整块；PNG / JPG，≤ 3MB</div>
              </div>
              <div class="note"><b>流程</b><br />① 选择/粘贴截图 → ② 表单直接填入识别结果，可修改 → ③ 保存。</div>
            </template>
          </template>

          <!-- 表单 -->
          <template v-else>
            <div class="sec sec-top">
              <span>基本信息</span>
              <div v-if="!isEdit" class="modes">
                <button type="button" :class="{ on: mode === 'manual' }" @click="mode = 'manual'">
                  <svg class="mode-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="2.5" y="6.5" width="19" height="11" rx="2.5" />
                    <path d="M6 10h.01M9.5 10h.01M13 10h.01M16.5 10h.01M6 13.5h.01M9.5 13.5h.01M13 13.5h.01M16.5 13.5h.01M8 16.5h8" />
                  </svg>
                  手动录入
                </button>
                <button type="button" :class="{ on: mode === 'ocr' }" @click="mode = 'ocr'">
                  <svg class="mode-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="3" y="4.5" width="18" height="15" rx="2.5" />
                    <circle cx="8.5" cy="9.5" r="1.6" />
                    <path d="M4 17.6l4.6-4.6a1.8 1.8 0 0 1 2.5 0L20 20" />
                  </svg>
                  图片识别
                </button>
              </div>
            </div>

            <div v-if="ocr.started" class="shot">
              <span class="thumb"><img :src="ocr.thumb" alt="" /></span>
              <span class="meta">
                <span class="fn">{{ ocr.fileName }}</span>
                <span class="st">{{ ocr.source }}</span>
              </span>
              <span class="right">
                <el-button size="small" @click="pickFile">重新上传</el-button>
                <el-button size="small" @click="clearOcr">清除</el-button>
              </span>
            </div>

            <div class="base-grid">
              <div class="field sg-l1">
                <span class="lbl">槽位</span>
                <div class="ctl">
                  <div class="chips schips">
                    <button
                      v-for="n in 6" :key="n" type="button" class="chip"
                      :class="{ on: form.slotNumber === n }" @click="onSlot(n)"
                    >{{ n }}</button>
                  </div>
                </div>
              </div>
              <div class="field sg-l2">
                <span class="lbl">套组</span>
                <div class="ctl">
                  <RuneIconSelect
                    :model-value="form.setType"
                    :options="setOptions"
                    :selected-icon-size="25"
                    :option-icon-size="25"
                    :selected-gap="5"
                    :option-gap="5"
                    :icon-offset-y="2"
                    @update:model-value="onSet"
                  />
                </div>
              </div>
              <div class="field sg-l3">
                <span class="lbl">品质</span>
                <div class="ctl">
                  <div class="chips">
                    <button
                      v-for="q in QUALITIES" :key="q.key" type="button" class="chip"
                      :class="['qc-' + q.key, { on: form.quality === q.label }]"
                      :disabled="isAct" @click="onQuality(q.label)"
                    ><i class="qd" :class="'qd-' + q.key" />{{ q.label }}</button>
                  </div>
                </div>
              </div>
              <div class="field sg-r1">
                <span class="lbl">锁定</span>
                <div class="ctl switch-line">
                  <el-switch v-model="form.locked" />
                  <span>{{ form.locked ? '已锁定' : '未锁定' }}</span>
                </div>
              </div>
              <div class="field sg-r2">
                <span class="lbl">星等</span>
                <div class="ctl">
                  <div class="num-box">
                    <RuneValueStepper
                      :model-value="form.starRating" :min="1" :max="7" :show-unit="false" :disabled="isAct"
                      @update:model-value="onStarChange"
                    />
                  </div>
                </div>
              </div>
              <div class="field sg-r3">
                <span class="lbl">等级</span>
                <div class="ctl">
                  <div class="num-box">
                    <RuneValueStepper
                      :model-value="form.levels" :min="0" :max="15" :show-unit="false" :disabled="isAct"
                      @update:model-value="onLevelChange"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div v-if="isAct" class="act-fields">
              <div class="field">
                <span class="lbl">名称</span>
                <div class="ctl">
                  <el-input v-model="form.runeName" :placeholder="`活动${form.slotNumber}号符文`" maxlength="50" />
                </div>
              </div>
              <div class="field">
                <span class="lbl">图片</span>
                <div class="ctl">
                  <RuneActivityPicker v-model="form.actImage" />
                </div>
              </div>
            </div>

            <div class="sec">词条设置</div>
            <div class="stats-panel">
              <div class="sp-part">
                <div class="main-row" :class="{ 'has-err': !!errOf('main') }">
                  <span class="mr-label">
                    <img src="/runes/info/prefix_option_0101.png" alt="" />
                    主词条
                    <el-tag size="small" type="primary">必填</el-tag>
                  </span>
                  <span class="mr-ctl">
                    <RuneIconSelect
                      :model-value="form.mainType" :options="mainOptions"
                      :selected-icon-size="16" :option-icon-size="16"
                      :selected-gap="5" :option-gap="5"
                      :disabled="mainOptions.length === 1" @update:model-value="onMainType"
                    />
                    <span class="ctl-wrap">
                      <RuneValueStepper
                        :model-value="form.mainValue" :max="mainCap" :percent="mainPercent"
                        @update:model-value="(v: number) => (form.mainValue = v)"
                      />
                      <span v-if="errOf('main')" class="ctl-err">{{ errOf('main') }}</span>
                    </span>
                  </span>
                </div>
              </div>

              <div class="sp-part">
                <div class="stat-head">
                  <img src="/runes/info/prefix_option_0102.png" alt="" />
                  <b>额外词条</b>
                  <el-tag size="small" type="info">可选</el-tag>
                  <span class="right">{{ form.extra ? 1 : 0 }}/1</span>
                </div>
                <div v-if="form.extra" class="stat-line" :class="{ 'has-err': !!errOf('extra') }">
                  <span class="drag ghost"><i class="dots" /></span>
                  <RuneIconSelect
                    :model-value="form.extra.statType" :options="extraStatOptions()"
                    :selected-icon-size="16" :option-icon-size="16"
                    :selected-gap="5" :option-gap="5"
                    @update:model-value="onExtraType"
                  />
                  <span class="ctl-wrap">
                    <RuneValueStepper
                      :model-value="form.extra.value" :max="subCapOf(form.extra.statType, form.starRating)"
                      :percent="form.extra.statType.endsWith('%')"
                      @update:model-value="onExtraValue"
                    />
                    <span v-if="errOf('extra')" class="ctl-err">{{ errOf('extra') }}</span>
                  </span>
                  <button type="button" class="del" title="删除额外词条" @click="delExtra">
                    <el-icon><Delete /></el-icon>
                  </button>
                </div>
                <button v-else type="button" class="add" @click="addExtra"><span class="plus-ic">＋</span>添加额外词条</button>
              </div>

              <div class="sp-part">
                <div class="stat-head">
                  <img src="/runes/info/prefix_option_0103.png" alt="" />
                  <b>副词条</b>
                  <span class="right">{{ form.subs.length }}/4</span>
                </div>
                <div
                  v-for="(s, i) in form.subs" :key="i" class="stat-line" :data-sub-index="i"
                  :class="{ 'has-err': !!errOf('sub:' + i), dragging: dragFrom === i, 'drag-over': dragOver === i && dragFrom !== i }"
                >
                  <span class="drag" title="拖拽调整词条顺序" @pointerdown="startDrag(i, $event)"><i class="dots" /></span>
                  <RuneIconSelect :model-value="s.statType" :options="subStatOptions(i)" :selected-icon-size="16" :option-icon-size="16" :selected-gap="5" :option-gap="5" @update:model-value="(v: string | number) => onSubType(i, v)" />
                  <span class="ctl-wrap">
                    <RuneValueStepper
                      :model-value="s.value" :max="subCapOf(s.statType, form.starRating)" :percent="s.statType.endsWith('%')"
                      @update:model-value="(v: number) => (s.value = v)"
                    />
                    <span v-if="errOf('sub:' + i)" class="ctl-err">{{ errOf('sub:' + i) }}</span>
                  </span>
                  <button
                    type="button" class="del" title="删除词条"
                    :disabled="form.subs.length <= 1 || (isEdit && form.levels >= 12)" @click="delSub(i)"
                  >
                    <el-icon><Delete /></el-icon>
                  </button>
                </div>
                <button v-if="form.subs.length < 4" type="button" class="add" style="margin-top: 8px" @click="addSub">
                  <span class="plus-ic">＋</span>添加副词条
                </button>
              </div>

              <div class="sp-part">
                <div class="grind-block">
                  <div class="stat-head grind-head">
                    <img src="/runes/info/grind_rune_icon.png" alt="" />
                    <b>研磨词条</b>
                  </div>
                  <div class="grind-tip">
                    <span class="tip-ic">i</span>
                    <div class="tip-body"><b>研磨状态</b><span>仅副词条可研磨；最多可选择一项词条研磨</span></div>
                  </div>
                  <span class="drag ghost"><i class="dots" /></span>
                  <div class="grind-sel">
                    <RuneIconSelect
                      :model-value="form.grind" :options="grindOptions" :disabled="!grind.ok"
                      :disabled-text="'不研磨 · ' + grind.reason"
                      :selected-icon-size="16" :option-icon-size="16"
                      :selected-gap="5" :option-gap="5"
                      @update:model-value="onGrind"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div class="actions">
              <el-button :disabled="saving" @click="onResetOrReload">{{ isEdit ? '重新加载' : '重置' }}</el-button>
              <el-button
                type="primary" :loading="saving" :disabled="isEdit && (!loaded || loadError)"
                @click="onSave"
              >{{ saving ? '保存中…' : (isEdit ? '保存修改' : '保存') }}</el-button>
            </div>
          </template>
        </template>
      </section>

      <aside class="card side-card">
        <div class="sec">符文预览</div>
        <template v-if="isEdit && loading">
          <div class="pv-empty"><span class="spin" />正在加载…</div>
          <div class="pv-note"><b>提示</b><br />加载完成后，这里会显示符文外观、属性与套组效果。</div>
        </template>
        <div v-else-if="isEdit && loadError" class="pv-empty">加载失败，暂无预览</div>
        <div v-else-if="isOcrEmpty" class="pv-empty">识别出结果后，将于此处显示预览效果</div>
        <RuneFormPreview v-else :state="form" />
      </aside>
    </div>

    <input ref="ocrFile" type="file" accept="image/*" hidden @change="onFileChange" />

    <DeleteConfirmDialog
      v-model="leaveOpen"
      title="尚未保存"
      message="当前修改还没有保存，离开本页后改动会丢失"
      description="确定要离开吗？"
      confirm-text="放弃修改并离开"
      cancel-text="继续编辑"
      danger-color="#409eff"
      @confirm="onLeaveConfirm"
      @cancel="onLeaveCancel"
    />
    <DeleteConfirmDialog
      v-model="reloadOpen"
      title="重新加载？"
      message="表单将恢复为这件符文已保存的数据，当前未保存的修改会丢失"
      description="确定要重新加载吗？"
      confirm-text="重新加载"
      cancel-text="继续编辑"
      danger-color="#409eff"
      @confirm="doReload"
    />
    <DeleteConfirmDialog
      v-model="overwriteOpen"
      title="覆盖当前内容？"
      message="图片识别会把结果写进表单，替换掉你已经填写的字段"
      description="确定用这张截图重新识别吗？"
      confirm-text="覆盖并识别"
      cancel-text="取消"
      danger-color="#409eff"
      @confirm="onOverwriteConfirm"
    />
  </div>
</template>

<style scoped>
.rune-form-page { font-size: 13px; color: #303133; }
:global(.center-toast) {
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
}
.page-head { display: flex; align-items: center; margin-bottom: 14px; }
.page-head h1 { font-size: 18px; font-weight: 700; margin: 0; }
.back-btn {
  margin-left: auto;
  width: 85px;
  height: 38px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  box-sizing: border-box;
  appearance: none;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  color: #606266;
  font: inherit;
  font-size: 13px;
  font-weight: 400;
  line-height: 1;
  cursor: pointer;
}
.back-btn:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background: #ecf5ff;
}
.back-btn:disabled {
  color: #a8abb2;
  background: #f5f7fa;
  border-color: #e4e7ed;
  cursor: not-allowed;
}
.back-btn:disabled:hover {
  color: #a8abb2;
  background: #f5f7fa;
  border-color: #e4e7ed;
}
.back-btn:focus-visible {
  outline: 2px solid #a0cfff;
  outline-offset: 1px;
}
.back-btn .ico-back {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}
.cols {
  display: flex;
  gap: 16px;
  align-items: stretch;
}
.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px 18px;
  box-sizing: border-box;
}
.form-card {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 320px;
}
.side-card {
  flex: 0 0 375px;
  display: flex;
  flex-direction: column;
}
.sec {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  margin: 20px 0 14px;
  padding-left: 2px;
  line-height: 1.25;
}
.sec::before {
  content: '';
  flex: 0 0 auto;
  width: 10px;
  height: 10px;
  transform: rotate(45deg);
  background: linear-gradient(135deg, #a5d6ff 0%, #4aa3ff 42%, #2b7cd3 100%);
}
.sec:first-child {
  margin-top: 0;
}
.sec-top {
  padding: 4px 0 0;
  gap: 10px;
}
.sec-top > span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.modes {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px;
  border-radius: 999px;
  background: #f2f5fa;
}
.modes button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 32px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #606266;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.modes button .mode-ic {
  width: 15px;
  height: 15px;
  flex: 0 0 auto;
}
.modes button:hover {
  color: #409eff;
  background: #ecf5ff;
}
.modes button.on {
  background: #409eff;
  color: #fff;
}
.modes button.on:hover {
  background: #66b1ff;
  color: #fff;
}

.base-grid {
  display: grid;
  grid-template-columns: minmax(0, 421px) max-content;
  column-gap: clamp(28px, 6vw, 100px);
  row-gap: 14px;
  padding-left: 17px;
}
.base-grid .sg-l1 {
  grid-area: 1 / 1;
}
.base-grid .sg-l2 {
  grid-area: 2 / 1;
}
.base-grid .sg-l3 {
  grid-area: 3 / 1;
}
.base-grid .sg-r1 {
  grid-area: 1 / 2;
}
.base-grid .sg-r2 {
  grid-area: 2 / 2;
}
.base-grid .sg-r3 {
  grid-area: 3 / 2;
}
.field {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  column-gap: 18px;
  align-items: center;
  min-width: 0;
}
.lbl {
  color: #606266;
  font-weight: 600;
  white-space: nowrap;
  font-size: 13.5px;
}
.ctl {
  min-width: 0;
  max-width: 380px;
}
.act-fields {
  display: grid;
  grid-template-columns: minmax(0, 421px) minmax(0, 1fr);
  column-gap: clamp(28px, 6vw, 100px);
  row-gap: 14px;
  margin-top: 14px;
  padding-left: 17px;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.chip {
  height: 32px;
  min-width: 56px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #606266;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}
.chip:hover {
  border-color: #c0c4cc;
  color: #409eff;
}
.chip.on {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}
.chip:disabled {
  cursor: not-allowed;
  color: #c0c4cc;
  background: #fafbfc;
}
.chip .qd {
  width: 10px;
  height: 10px;
  display: inline-block;
  vertical-align: -1px;
  margin-right: 6px;
  transform: rotate(45deg);
  border-radius: 2px;
  border: 1px solid rgba(0, 0, 0, .12);
}
.qd-gray {
  background: linear-gradient(135deg, #A6A6A6 0%, #7A7A7A 45%, #4C4C4C 100%);
}
.qd-blue {
  background: linear-gradient(180deg, #7BC0F0 0%, #1E3FA8 100%);
}
.qd-purple {
  background: radial-gradient(circle farthest-side at 100% 100%, #5A1A70 0%, #7A2A94 52%, #8E33AA 99.6%, rgba(142,51,170,0) 100%), linear-gradient(180deg, #8E33AA 0%, #7A2A94 55%, #6A1E86 100%);
}
.qd-orange {
  background: linear-gradient(135deg, #FFB65A 0%, #E07E22 45%, #A5541A 100%);
}
.chip.qc-gray.on {
  border-color: #909399;
  color: #606266;
  background: #f2f2f2;
}
.chip.qc-blue.on {
  border-color: #5FA8E0;
  color: #1f6fb2;
  background: #eef6fd;
}
.chip.qc-purple.on {
  border-color: #7a3f96;
  color: #572A6D;
  background: #f6eefa;
}
.chip.qc-orange.on {
  border-color: #d99b2b;
  color: #9a6110;
  background: #fdf5e4;
}
.schips .chip.on {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.schips .chip.on:hover {
  background: #66b1ff;
  border-color: #66b1ff;
  color: #fff;
}
.switch-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 13px;
}
.stats-panel {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 0 14px;
}
.sp-part {
  padding: 14px 0;
  border-bottom: 1px solid #f0f2f5;
}
.sp-part:last-child {
  border-bottom: 0;
}
.main-row {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  position: relative;
}
.main-row.has-err {
  margin-bottom: 22px;
}
.mr-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.mr-label img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}
.mr-ctl {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 3fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
}
.stat-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #303133;
}
.stat-head img {
  width: 20px;
  height: 20px;
  object-fit: contain;
  flex: 0 0 auto;
}
.stat-head .right {
  margin-left: auto;
  font-size: 11.5px;
  font-weight: 400;
  color: #909399;
}
.stat-line {
  display: grid;
  grid-template-columns: 26px minmax(0, 10fr) minmax(0, 7fr) 40px;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
  position: relative;
}
.stat-line.has-err {
  margin-bottom: 26px;
}
.stat-line.dragging {
  opacity: .5;
}
.stat-line.drag-over {
  background: #ecf5ff;
  outline: 1px dashed #a0cfff;
  outline-offset: 1px;
}
.ctl-wrap {
  position: relative;
  min-width: 0;
  display: block;
}
.stat-line .ctl-wrap {
  margin-left: 10px;
}
.ctl-err {
  position: absolute;
  left: 0;
  top: calc(100% + 2px);
  font-size: 11px;
  line-height: 1.4;
  color: #f56c6c;
  white-space: nowrap;
  z-index: 2;
}
.drag {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  touch-action: none;
  color: #c0c4cc;
}
.drag.ghost {
  cursor: default;
}:global(body.is-row-dragging) {
  user-select: none;
  cursor: grabbing;
}
.dots {
  display: block;
  width: 10px;
  height: 14px;
  background-image: radial-gradient(circle, currentColor 1.25px, transparent 1.3px);
  background-size: 6px 5px;
  background-position: 0 1px;
  opacity: .9;
}
.del {
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  color: #8a97ad;
  cursor: pointer;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.del:hover:not(:disabled) {
  color: #f56c6c;
  background: #fef0f0;
}
.del:disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}
.add {
  width: 100%;
  height: 32px;
  border: 1px dashed #a0cfff;
  border-radius: 4px;
  background: #eef5ff;
  color: #409eff;
  font: inherit;
  font-size: 12.5px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.add:hover {
  border-style: solid;
  border-color: #409eff;
  background: #e6f1ff;
}
.plus-ic {
  font-size: 14px;
  line-height: 1;
}
.grind-block {
  display: grid;
  grid-template-columns: 26px minmax(0, 10fr) minmax(0, 7fr) 40px;
  gap: 8px;
  row-gap: 8px;
  align-items: center;
}
.grind-head {
  grid-column: 1 / 3;
  grid-row: 1;
  margin-bottom: 0;
  gap: 11px;
}
.grind-block > .drag {
  grid-column: 1;
  grid-row: 2;
}
.grind-sel {
  grid-column: 2;
  grid-row: 2;
  min-width: 0;
}
.grind-tip {
  grid-column: 3 / 5;
  grid-row: 1 / span 2;
  align-self: stretch;
  margin: -3px 0 1px 10px;
  display: flex;
  align-items: stretch;
  gap: 8px;
  padding: 3px 10px 10px;
  background: #eff4fb;
  border-radius: 6px;
}
.tip-ic {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  margin-top: 7px;
  border-radius: 50%;
  background: #9db2d6;
  color: #fff;
  font-size: 10px;
  font-style: normal;
  line-height: 14px;
  text-align: center;
  font-weight: 700;
}
.tip-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  font-size: 11.5px;
  line-height: 1.5;
  color: #8a97ad;
}
.tip-body b {
  margin-top: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #2f3b52;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.actions :deep(.el-button) {
  height: 32px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 500;
}
.drop {
  border: 1px dashed #c0c4cc;
  border-radius: 6px;
  background: #fafbfc;
  padding: 24px 16px;
  text-align: center;
  cursor: pointer;
  transition: border-color .15s, background .15s;
}
.drop:hover, .drop.is-over {
  border-color: #409eff;
  background: #f4f9ff;
}
.drop .plus {
  font-size: 24px;
  color: #c0c4cc;
  line-height: 1;
}
.drop .t {
  margin-top: 8px;
  color: #606266;
  font-size: 13px;
}
.drop .s {
  margin-top: 4px;
  color: #a8abb2;
  font-size: 11.5px;
}
.note {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}
.note b {
  color: #606266;
}
.loading-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px 0;
  color: #909399;
}
.spin {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid #c6e2ff;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: sp .7s linear infinite;
}@keyframes sp {
  to {
    transform: rotate(360deg);
  }
}
.shot {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafbfc;
  margin-bottom: 14px;
}
.shot .thumb {
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.shot .thumb img {
  width: 118%;
  height: 118%;
  object-fit: contain;
}
.shot .meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.shot .fn {
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.shot .st {
  color: #909399;
  font-size: 11.5px;
}
.shot .right {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.edit-fail {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 38px 16px 30px;
}
.edit-fail .fail-ic {
  width: 46px;
  height: 46px;
  margin-bottom: 14px;
  border-radius: 50%;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.edit-fail b {
  font-size: 15px;
  color: #303133;
}
.edit-fail p {
  margin: 7px 0 0;
  max-width: 460px;
  font-size: 12.5px;
  line-height: 1.7;
  color: #909399;
}
.edit-fail .fail-ft {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
.pv-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: #c0c4cc;
  font-size: 13px;
  border: 1px dashed #ebeef5;
  border-radius: 6px;
  background: #fafbfc;
}
.pv-note {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}
.pv-note b {
  color: #606266;
}
.num-box {
  width: 124px;
}
:deep(.el-input__wrapper) {
  padding: 1px 9px;
}
:deep(.el-input__inner) {
  font-size: 13px;
}
:deep(.el-tag) {
  font-weight: 600;
}

@media (max-width: 1120px) {
  .cols {
    flex-direction: column;
  }
  .side-card {
    flex: 0 0 auto;
    width: 100%;
  }
}   
@media (max-width: 760px) {
  .base-grid {
  grid-template-columns: minmax(0, 1fr);
}
.base-grid > .field {
  grid-area: auto;
}
.act-fields {
  grid-template-columns: minmax(0, 1fr);
}}
</style>
