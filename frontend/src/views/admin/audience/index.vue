<template>
  <div class="page-body" :class="isAdminSide ? 'theme-admin' : 'theme-user'">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">智能人群圈选</h1>
          <span class="title-tag">AUDIENCE BUILDER</span>
        </div>
        <p class="page-desc">基于用户属性和行为标签，灵活组合圈选条件，精准筛选目标人群。</p>
      </div>
      <router-link :to="packagesPath" class="header-link">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
        <span>管理人群包</span>
      </router-link>
    </div>

    <!-- ═══ 快速模板 ═══ -->
    <div class="template-bar">
      <span class="template-lbl">快速模板</span>
      <button v-for="(t,i) in presets" :key="i" class="template-chip" @click="applyPreset(t)">{{ t.label }}</button>
    </div>

    <!-- ═══ 条件构建区 — Double-Bezel ═══ -->
    <div class="builder-outer"><div class="builder-inner">
      <div class="builder-top">
        <div class="builder-top-left">
          <h2 class="section-heading">圈选条件</h2>
          <span class="condition-count">{{ conditions.length }} 个条件</span>
        </div>
        <div class="logic-selector">
          <span class="logic-lbl">连接逻辑</span>
          <ElTooltip placement="top" :show-after="200" content="多个条件混用'且/或'时，会先按'且'再按'或'计算。例如：A 且 B 或 C 等于 (A 且 B) 或 C。全部都用'且'或全部都用'或'就不会有歧义。" raw-content>
            <span class="logic-help">?</span>
          </ElTooltip>
          <div class="logic-group">
            <button class="logic-btn" @click="setAllLogic('AND')">全部设为且 <span class="logic-sub">AND</span></button>
            <button class="logic-btn" @click="setAllLogic('OR')">全部设为或 <span class="logic-sub">OR</span></button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="conditions.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/><path d="M8 11h6"/><path d="M11 8v6"/></svg>
        <p class="empty-title">暂无圈选条件</p>
        <p class="empty-desc">点击下方按钮添加条件，或使用上方的快速模板</p>
      </div>

      <!-- 条件列表 -->
      <div class="conditions-list">
        <template v-for="(c, i) in conditions" :key="i">
          <!-- 条件间独立逻辑：连接器放在两个条件卡之间（横置，居中） -->
          <div v-if="i > 0" class="cond-connector">
            <div class="connector-line"></div>
            <div class="connector-sel">
              <ElSelect v-model="c.logicOp" size="small" class="connector-sel-ctrl" title="混用时先按'且'再按'或'" @change="onChange">
                <ElOption label="且 AND" value="AND" />
                <ElOption label="或 OR" value="OR" />
              </ElSelect>
            </div>
          </div>
          <div class="condition-card">
            <div class="cond-body">
              <div class="cond-row">
                <ElSelect v-model="c.field" placeholder="选择字段" class="cond-field" @change="onFieldChange(i)">
                  <ElOptionGroup v-for="g in fieldGroups" :key="g" :label="g">
                    <ElOption v-for="f in fieldOptions.filter(x => x.group === g)" :key="f.value" :label="f.label" :value="f.value">
                      <ElTooltip :content="f.desc" placement="top" :show-after="300">
                        <span class="field-opt">{{ f.label }}</span>
                      </ElTooltip>
                    </ElOption>
                  </ElOptionGroup>
                </ElSelect>
              <ElSelect v-model="c.operator" placeholder="运算符" class="cond-op">
                <ElOption v-for="o in getOperators(c.field)" :key="o.value" :label="o.label" :value="o.value" />
              </ElSelect>
              <div class="cond-value-area">
                <template v-if="c.field === 'gender'">
                  <ElSelect v-model="c.value" placeholder="选择" class="cond-val" @change="onChange">
                    <ElOption label="男" value="Male" /><ElOption label="女" value="Female" />
                  </ElSelect>
                </template>
                <template v-else-if="c.field === 'segment_code'">
                  <ElSelect v-model="c.value" placeholder="选择分层" class="cond-val" @change="onChange">
                    <ElOption v-for="s in segmentOptions" :key="s.value" :label="s.label" :value="s.value" />
                  </ElSelect>
                </template>
                <!-- 省份下拉：全量省份，避免手输少打/错字 -->
                <template v-else-if="c.field === 'province'">
                  <ElSelect v-model="c.value" filterable clearable placeholder="选择省份" class="cond-val" @change="onChange">
                    <ElOption v-for="p in provinceOptions" :key="p" :label="p" :value="p" />
                  </ElSelect>
                </template>
                <!-- 城市下拉：按省份分组（第一条就是城市也能看到归属省份），可搜索 -->
                <template v-else-if="c.field === 'city'">
                  <ElSelect v-model="c.value" filterable clearable placeholder="选择城市" class="cond-val" @change="onChange">
                    <ElOptionGroup v-for="g in cityGroupOptions" :key="g.province" :label="g.province">
                      <ElOption v-for="ct in g.cities" :key="ct" :label="ct" :value="ct" />
                    </ElOptionGroup>
                  </ElSelect>
                </template>
                <!-- 偏好品类直选：显示品类名 → 传品类原始 id -->
                <template v-else-if="c.field === 'favorite_category'">
                  <ElSelect v-model="c.value" filterable clearable placeholder="选择品类" class="cond-val" @change="onChange">
                    <ElOption v-for="fc in favoriteCategoryOptions" :key="fc.value" :label="fc.label" :value="fc.value" />
                  </ElSelect>
                </template>
                <!-- 标签条件：两级选择——先选标签类型（中文），再选该标签下的取值（中文） -->
                <template v-else-if="c.field === 'tag_value'">
                  <div class="cond-tag-group">
                    <ElSelect v-model="c.tagCode" placeholder="标签类型" class="cond-tag-type" @change="c.value=''; onChange()">
                      <ElOption v-for="tt in tagTypeOptions" :key="tt.value" :label="tt.label" :value="tt.value" />
                    </ElSelect>
                    <ElSelect v-model="c.value" filterable clearable placeholder="选择取值" class="cond-val" @change="onChange">
                      <ElOption v-for="tv in tagValueOptions(c)" :key="tv.value" :label="tv.label" :value="tv.value" />
                    </ElSelect>
                  </div>
                </template>
                <template v-else-if="c.operator === 'between'">
                  <div class="cond-between">
                    <ElInputNumber v-model="c.valueFrom" :min="0" :max="99999" placeholder="从" controls-position="right" class="between-input" @change="onChange" />
                    <span class="between-sep">—</span>
                    <ElInputNumber v-model="c.valueTo" :min="0" :max="99999" placeholder="到" controls-position="right" class="between-input" @change="onChange" />
                  </div>
                </template>
                <!-- 数字型字段用 InputNumber，防手输非数字 -->
                <template v-else-if="isNumericField(c.field)">
                  <ElInputNumber v-model="c.value" :min="0" :max="99999999" placeholder="输入数值" controls-position="right" class="cond-val" @change="onChange" />
                </template>
                <template v-else>
                  <ElInput v-model="c.value" placeholder="输入值" class="cond-val" clearable @change="onChange" />
                </template>
              </div>
              <button class="cond-del" @click="removeCondition(i)" title="删除条件">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="cond-meta">
              <span class="cond-field-name" :title="getFieldDesc(c.field)">{{ getFieldLabel(c.field) }}</span>
              <span v-if="c.value || c.valueFrom" class="cond-value-preview">{{ getValuePreview(c) }}</span>
            </div>
          </div>
          </div>
        </template>
      </div>

      <button class="add-btn" @click="addCondition">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 5v14M5 12h14"/></svg>
        <span>添加条件</span>
      </button>
    </div></div>

    <!-- ═══ 预估 + 操作 ═══ -->
    <div class="estimate-outer"><div class="estimate-inner">
      <div class="estimate-left">
        <span class="estimate-dot"></span>
        <span class="estimate-lbl">预计命中</span>
        <span class="estimate-num" v-if="!estimating">{{ estimatedCount.toLocaleString() }}</span>
        <span v-else class="estimate-loading"><span class="spinner-sm"></span></span>
        <span class="estimate-unit">人</span>
        <span v-if="estimateDetails.length" class="estimate-detail">
          <span v-for="(d, di) in estimateDetails" :key="di" class="detail-chip">{{ d.label }} <b>{{ d.count.toLocaleString() }}</b></span>
        </span>
      </div>
      <div class="estimate-actions">
        <button class="btn-outline" :disabled="conditions.length === 0" @click="showSaveDialog = true">保存人群包</button>
        <button class="btn-primary" :disabled="conditions.length === 0" @click="doSearch">执行圈选</button>
      </div>
    </div></div>

    <!-- ═══ 圈选结果 ═══ -->
    <div v-if="searched" class="result-outer"><div class="result-inner">
      <div class="result-header">
        <h3 class="result-title">圈选结果（共 {{ resultTotal }} 人）</h3>
        <div class="result-actions">
          <button class="btn-text" @click="searched = false">清除结果</button>
          <button class="btn-outline btn-export-sm" :disabled="!resultTotal" @click="doExportResult">导出 CSV</button>
        </div>
      </div>
      <ElTable :data="resultRows" stripe size="small" max-height="400" v-loading="searching" class="data-table" :fit="true" @row-click="rowClick">
        <ElTableColumn prop="userCode" label="用户编码" min-width="120" />
        <ElTableColumn label="性别" min-width="60">
          <template #default="{ row }">{{ ({'Male':'男','Female':'女'} as Record<string,string>)[row.gender] || row.gender || '-' }}</template>
        </ElTableColumn>
        <ElTableColumn prop="age" label="年龄" min-width="60" align="right" />
        <ElTableColumn prop="province" label="省份" min-width="100" />
        <ElTableColumn prop="city" label="城市" min-width="100" />
        <ElTableColumn prop="segmentName" label="分层" min-width="90" />
        <ElTableColumn label="分层评分" min-width="80" align="right">
          <template #default="{ row }">{{ row.segmentScore != null ? Number(row.segmentScore).toFixed(2) : '-' }}</template>
        </ElTableColumn>
        <ElTableColumn prop="totalOrderCount" label="订单数" min-width="70" align="right" />
        <ElTableColumn label="消费金额" min-width="120" align="right">
          <template #default="{ row }">¥{{ (row.totalPaymentAmount || 0).toLocaleString() }}</template>
        </ElTableColumn>
      </ElTable>
      <div v-if="resultTotal > 20" class="result-page">
        <ElPagination v-model:current-page="resultPage" :page-size="20" :total="resultTotal" layout="prev,pager,next" small @current-change="doSearch" />
      </div>
    </div></div>

    <!-- ═══ 保存对话框 ═══ -->
    <ElDialog v-model="showSaveDialog" width="440px" class="save-dialog">
      <template #header><span class="dialog-title">保存人群包</span></template>
      <ElForm :model="saveForm" label-position="top">
        <ElFormItem label="人群包名称" required><ElInput v-model="saveForm.name" placeholder="如：高价值女性用户" maxlength="50" /></ElFormItem>
        <ElFormItem label="描述"><ElInput v-model="saveForm.desc" type="textarea" :rows="2" placeholder="可选描述" maxlength="200" /></ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showSaveDialog = false">取消</ElButton>
        <ElButton type="primary" @click="doSave" :loading="saving">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { estimateAudience, saveAudiencePackage, searchAudience, exportAudienceCsv } from '@/api/admin'
import { fetchTagDistribution } from '@/api/profile'
import request from '@/utils/http'
defineOptions({ name: 'AudienceSegmentation' })

const router = useRouter()
const route = useRoute()
/** 管理端蓝色 / 用户端青色（共享页面按路由自动切换） */
const isAdminSide = computed(() => !route.path.startsWith('/user'))
/** 人群包管理链接：相对路径在 vue-router 4 + hash 模式下会解析为绝对路径导致 404，用绝对路径按端切换 */
const packagesPath = computed(() => isAdminSide.value ? '/admin/audience/packages' : '/user/audience/packages')
/** 结果行点击 → 跳画像详情 */
function rowClick(row: any) {
  if (row?.userId) router.push(`${route.path.startsWith('/user') ? '/user' : ''}/profiles/${row.userId}`)
}

/** 值输入下拉数据源：省份（全量）/ 城市（按省份分组，不依赖省份条件也能选）/ 标签值（两级：标签类型 + 取值） */
const provinceOptions = ref<string[]>([])
const cityGroupOptions = ref<{ province: string; cities: string[] }[]>([])
/** 标签类型选项（中文）；取值选项见 tagOptions */
const tagTypeOptions = [
  { value: 'ACTIVE_LEVEL', label: '用户活跃度' },
  { value: 'CONSUMPTION_LEVEL', label: '用户消费力' },
  { value: 'FAVORITE_CATEGORY', label: '偏好品类' }
]
/** 英文档位 → 中文显示（ACTIVE_LEVEL/CONSUMPTION_LEVEL 的原始值是 High/Medium/Low） */
const TIER_CN: Record<string, Record<string, string>> = {
  ACTIVE_LEVEL: { High: '高活跃', Medium: '中活跃', Low: '低活跃' },
  CONSUMPTION_LEVEL: { High: '高消费', Medium: '中等消费', Low: '低消费' }
}
/** 标签取值：{ tagCode, label(中文), value(原始值) } */
const tagOptions = ref<{ tagCode: string; label: string; value: string }[]>([])
/** 偏好品类直选：{ label: 品类名, value: 品类原始 id }（与"标签值→偏好品类"同源，独立成字段更直观） */
const favoriteCategoryOptions = ref<{ label: string; value: string }[]>([])
onMounted(async () => {
  const [ps, cs, tags] = await Promise.all([
    request.get<string[]>({ url: '/api/v1/public/all-provinces', showErrorMessage: false }).catch(() => []),
    request.get<{ city: string; province: string }[]>({ url: '/api/v1/public/cities', showErrorMessage: false }).catch(() => []),
    fetchTagDistribution().catch(() => [])
  ])
  provinceOptions.value = ps || []
  cityGroupOptions.value = groupCities(cs)
  // 标签取值：显示名(tagValue，档位转中文) → 原始值(filterTagValue)；过滤 Unknown 并按 (tagCode,value) 去重
  const seen = new Set<string>()
  const list: { tagCode: string; label: string; value: string }[] = []
  const favList: { label: string; value: string }[] = []
  ;(tags || []).forEach((t: any) => {
    const rawLabel = String(t.tagValue || '')
    const val = String(t.filterTagValue ?? t.tagValue ?? '')
    if (!rawLabel || rawLabel === 'Unknown' || seen.has(t.tagCode + '|' + val)) return
    seen.add(t.tagCode + '|' + val)
    const label = TIER_CN[t.tagCode]?.[rawLabel] || rawLabel
    list.push({ tagCode: t.tagCode, label, value: val })
    if (t.tagCode === 'FAVORITE_CATEGORY') favList.push({ label, value: val })
  })
  tagOptions.value = list
  favoriteCategoryOptions.value = favList
})

/** 城市列表 → 按省份分组（下拉显示层级，第一条就是城市也能看到归属省份） */
function groupCities(rows: { city: string; province: string }[] | null) {
  const groups: Record<string, string[]> = {}
  ;(rows || []).forEach(r => { (groups[r.province || '其他'] ??= []).push(r.city) })
  return Object.entries(groups).map(([province, cities]) => ({ province, cities }))
}

/** 某标签类型下的取值选项（按 tagCode 过滤） */
function tagValueOptions(c: any) {
  return c?.tagCode ? tagOptions.value.filter(t => t.tagCode === c.tagCode) : []
}

/** 城市联动：存在省份条件且已选值时，城市下拉只显示该省城市（否则全量分组） */
async function refreshCities() {
  const prov = conditions.value.find(c => c.field === 'province')?.value
  const cs = await request.get<{ city: string; province: string }[]>({
    url: '/api/v1/public/cities',
    params: prov ? { province: prov } : {},
    showErrorMessage: false
  }).catch(() => [])
  cityGroupOptions.value = groupCities(cs)
}

/** 数字型字段：用 ElInputNumber 防输入错误 */
function isNumericField(f: string) {
  return ['age', 'total_payment_amount', 'total_order_count', 'average_order_amount', 'recency_days', 'browse_count_30d', 'login_count_30d', 'segment_score'].includes(f)
}
/** 校验：所有条件值必须填写完整（between 需起止都有；标签条件需标签类型+取值） */
function validateComplete(): boolean {
  const bad = conditions.value.find(c => {
    if (c.field === 'tag_value') return !c.tagCode || c.value === undefined || c.value === null || c.value === ''
    if (c.operator === 'between') return c.valueFrom === undefined || c.valueFrom === null || c.valueTo === undefined || c.valueTo === null
    return c.value === undefined || c.value === null || c.value === ''
  })
  if (bad) { ElMessage.warning('请完整填写所有条件的值'); return false }
  return true
}

interface Condition { field: string; operator: string; value: any; valueFrom?: number; valueTo?: number; tagCode?: string; logicOp?: string }
const conditions = ref<Condition[]>([])
const logic = ref('AND')

const presets = [
  { label:'高价值女性', conditions:[{ field:'gender', operator:'eq', value:'Female' },{ field:'segment_code', operator:'eq', value:'HIGH_VALUE' }] },
  { label:'高价值男性', conditions:[{ field:'gender', operator:'eq', value:'Male' },{ field:'segment_code', operator:'eq', value:'HIGH_VALUE' }] },
  { label:'流失风险用户', conditions:[{ field:'segment_code', operator:'eq', value:'AT_RISK' }] },
  { label:'沉睡用户', conditions:[{ field:'login_count_30d', operator:'lt', value:1 }] },
  { label:'大额消费者', conditions:[{ field:'total_payment_amount', operator:'gt', value:30000 }] },
  { label:'偏好服装鞋靴', conditions:[{ field:'tag_value', operator:'eq', value:'22', tagCode:'FAVORITE_CATEGORY' }] }
]
function applyPreset(t: any) {
  // 清空再 push 强制重建（避免 ElSelect 复用 DOM 不重绑 v-model 导致部分条件值没选上）
  conditions.value.splice(0)
  t.conditions.forEach((c: any) => conditions.value.push({ ...c, logicOp: 'AND' }))
  onChange()
}

/** 字段体系：按维度分组（group 用于下拉分组），desc 用于 hover 说明 */
const fieldOptions = [
  { label:'性别', value:'gender', group:'基础属性', desc:'男 / 女' },
  { label:'年龄', value:'age', group:'基础属性', desc:'按周岁' },
  { label:'省份', value:'province', group:'基础属性', desc:'注册省份' },
  { label:'城市', value:'city', group:'基础属性', desc:'注册城市' },
  { label:'累计消费', value:'total_payment_amount', group:'消费行为', desc:'历史订单总金额' },
  { label:'订单数', value:'total_order_count', group:'消费行为', desc:'累计下单次数' },
  { label:'平均客单价', value:'average_order_amount', group:'消费行为', desc:'累计消费 ÷ 订单数，判断消费档次' },
  { label:'距最近购买天数', value:'recency_days', group:'消费行为', desc:'距最近一次购买的天数（从未购买不计入）' },
  { label:'30天浏览', value:'browse_count_30d', group:'活跃行为', desc:'近 30 天浏览行为次数' },
  { label:'30天登录', value:'login_count_30d', group:'活跃行为', desc:'近 30 天登录次数' },
  { label:'用户分层', value:'segment_code', group:'价值分层', desc:'基于 RFM 评分（近度/频次/金额）的 5 级价值分层' },
  { label:'偏好品类', value:'favorite_category', group:'偏好标签', desc:'基于浏览行为加权评分得到的品类偏好' },
  { label:'标签值（高级）', value:'tag_value', group:'偏好标签', desc:'按标签类型+取值精确圈选（活跃度/消费力/偏好品类）' }
]
const fieldGroups = ['基础属性', '消费行为', '活跃行为', '价值分层', '偏好标签']
const segmentOptions = [
  { value: 'HIGH_VALUE', label: '高价值用户' },
  { value: 'POTENTIAL', label: '潜力用户' },
  { value: 'GENERAL', label: '一般用户' },
  { value: 'AT_RISK', label: '流失风险' },
  { value: 'LOW_VALUE', label: '低价值用户' }
]
const operatorMap: Record<string, {label:string;value:string}[]> = {
  gender:[{label:'等于',value:'eq'},{label:'不等于',value:'neq'}],
  age:[{label:'等于',value:'eq'},{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  province:[{label:'等于',value:'eq'},{label:'包含',value:'contains'}],
  city:[{label:'等于',value:'eq'},{label:'包含',value:'contains'}],
  segment_code:[{label:'等于',value:'eq'},{label:'不等于',value:'neq'}],
  tag_value:[{label:'等于',value:'eq'},{label:'包含',value:'contains'}],
  favorite_category:[{label:'等于',value:'eq'}],
  total_payment_amount:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  total_order_count:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  average_order_amount:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  recency_days:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  browse_count_30d:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  login_count_30d:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
}
function getOperators(field:string){return operatorMap[field]||[{label:'等于',value:'eq'}]}
const defaultOp:Record<string,string>={gender:'eq',age:'gt',province:'eq',city:'eq',segment_code:'eq',tag_value:'contains',favorite_category:'eq',total_payment_amount:'gt',total_order_count:'gt',average_order_amount:'gt',recency_days:'gt',browse_count_30d:'gt',login_count_30d:'gt'}
function getFieldLabel(v:string){return fieldOptions.find(f=>f.value===v)?.label||v}
function getFieldDesc(v:string){return fieldOptions.find(f=>f.value===v)?.desc||''}
function getValuePreview(c:any){
  if(c.operator==='between')return`${c.valueFrom||0} — ${c.valueTo||0}`
  if(c.field==='gender')return({Male:'男',Female:'女'} as Record<string,string>)[c.value]||c.value||''
  if(c.field==='favorite_category'){const hit=favoriteCategoryOptions.value.find(f=>f.value===String(c.value));return hit?hit.label:c.value||''}
  if(c.field==='tag_value'){const hit=tagOptions.value.find(t=>t.tagCode===c.tagCode&&t.value===String(c.value));return hit?hit.label:c.value||''}
  if(c.field==='segment_code'){const hit=segmentOptions.find((s:any)=>s.value===c.value);return hit?hit.label:c.value||''}
  return c.value||''
}

function addCondition(){
  const d = 'gender'
  conditions.value.push({ field: d, operator: defaultOp[d] || 'eq', value: '', logicOp: 'AND' })
}
function removeCondition(i:number){conditions.value.splice(i,1);onChange()}
/** 批量设置所有非首条件的连接逻辑（AND/OR） */
function setAllLogic(op: 'AND'|'OR') {
  conditions.value.forEach((c: any, i: number) => { if (i > 0) c.logicOp = op })
  onChange()
}
function onFieldChange(i:number){
  const c=conditions.value[i]
  c.operator=defaultOp[c.field]||'eq'
  c.value='';c.valueFrom=undefined;c.valueTo=undefined
  if (c.field === 'tag_value' && !c.tagCode) c.tagCode = 'ACTIVE_LEVEL'
  onChange()
}

const estimatedCount=ref(0);const estimating=ref(false);let debounceTimer:any=null
/** 预估明细：每个条件独立命中人数（帮用户理解哪个条件是瓶颈） */
const estimateDetails=ref<{label:string;count:number}[]>([])
function buildCond(c:any){const cond:any={field:c.field,operator:c.operator};if(c.operator==='between')cond.value=[c.valueFrom||0,c.valueTo||0];else cond.value=c.value;if(c.field==='tag_value')cond.tagCode=c.tagCode;if(c.logicOp)cond.logicOp=c.logicOp;return cond}
function onChange(){clearTimeout(debounceTimer);debounceTimer=setTimeout(doEstimate,400)}
async function doEstimate(){
  if(conditions.value.length===0){estimatedCount.value=0;estimateDetails.value=[];return}
  const list=conditions.value.map(buildCond)
  estimating.value=true
  try{
    const res=await estimateAudience({conditions:list,logic:logic.value})
    estimatedCount.value=(res as any)?.data?.count??(res as any)?.count??0
    // 明细：每个条件独立预估（双 fallback 兼容 axios interceptor 可能 unwrap Result wrapper）
    const details=await Promise.all(conditions.value.map(async c => {
      try {
        const r: any = await estimateAudience({conditions:[buildCond(c)],logic:'AND'})
        return Number(r?.data?.count ?? r?.count ?? 0)
      } catch (e) {
        console.warn('[audience] 条件明细预估失败', c.field, e)
        return 0
      }
    }))
    estimateDetails.value=conditions.value.map((c,i)=>({
      label:`${getFieldLabel(c.field)} ${getValuePreview(c)}`,
      count:details[i]
    }))
  }catch{estimatedCount.value=0;estimateDetails.value=[]}finally{estimating.value=false}}

const searched=ref(false);const searching=ref(false);const resultRows=ref<any[]>([]);const resultTotal=ref(0);const resultPage=ref(1)
async function doSearch(){
  if (!validateComplete()) return
  const list=conditions.value.map(buildCond)
  searching.value=true;try{const res=await searchAudience({conditions:list,logic:logic.value,page:resultPage.value-1,size:20})as any;resultRows.value=res?.records??res?.list??[];resultTotal.value=res?.total??resultRows.value.length;searched.value=true}catch(e:any){ElMessage.error('圈选查询失败：'+(e?.message||'未知错误'))}finally{searching.value=false}}

const showSaveDialog=ref(false);const saving=ref(false);const saveForm=reactive({name:'',desc:''})

/** 导出圈选结果 CSV（复用当前条件，后端全量导出） */
function doExportResult() {
  if (!validateComplete() || !conditions.value.length) return
  exportAudienceCsv({ conditions: conditions.value.map(buildCond), logic: logic.value }).then((blob: any) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `圈选结果_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  }).catch(() => ElMessage.error('导出失败，请重试'))
}
async function doSave(){
  if(!saveForm.name.trim()){ElMessage.warning('请输入人群包名称');return}
  if(!validateComplete()) return
  saving.value=true;try{const list=conditions.value.map(buildCond)
  await saveAudiencePackage({packageName:saveForm.name.trim(),description:saveForm.desc.trim(),conditions:list,logic:logic.value});ElMessage.success('人群包保存成功');showSaveDialog.value=false;saveForm.name='';saveForm.desc=''}catch(e:any){ElMessage.error('保存失败：'+(e?.message||''))}finally{saving.value=false}}

watch(conditions,onChange,{deep:true});watch(logic,onChange);watch(conditions,()=>{refreshCities()},{deep:true})
</script>

<style scoped>
.page-body{font-family:'Inter','PingFang SC',sans-serif}
/* 主题变量：管理端蓝色 / 用户端青色（共享页面按路由自动切换） */
.theme-admin.page-body{--acc:#2563eb;--acc-dark:#1d4ed8;--acc-soft:rgba(37,99,235,.08);--acc-line:#93c5fd;--acc-faint:rgba(37,99,235,.05)}
.theme-user.page-body{--acc:#0d9488;--acc-dark:#0f766e;--acc-soft:rgba(13,148,136,.08);--acc-line:#5eead4;--acc-faint:rgba(13,148,136,.05)}
/* 用户端容器内 Element 组件主色 → 青色（按钮/下拉/对话框），管理端保持默认 */
.theme-user{--el-color-primary:#0d9488;--el-color-primary-light-3:#14b8a6;--el-color-primary-light-5:#5eead4;--el-color-primary-light-8:#ccfbf1}
/* ═══ 页面头部（企业级统一风格） ═══ */
.page-header{
  display:flex;align-items:flex-end;justify-content:space-between;gap:20px;
  margin-bottom:20px;padding-bottom:18px;
  border-bottom:1px solid #eef2f6;
}
.ph-left{min-width:0}
.ph-title-row{display:flex;align-items:center;gap:10px}
.title-accent{
  width:4px;height:20px;border-radius:2px;flex-shrink:0;
}
.theme-admin .title-accent{background:linear-gradient(180deg,#2563eb 0%,#60a5fa 100%)}
.theme-user .title-accent{background:linear-gradient(180deg,#0d9488 0%,#5eead4 100%)}
.page-title{
  font-size:22px;font-weight:700;color:#0f172a;margin:0;
  font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif;
  letter-spacing:-0.3px;line-height:1.2;
}
.title-tag{
  font-size:10px;font-weight:600;letter-spacing:1.2px;color:#94a3b8;
  background:#f1f5f9;border-radius:4px;padding:2px 6px;
  font-family:'JetBrains Mono',monospace;text-transform:uppercase;
}
.page-desc{font-size:13px;color:#64748b;margin:8px 0 0 14px;line-height:1.6;max-width:600px}
.header-link{display:inline-flex;align-items:center;gap:6px;padding:6px 14px;border-radius:8px;border:1.5px solid var(--default-border);color:#7987a1;font-size:13px;font-weight:500;text-decoration:none;;transition:all .2s;flex-shrink:0}
.header-link:hover{border-color:var(--acc);color:var(--acc);background:var(--acc-soft)}

/* ═══ TEMPLATES ═══ */
.template-bar{display:flex;align-items:center;gap:8px;margin-bottom:16px;flex-wrap:wrap}
.template-lbl{font-size:12px;font-weight:600;color:#949eb7;margin-right:4px}
.template-chip{padding:5px 14px;border-radius:20px;border:1.5px solid var(--default-border);background:var(--default-box-color);font-size:12px;font-weight:500;color:#7987a1;cursor:pointer;;transition:all .2s}
.template-chip:hover{border-color:var(--acc);color:var(--acc);background:var(--acc-soft)}

/* ═══ BUILDER ═══ */
.builder-outer{padding:1.5px;border-radius:14px;background:rgba(0,0,0,.025);margin-bottom:16px}
.builder-inner{border-radius:calc(14px-1.5px);background:var(--default-box-color);padding:20px 24px 24px;border:1px solid var(--default-border)}
.builder-top{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}
.builder-top-left{display:flex;align-items:center;gap:10px}
.section-heading{font-size:15px;font-weight:700;color:#323251;margin:0;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}
.condition-count{font-size:11px;color:#dbdfe1;font-weight:500}
.logic-selector{display:flex;align-items:center;gap:8px}
.logic-lbl{font-size:12px;color:#dbdfe1}
.logic-help{display:inline-flex;align-items:center;justify-content:center;width:14px;height:14px;border-radius:50%;background:#f2f4f5;color:#949eb7;font-size:10px;font-weight:700;cursor:help;flex-shrink:0}
.logic-group{display:flex;border-radius:8px;overflow:hidden;border:1.5px solid var(--default-border)}
.logic-btn{padding:6px 14px;border:none;cursor:pointer;font-size:12px;font-weight:600;;transition:all .15s;background:transparent;color:#949eb7;display:flex;align-items:center;gap:4px}
.logic-btn.active{background:var(--acc);color:#FFF}
.logic-sub{font-size:10px;font-weight:400;opacity:.7}

/* ═══ EMPTY ═══ */
.empty-state{text-align:center;padding:28px 20px}
.empty-title{font-size:14px;font-weight:600;color:#949eb7;margin:8px 0 4px}
.empty-desc{font-size:12px;color:#dbdfe1;margin:0}

/* ═══ CONDITIONS ═══ */
.conditions-list{display:flex;flex-direction:column;align-items:flex-start;margin-bottom:14px}
/* 条件卡：按内容自适应宽度（不再强制撑满，保留圆角感） */
.condition-card{display:block;width:100%;box-sizing:border-box;background:#f9fafb;border:1px solid rgba(15,23,42,0.05);border-radius:12px;margin-bottom:0;transition:all .25s cubic-bezier(0.32,0.72,0,1);box-shadow:0 2px 8px rgba(15,23,42,0.02)}
.condition-card:hover{border-color:var(--acc-line);box-shadow:0 6px 18px var(--acc-faint);transform:translateY(-1px)}
/* 连接器：位于两个条件卡之间（横向布局：左短线 + 下拉 + 右短线） */
.cond-connector{display:flex;align-items:center;justify-content:center;gap:8px;padding:8px 0}
.connector-line{flex:1;height:1px;background:#f2f4f5;max-width:100px}
.connector-sel{background:var(--default-box-color);padding:0;border-radius:6px;flex-shrink:0}
.connector-sel-ctrl{width:96px}
.connector-sel-ctrl :deep(.el-select__wrapper){box-shadow:0 0 0 1px #f2f4f5 inset;border-radius:6px;min-height:26px;padding:0 24px 0 10px;font-size:12px}
.connector-sel-ctrl :deep(.el-select__placeholder){font-size:12px;color:#949eb7}
.connector-sel-ctrl :deep(.el-select__selected-item){font-size:12px;color:var(--acc);font-weight:600}
.cond-body{display:block;width:100%;padding:12px 14px;border-radius:10px;background:#f9fafb;border:1px solid #f2f4f5;box-sizing:border-box}
.cond-row{display:grid;grid-template-columns:1fr 1fr 1.5fr auto;gap:8px;align-items:center}
.cond-field{width:auto;min-width:120px}
.cond-op{width:auto;min-width:80px}
.cond-value-area{width:auto;min-width:200px;max-width:100%}
.cond-val{width:100%}
.cond-tag-group{display:flex;gap:8px;align-items:center}
.cond-tag-type{width:140px;flex-shrink:0}
.cond-between{display:flex;align-items:center;gap:6px}
.between-input{width:100%;max-width:140px}
.between-sep{color:#dbdfe1;font-size:13px}
.cond-del{width:30px;height:30px;border-radius:8px;border:none;background:transparent;color:#dbdfe1;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all .15s;flex:0 0 auto}
.cond-del:hover{background:rgba(255,77,79,.08);color:#FF4D4F}
.cond-meta{display:flex;align-items:center;gap:8px;margin-top:6px;padding-left:2px}
.cond-field-name{font-size:11px;color:#dbdfe1;font-weight:500;cursor:help;border-bottom:1px dashed #e6eaeb}
.cond-value-preview{font-size:11px;color:#949eb7}
.field-opt{display:block;line-height:1.4}

.add-btn{display:inline-flex;align-items:center;gap:6px;padding:8px 18px;border-radius:8px;border:1.5px dashed var(--default-border);background:transparent;color:#949eb7;font-size:13px;font-weight:500;cursor:pointer;;transition:all .2s}
.add-btn:hover{border-color:var(--acc);color:var(--acc);background:var(--acc-soft)}

/* ═══ ESTIMATE ═══ */
.estimate-outer{padding:1.5px;border-radius:12px;background:var(--acc-faint);margin-bottom:16px}
.estimate-inner{border-radius:calc(12px-1.5px);background:var(--default-box-color);padding:14px 20px;display:flex;align-items:center;justify-content:space-between;border:1px solid var(--acc-line);flex-wrap:wrap;gap:10px}
.estimate-left{display:flex;align-items:center;gap:8px}
.estimate-dot{width:8px;height:8px;border-radius:50%;background:var(--acc)}
.estimate-lbl{font-size:13px;font-weight:500;color:#7987a1}
.estimate-num{font-size:24px;font-weight:800;color:var(--acc);font-family:'JetBrains Mono','Space Grotesk',monospace;letter-spacing:-.3px}
.estimate-unit{font-size:13px;color:#949eb7}
.estimate-loading{padding:0 8px}
.estimate-detail{display:flex;gap:6px;flex-wrap:wrap;margin-left:14px;padding-left:14px;border-left:1px solid var(--default-border)}
.detail-chip{font-size:11px;color:#949eb7;background:#f9fafb;border-radius:6px;padding:2px 8px;white-space:nowrap}
.detail-chip b{color:var(--acc);font-weight:700;font-variant-numeric:tabular-nums}
.spinner-sm{display:inline-block;width:16px;height:16px;border:2px solid var(--acc-soft);border-top-color:var(--acc);border-radius:50%;animation:spin .6s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}
.estimate-actions{display:flex;gap:8px}
.btn-outline{padding:9px 18px;border-radius:10px;border:1.5px solid var(--default-border);background:transparent;color:#7987a1;font-size:13px;font-weight:500;cursor:pointer;;transition:all .3s cubic-bezier(0.32,0.72,0,1)}
.btn-outline:hover:not(:disabled){border-color:var(--acc);color:var(--acc)}
.btn-outline:disabled{opacity:.4;cursor:not-allowed}
.btn-primary{padding:9px 22px;border-radius:10px;border:none;background:linear-gradient(135deg,var(--acc),var(--acc-dark));color:#FFF;font-size:13px;font-weight:600;cursor:pointer;;transition:all .3s cubic-bezier(0.32,0.72,0,1);box-shadow:0 4px 14px var(--acc-soft)}
.btn-primary:hover:not(:disabled){box-shadow:0 8px 22px var(--acc-soft);transform:translateY(-2px)}
.btn-primary:disabled{opacity:.4;cursor:not-allowed}

/* ═══ RESULT ═══ */
.result-outer{padding:6px;border-radius:20px;background:linear-gradient(180deg,#ffffff,#eef2f7);border:1px solid rgba(15,23,42,0.06);box-shadow:0 8px 28px rgba(15,23,42,0.05)}
.result-inner{border-radius:14px;background:var(--default-box-color);padding:20px 24px 24px;box-shadow:inset 0 1px 1px rgba(255,255,255,0.6)}
.result-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.result-actions{display:flex;align-items:center;gap:12px}
.btn-export-sm{padding:5px 14px;font-size:12px}
.result-title{font-size:15px;font-weight:700;color:#323251;margin:0;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}
.btn-text{background:none;border:none;color:#949eb7;font-size:13px;cursor:pointer;padding:0;}
.btn-text:hover{color:var(--acc)}
.data-table{width:100%}
.data-table :deep(.el-table__row){cursor:pointer}
.theme-admin .data-table :deep(.el-table__row:hover>td){background:#f5f9ff!important}
.theme-user .data-table :deep(.el-table__row:hover>td){background:#f0fdfa!important}
.result-page{display:flex;justify-content:center;padding-top:12px}

/* ═══ DIALOG ═══ */
.dialog-title{font-size:16px;font-weight:700;color:#323251;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}

/* ═══ RESPONSIVE ═══ */
@media(max-width:768px){
  .cond-field{width:100%}.cond-op{width:100%}.cond-value-area{width:100%}
  .estimate-inner{flex-direction:column;text-align:center}
}

/* ─── 条件行下拉统一样式（含 .cond-value-area 下的 ElSelect，与字段/运算符一致） ─── */
.cond-field :deep(.el-select__wrapper),
.cond-op :deep(.el-select__wrapper),
.cond-value-area :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1.5px var(--default-border) inset;
  transition: box-shadow .2s;
}
.cond-field :deep(.el-select__wrapper:hover),
.cond-op :deep(.el-select__wrapper:hover),
.cond-value-area :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1.5px var(--acc-line) inset;
}
.cond-field :deep(.el-select__wrapper.is-focused),
.cond-op :deep(.el-select__wrapper.is-focused),
.cond-value-area :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1.5px var(--acc) inset, 0 0 0 3px var(--acc-soft);
}
</style>