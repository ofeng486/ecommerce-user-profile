package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.domain.dto.comparison.ComparisonResultVO;
import com.oufeng.ecommerceuserprofile.domain.dto.comparison.ComparisonResultVO.DimensionItem;
import com.oufeng.ecommerceuserprofile.domain.dto.comparison.ComparisonResultVO.DimensionStat;
import com.oufeng.ecommerceuserprofile.domain.dto.segmentation.ConditionDTO;
import com.oufeng.ecommerceuserprofile.domain.entity.AudiencePackage;
import com.oufeng.ecommerceuserprofile.domain.entity.AudiencePackageUser;
import com.oufeng.ecommerceuserprofile.domain.entity.AudienceRule;
import com.oufeng.ecommerceuserprofile.domain.mapper.AudiencePackageMapper;
import com.oufeng.ecommerceuserprofile.domain.mapper.AudiencePackageUserMapper;
import com.oufeng.ecommerceuserprofile.domain.mapper.AudienceRuleMapper;
import com.oufeng.ecommerceuserprofile.infrastructure.mapper.UserProfileQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProfileComparisonService {

    private final AudiencePackageMapper packageMapper;
    private final AudienceRuleMapper ruleMapper;
    private final AudienceSegmentationService segmentationService;
    private final UserProfileQueryMapper queryMapper;
    private final AudiencePackageUserMapper packageUserMapper;

    private static final Map<String, String> AGE_LABELS = new LinkedHashMap<>();
    static {
        AGE_LABELS.put("0-17","0-17岁");AGE_LABELS.put("18-24","18-24岁");
        AGE_LABELS.put("25-30","25-30岁");AGE_LABELS.put("31-40","31-40岁");
        AGE_LABELS.put("41-50","41-50岁");AGE_LABELS.put("50+","50岁以上");
    }

    public ProfileComparisonService(AudiencePackageMapper pm, AudienceRuleMapper rm,
                                     AudienceSegmentationService ss, UserProfileQueryMapper qm,
                                     AudiencePackageUserMapper pum) {
        this.packageMapper=pm;this.ruleMapper=rm;this.segmentationService=ss;this.queryMapper=qm;
        this.packageUserMapper=pum;
    }

    public ComparisonResultVO compareProfiles(Long groupAId, Long groupBId) {
        AudiencePackage pkgA=packageMapper.selectById(groupAId);
        AudiencePackage pkgB=packageMapper.selectById(groupBId);
        if(pkgA==null)throw new BusinessException(ResultCode.NOT_FOUND,"人群包 A 不存在");
        if(pkgB==null)throw new BusinessException(ResultCode.NOT_FOUND,"人群包 B 不存在");
        List<Long> idsA=getUserIds(groupAId),idsB=getUserIds(groupBId);
        int ca=idsA.size(),cb=idsB.size();
        if(ca==0&&cb==0)throw new BusinessException(ResultCode.BAD_REQUEST,"两个人群包均为空");
        List<DimensionStat> dims = new ArrayList<>();
        dims.add(cmpGender(idsA, ca, idsB, cb));
        dims.add(cmpAge(idsA, ca, idsB, cb));
        dims.add(cmpSegment(idsA, ca, idsB, cb));
        dims.add(cmpConsume(idsA, ca, idsB, cb));
        dims.add(cmpTag(idsA, ca, idsB, cb, "FAVORITE_CATEGORY", "tag", "品类偏好分布", this::categoryName));
        dims.add(cmpTag(idsA, ca, idsB, cb, "ACTIVE_LEVEL", "activity", "活跃度分布", ProfileComparisonService::labelActive));
        return new ComparisonResultVO(pkgA.getPackageName(), pkgB.getPackageName(), ca, cb, dims);
    }

    private DimensionStat cmpGender(List<Long> a,int ta,List<Long> b,int tb){return stat("gender","性别分布",a,ta,b,tb,queryMapper::compareGender);}
    private DimensionStat cmpAge(List<Long> a,int ta,List<Long> b,int tb){Map<String,Integer> mA=safe(a,queryMapper::compareAge),mB=safe(b,queryMapper::compareAge);List<DimensionItem> items=new ArrayList<>();for(String k:AGE_LABELS.keySet()){int ca=mA.getOrDefault(k,0),cb=mB.getOrDefault(k,0);items.add(makeItem(AGE_LABELS.get(k),ca,ta,cb,tb));}return new DimensionStat("age","年龄段分布",items);}
    private DimensionStat cmpSegment(List<Long> a,int ta,List<Long> b,int tb){return stat("segment","用户分层分布",a,ta,b,tb,queryMapper::compareSegment);}
    private DimensionStat cmpConsume(List<Long> a,int ta,List<Long> b,int tb){Map<String,Integer> mA=safe(a,queryMapper::compareConsumptionLevel),mB=safe(b,queryMapper::compareConsumptionLevel);List<DimensionItem> items=new ArrayList<>();for(String k:List.of("High","Medium","Low")){int ca=mA.getOrDefault(k,0),cb=mB.getOrDefault(k,0);items.add(makeItem(labelConsume(k),ca,ta,cb,tb));}return new DimensionStat("consumption","消费力等级分布",items);}
    /** 通用标签维度对比（tagCode 区分品类/消费力/活跃度等标签） */
    private DimensionStat cmpTag(List<Long> a, int ta, List<Long> b, int tb, String tagCode,
                                 String dim, String label, java.util.function.Function<String, String> labelFn) {
        Map<String, Integer> mA = safe(a, ids -> queryMapper.compareTags(ids, tagCode));
        Map<String, Integer> mB = safe(b, ids -> queryMapper.compareTags(ids, tagCode));
        Set<String> keys = new LinkedHashSet<>(mA.keySet());
        keys.addAll(mB.keySet());
        List<DimensionItem> items = new ArrayList<>();
        for (String k : keys) {
            int ca = mA.getOrDefault(k, 0), cb = mB.getOrDefault(k, 0);
            items.add(makeItem(labelFn.apply(k), ca, ta, cb, tb));
        }
        return new DimensionStat(dim, label, items);
    }

    /** 活跃度标签中文化 */
    private static String labelActive(String k) {
        return switch (k) {
            case "High" -> "高活跃";
            case "Medium" -> "中活跃";
            case "Low" -> "低活跃";
            default -> k;
        };
    }

    /** 分类 id → 中文名（懒加载全表映射；画像对比「品类偏好分布」维度展示用） */
    private Map<Long, String> categoryNames;

    private String categoryName(String id) {
        if (categoryNames == null) {
            Map<Long, String> m = new HashMap<>();
            for (Map<String, Object> r : queryMapper.queryCategoryNames()) {
                Object k = r.get("id");
                if (k != null) m.put(Long.valueOf(k.toString()), String.valueOf(r.get("categoryName")));
            }
            categoryNames = m;
        }
        try { return categoryNames.getOrDefault(Long.valueOf(id), id); } catch (NumberFormatException e) { return "未分类"; }
    }

    private DimensionStat stat(String dim,String label,List<Long> a,int ta,List<Long> b,int tb,java.util.function.Function<List<Long>,List<Map<String,Object>>> fn){Map<String,Integer> mA=safe(a,fn),mB=safe(b,fn);Set<String> k=new LinkedHashSet<>(mA.keySet());k.addAll(mB.keySet());return buildStat(dim,label,k,mA,ta,mB,tb);}
    private Map<String,Integer> safe(List<Long> ids,java.util.function.Function<List<Long>,List<Map<String,Object>>> fn){return ids.isEmpty()?Map.of():toMap(fn.apply(ids));}

    private String labelConsume(String k){return switch(k){case"High"->"高消费";case"Medium"->"中等消费";case"Low"->"低消费";default->k;};}
    private Map<String,Integer> toMap(List<Map<String,Object>> rows){Map<String,Integer> m=new LinkedHashMap<>();for(Map<String,Object> r:rows)m.put(String.valueOf(r.get("label")),((Number)r.get("cnt")).intValue());return m;}
    private DimensionStat buildStat(String dim,String label,Set<String> keys,Map<String,Integer> mA,int tA,Map<String,Integer> mB,int tB){List<DimensionItem> items=new ArrayList<>();for(String k:keys){int ca=mA.getOrDefault(k,0),cb=mB.getOrDefault(k,0);items.add(makeItem(k,ca,tA,cb,tB));}return new DimensionStat(dim,label,items);}
    private DimensionItem makeItem(String label,int ca,int ta,int cb,int tb){BigDecimal ra=ta>0?div(ca,ta):BigDecimal.ZERO,rb=tb>0?div(cb,tb):BigDecimal.ZERO;return new DimensionItem(label,ca,cb,ra,rb,ra.subtract(rb));}
    private BigDecimal div(int n,int d){return BigDecimal.valueOf(n).divide(BigDecimal.valueOf(d),4,RoundingMode.HALF_UP);}

    /** 圈选用户 ID 列表：分页拉全，避免 1 万截断导致大群体对比失真；设安全上限防止超长 IN 查询 */
    private static final int PAGE_SIZE = 5000;
    private static final int MAX_USER_IDS = 50000;

    private List<Long> getUserIds(Long pid) {
        try {
            List<AudienceRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<AudienceRule>().eq(AudienceRule::getPackageId, pid).orderByAsc(AudienceRule::getSortOrder));
            if (!rules.isEmpty()) {
                List<ConditionDTO> conds = new ArrayList<>();
                String logic = "AND";
                for (AudienceRule r : rules) {
                    logic = r.getLogicOp() != null ? r.getLogicOp() : "AND";
                    conds.add(new ConditionDTO(r.getFieldName(), r.getOperator(), r.getValue(), r.getLogicOp()));
                }
                List<Long> ids = new ArrayList<>();
                int page = 0;
                while (true) {
                    List<Long> batch = segmentationService.segmentUsers(conds, logic, page, PAGE_SIZE)
                            .getRecords().stream().map(p -> p.userId()).toList();
                    ids.addAll(batch);
                    if (batch.size() < PAGE_SIZE || ids.size() >= MAX_USER_IDS) break;
                    page++;
                }
                return ids;
            }
            // 指定用户包：audience_rule 无规则时读 audience_package_users 关联表
            return packageUserMapper.selectList(
                            new LambdaQueryWrapper<AudiencePackageUser>().eq(AudiencePackageUser::getPackageId, pid))
                    .stream().map(AudiencePackageUser::getUserId).toList();
        } catch (Exception e) {
            // 底层查询失败必须向调用方暴露，静默返回空列表会误导对比结论
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "人群包用户解析失败: " + e.getMessage());
        }
    }
}
