<template>
  <div class="page-body">
    <div class="page-header">
      <h1 class="page-title">智能人群圈选</h1>
      <div class="page-header-right">
        <p class="page-desc">基于用户属性和行为标签，灵活组合圈选条件，精准筛选目标人群</p>
        <router-link to="/admin/audience/packages" class="header-link">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
          <span>管理人群包</span>
        </router-link>
      </div>
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
          <span class="logic-lbl">逻辑关系</span>
          <div class="logic-group">
            <button class="logic-btn" :class="{ active: logic === 'AND' }" @click="logic='AND';onChange()">且 <span class="logic-sub">AND</span></button>
            <button class="logic-btn" :class="{ active: logic === 'OR' }" @click="logic='OR';onChange()">或 <span class="logic-sub">OR</span></button>
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
        <div v-for="(c, i) in conditions" :key="i" class="condition-card">
          <div class="cond-connector-area">
            <div v-if="i > 0" class="connector-line"></div>
            <span v-if="i > 0" class="connector-badge" :class="logic === 'AND' ? 'conn-and' : 'conn-or'">{{ logic === 'AND' ? 'AND' : 'OR' }}</span>
          </div>
          <div class="cond-body">
            <div class="cond-row">
              <ElSelect v-model="c.field" placeholder="选择字段" class="cond-field" @change="onFieldChange(i)">
                <ElOption v-for="f in fieldOptions" :key="f.value" :label="f.label" :value="f.value" />
              </ElSelect>
              <ElSelect v-model="c.operator" placeholder="运算符" class="cond-op">
                <ElOption v-for="o in getOperators(c.field)" :key="o.value" :label="o.label" :value="o.value" />
              </ElSelect>
              <div class="cond-value-area">
                <template v-if="c.field === 'gender'">
                  <ElSelect v-model="c.value" placeholder="选择" class="cond-val">
                    <ElOption label="男" value="Male" /><ElOption label="女" value="Female" />
                  </ElSelect>
                </template>
                <template v-else-if="c.field === 'segment_code'">
                  <ElSelect v-model="c.value" placeholder="选择分层" class="cond-val">
                    <ElOption v-for="s in segmentOptions" :key="s" :label="s" :value="s" />
                  </ElSelect>
                </template>
                <template v-else-if="c.operator === 'between'">
                  <div class="cond-between">
                    <ElInputNumber v-model="c.valueFrom" :min="0" :max="99999" placeholder="从" controls-position="right" class="between-input" />
                    <span class="between-sep">—</span>
                    <ElInputNumber v-model="c.valueTo" :min="0" :max="99999" placeholder="到" controls-position="right" class="between-input" />
                  </div>
                </template>
                <template v-else>
                  <ElInput v-model="c.value" placeholder="输入值" class="cond-val" clearable />
                </template>
              </div>
              <button class="cond-del" @click="removeCondition(i)" title="删除条件">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="cond-meta">
              <span class="cond-field-name">{{ getFieldLabel(c.field) }}</span>
              <span v-if="c.value || c.valueFrom" class="cond-value-preview">{{ getValuePreview(c) }}</span>
            </div>
          </div>
        </div>
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
        <button class="btn-text" @click="searched = false">清除结果</button>
      </div>
      <ElTable :data="resultRows" stripe size="small" max-height="400" v-loading="searching" class="data-table">
        <ElTableColumn prop="userId" label="ID" width="80" />
        <ElTableColumn prop="userCode" label="用户编码" width="120" />
        <ElTableColumn prop="gender" label="性别" width="60" />
        <ElTableColumn prop="age" label="年龄" width="60" />
        <ElTableColumn prop="province" label="省份" width="100" />
        <ElTableColumn prop="segmentName" label="分层" width="100" />
        <ElTableColumn prop="totalOrderCount" label="订单数" width="80" />
        <ElTableColumn prop="totalPaymentAmount" label="消费金额" width="120">
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
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { estimateAudience, saveAudiencePackage, searchAudience } from '@/api/admin'
defineOptions({ name: 'AudienceSegmentation' })

interface Condition { field: string; operator: string; value: any; valueFrom?: number; valueTo?: number }
const conditions = ref<Condition[]>([])
const logic = ref('AND')

const presets = [
  { label:'高价值女性', conditions:[{ field:'gender', operator:'eq', value:'Female' },{ field:'segment_code', operator:'eq', value:'HIGH_VALUE' }] },
  { label:'流失风险用户', conditions:[{ field:'segment_code', operator:'eq', value:'AT_RISK' }] },
  { label:'高消费人群', conditions:[{ field:'total_payment_amount', operator:'gt', value:50000 }] },
  { label:'活跃年轻用户', conditions:[{ field:'age', operator:'between', valueFrom:18, valueTo:30 },{ field:'login_count_30d', operator:'gt', value:10 }] },
]
function applyPreset(t: any) { conditions.value = t.conditions.map((c:any)=>({...c})); onChange() }

const fieldOptions = [
  { label:'性别', value:'gender' }, { label:'年龄', value:'age' },
  { label:'省份', value:'province' }, { label:'城市', value:'city' },
  { label:'用户分层', value:'segment_code' }, { label:'标签值', value:'tag_value' },
  { label:'累计消费', value:'total_payment_amount' }, { label:'订单数', value:'total_order_count' },
  { label:'30天浏览', value:'browse_count_30d' }, { label:'30天登录', value:'login_count_30d' },
]
const segmentOptions = ['HIGH_VALUE', 'POTENTIAL', 'GENERAL', 'AT_RISK', 'LOW_VALUE']
const operatorMap: Record<string, {label:string;value:string}[]> = {
  gender:[{label:'等于',value:'eq'},{label:'不等于',value:'neq'}],
  age:[{label:'等于',value:'eq'},{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  province:[{label:'等于',value:'eq'},{label:'包含',value:'contains'}],
  city:[{label:'等于',value:'eq'},{label:'包含',value:'contains'}],
  segment_code:[{label:'等于',value:'eq'},{label:'不等于',value:'neq'}],
  tag_value:[{label:'等于',value:'eq'},{label:'包含',value:'contains'}],
  total_payment_amount:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  total_order_count:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  browse_count_30d:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
  login_count_30d:[{label:'大于',value:'gt'},{label:'小于',value:'lt'},{label:'介于',value:'between'}],
}
function getOperators(field:string){return operatorMap[field]||[{label:'等于',value:'eq'}]}
const defaultOp:Record<string,string>={gender:'eq',age:'gt',province:'eq',city:'eq',segment_code:'eq',tag_value:'contains',total_payment_amount:'gt',total_order_count:'gt',browse_count_30d:'gt',login_count_30d:'gt'}
function getFieldLabel(v:string){return fieldOptions.find(f=>f.value===v)?.label||v}
function getValuePreview(c:any){if(c.operator==='between')return`${c.valueFrom||0} — ${c.valueTo||0}`;return c.value||''}

function addCondition(){const d='gender';conditions.value.push({field:d,operator:defaultOp[d]||'eq',value:''})}
function removeCondition(i:number){conditions.value.splice(i,1);onChange()}
function onFieldChange(i:number){const c=conditions.value[i];c.operator=defaultOp[c.field]||'eq';c.value='';c.valueFrom=undefined;c.valueTo=undefined;onChange()}

const estimatedCount=ref(0);const estimating=ref(false);let debounceTimer:any=null
function onChange(){clearTimeout(debounceTimer);debounceTimer=setTimeout(doEstimate,400)}
async function doEstimate(){if(conditions.value.length===0){estimatedCount.value=0;return}
  const list=conditions.value.map(c=>{const cond:any={field:c.field,operator:c.operator};if(c.operator==='between')cond.value=[c.valueFrom||0,c.valueTo||0];else cond.value=c.value;return cond})
  estimating.value=true;try{const res=await estimateAudience({conditions:list,logic:logic.value});estimatedCount.value=(res as any)?.data?.count??(res as any)?.count??0}catch{estimatedCount.value=0}finally{estimating.value=false}}

const searched=ref(false);const searching=ref(false);const resultRows=ref<any[]>([]);const resultTotal=ref(0);const resultPage=ref(1)
async function doSearch(){const list=conditions.value.map(c=>{const cond:any={field:c.field,operator:c.operator};if(c.operator==='between')cond.value=[c.valueFrom||0,c.valueTo||0];else cond.value=c.value;return cond})
  searching.value=true;try{const res=await searchAudience({conditions:list,logic:logic.value,page:resultPage.value-1,size:20})as any;resultRows.value=res?.records??res?.list??[];resultTotal.value=res?.total??resultRows.value.length;searched.value=true}catch(e:any){ElMessage.error('圈选查询失败：'+(e?.message||'未知错误'))}finally{searching.value=false}}

const showSaveDialog=ref(false);const saving=ref(false);const saveForm=reactive({name:'',desc:''})
async function doSave(){if(!saveForm.name.trim()){ElMessage.warning('请输入人群包名称');return}
  saving.value=true;try{const list=conditions.value.map(c=>{const cond:any={field:c.field,operator:c.operator};if(c.operator==='between')cond.value=[c.valueFrom||0,c.valueTo||0];else cond.value=c.value;return cond})
  await saveAudiencePackage({packageName:saveForm.name.trim(),description:saveForm.desc.trim(),conditions:list,logic:logic.value});ElMessage.success('人群包保存成功');showSaveDialog.value=false;saveForm.name='';saveForm.desc=''}catch(e:any){ElMessage.error('保存失败：'+(e?.message||''))}finally{saving.value=false}}

watch(conditions,onChange,{deep:true});watch(logic,onChange)
</script>

<style scoped>
.page-body{font-family:'Geist','Inter','PingFang SC',sans-serif}
.page-header{margin-bottom:20px;display:flex;align-items:flex-start;justify-content:space-between;gap:16px}
.page-header-right{text-align:right;flex-shrink:0}
.page-title{font-size:22px;font-weight:700;color:var(--art-gray-900);margin:0 0 6px;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif;letter-spacing:-.3px}
.page-desc{font-size:13px;color:var(--art-gray-500);margin:0;line-height:1.6;max-width:600px}
.header-link{display:inline-flex;align-items:center;gap:6px;padding:6px 14px;border-radius:8px;border:1.5px solid var(--default-border);color:var(--art-gray-600);font-size:13px;font-weight:500;text-decoration:none;font-family:'Geist',sans-serif;transition:all .2s;margin-top:4px}
.header-link:hover{border-color:#5D87FF;color:#5D87FF;background:rgba(93,135,255,.04)}

/* ═══ TEMPLATES ═══ */
.template-bar{display:flex;align-items:center;gap:8px;margin-bottom:16px;flex-wrap:wrap}
.template-lbl{font-size:12px;font-weight:600;color:var(--art-gray-500);margin-right:4px}
.template-chip{padding:5px 14px;border-radius:20px;border:1.5px solid var(--default-border);background:var(--default-box-color);font-size:12px;font-weight:500;color:var(--art-gray-600);cursor:pointer;font-family:'Geist',sans-serif;transition:all .2s}
.template-chip:hover{border-color:#5D87FF;color:#5D87FF;background:rgba(93,135,255,.04)}

/* ═══ BUILDER ═══ */
.builder-outer{padding:1.5px;border-radius:14px;background:rgba(0,0,0,.025);margin-bottom:16px}
.builder-inner{border-radius:calc(14px-1.5px);background:var(--default-box-color);padding:20px 24px 24px;border:1px solid var(--default-border)}
.builder-top{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}
.builder-top-left{display:flex;align-items:center;gap:10px}
.section-heading{font-size:15px;font-weight:700;color:var(--art-gray-900);margin:0;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}
.condition-count{font-size:11px;color:var(--art-gray-400);font-weight:500}
.logic-selector{display:flex;align-items:center;gap:8px}
.logic-lbl{font-size:12px;color:var(--art-gray-400)}
.logic-group{display:flex;border-radius:8px;overflow:hidden;border:1.5px solid var(--default-border)}
.logic-btn{padding:6px 14px;border:none;cursor:pointer;font-size:12px;font-weight:600;font-family:'Geist',sans-serif;transition:all .15s;background:transparent;color:var(--art-gray-500);display:flex;align-items:center;gap:4px}
.logic-btn.active{background:#5D87FF;color:#FFF}
.logic-sub{font-size:10px;font-weight:400;opacity:.7}

/* ═══ EMPTY ═══ */
.empty-state{text-align:center;padding:40px 20px}
.empty-title{font-size:14px;font-weight:600;color:var(--art-gray-500);margin:8px 0 4px}
.empty-desc{font-size:12px;color:var(--art-gray-400);margin:0}

/* ═══ CONDITIONS ═══ */
.conditions-list{display:flex;flex-direction:column;margin-bottom:14px}
.condition-card{display:flex;gap:0;position:relative}
.cond-connector-area{display:flex;flex-direction:column;align-items:center;width:40px;flex-shrink:0;padding-top:4px}
.connector-line{width:2px;flex:1;background:var(--art-gray-200);min-height:20px}
.connector-badge{font-size:10px;font-weight:700;padding:2px 8px;border-radius:4px;letter-spacing:.03em}
.conn-and{background:rgba(93,135,255,.1);color:#5D87FF}
.conn-or{background:rgba(255,174,31,.1);color:#FFAE1F}
.cond-body{flex:1;padding:12px 14px;margin-bottom:8px;border-radius:10px;background:var(--art-gray-100);border:1px solid var(--art-gray-200);transition:all .15s}
.cond-body:hover{border-color:#c8dcff}
.cond-row{display:flex;align-items:center;gap:8px;flex-wrap:wrap}
.cond-field{width:160px}
.cond-op{width:120px}
.cond-value-area{min-width:160px;flex:1}
.cond-val{width:100%}
.cond-between{display:flex;align-items:center;gap:6px}
.between-input{width:100%;max-width:140px}
.between-sep{color:var(--art-gray-400);font-size:13px}
.cond-del{width:30px;height:30px;border-radius:8px;border:none;background:transparent;color:var(--art-gray-400);cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all .15s;flex-shrink:0}
.cond-del:hover{background:rgba(255,77,79,.08);color:#FF4D4F}
.cond-meta{display:flex;align-items:center;gap:8px;margin-top:6px;padding-left:2px}
.cond-field-name{font-size:11px;color:var(--art-gray-400);font-weight:500}
.cond-value-preview{font-size:11px;color:var(--art-gray-500)}

.add-btn{display:inline-flex;align-items:center;gap:6px;padding:8px 18px;border-radius:8px;border:1.5px dashed var(--default-border);background:transparent;color:var(--art-gray-500);font-size:13px;font-weight:500;cursor:pointer;font-family:'Geist',sans-serif;transition:all .2s}
.add-btn:hover{border-color:#5D87FF;color:#5D87FF;background:rgba(93,135,255,.04)}

/* ═══ ESTIMATE ═══ */
.estimate-outer{padding:1.5px;border-radius:12px;background:rgba(19,222,185,.06);margin-bottom:16px}
.estimate-inner{border-radius:calc(12px-1.5px);background:var(--default-box-color);padding:14px 20px;display:flex;align-items:center;justify-content:space-between;border:1px solid rgba(19,222,185,.15);flex-wrap:wrap;gap:10px}
.estimate-left{display:flex;align-items:center;gap:8px}
.estimate-dot{width:8px;height:8px;border-radius:50%;background:#13DEB9}
.estimate-lbl{font-size:13px;font-weight:500;color:var(--art-gray-600)}
.estimate-num{font-size:24px;font-weight:800;color:#13DEB9;font-family:'JetBrains Mono','Space Grotesk',monospace;letter-spacing:-.3px}
.estimate-unit{font-size:13px;color:var(--art-gray-500)}
.estimate-loading{padding:0 8px}
.spinner-sm{display:inline-block;width:16px;height:16px;border:2px solid rgba(19,222,185,.2);border-top-color:#13DEB9;border-radius:50%;animation:spin .6s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}
.estimate-actions{display:flex;gap:8px}
.btn-outline{padding:8px 18px;border-radius:8px;border:1.5px solid var(--default-border);background:transparent;color:var(--art-gray-600);font-size:13px;font-weight:500;cursor:pointer;font-family:'Geist',sans-serif;transition:all .2s}
.btn-outline:hover:not(:disabled){border-color:#5D87FF;color:#5D87FF}
.btn-outline:disabled{opacity:.4;cursor:not-allowed}
.btn-primary{padding:8px 22px;border-radius:8px;border:none;background:#5D87FF;color:#FFF;font-size:13px;font-weight:600;cursor:pointer;font-family:'Geist',sans-serif;transition:all .2s;box-shadow:0 2px 8px rgba(93,135,255,.2)}
.btn-primary:hover:not(:disabled){background:#4A7AFF;transform:translateY(-1px)}
.btn-primary:disabled{opacity:.4;cursor:not-allowed}

/* ═══ RESULT ═══ */
.result-outer{padding:1.5px;border-radius:14px;background:rgba(0,0,0,.025)}
.result-inner{border-radius:calc(14px-1.5px);background:var(--default-box-color);padding:20px 24px 24px;border:1px solid var(--default-border)}
.result-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.result-title{font-size:15px;font-weight:700;color:var(--art-gray-900);margin:0;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}
.btn-text{background:none;border:none;color:var(--art-gray-500);font-size:13px;cursor:pointer;padding:0;font-family:'Geist',sans-serif}
.btn-text:hover{color:#5D87FF}
.data-table{width:100%}
.result-page{display:flex;justify-content:center;padding-top:12px}

/* ═══ DIALOG ═══ */
.dialog-title{font-size:16px;font-weight:700;color:var(--art-gray-900);font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}

/* ═══ RESPONSIVE ═══ */
@media(max-width:768px){
  .cond-field{width:100%}.cond-op{width:100%}.cond-value-area{width:100%}
  .estimate-inner{flex-direction:column;text-align:center}
}
</style>