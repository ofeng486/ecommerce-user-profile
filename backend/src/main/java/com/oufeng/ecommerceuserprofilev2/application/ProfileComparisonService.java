package com.oufeng.ecommerceuserprofilev2.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oufeng.ecommerceuserprofilev2.common.BusinessException;
import com.oufeng.ecommerceuserprofilev2.common.ResultCode;
import com.oufeng.ecommerceuserprofilev2.domain.dto.comparison.ComparisonResultVO;
import com.oufeng.ecommerceuserprofilev2.domain.dto.comparison.ComparisonResultVO.DimensionItem;
import com.oufeng.ecommerceuserprofilev2.domain.dto.comparison.ComparisonResultVO.DimensionStat;
import com.oufeng.ecommerceuserprofilev2.domain.dto.segmentation.ConditionDTO;
import com.oufeng.ecommerceuserprofilev2.domain.entity.AudiencePackage;
import com.oufeng.ecommerceuserprofilev2.domain.entity.AudienceRule;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.AudiencePackageMapper;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.AudienceRuleMapper;
import com.oufeng.ecommerceuserprofilev2.infrastructure.mapper.UserProfileQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProfileComparisonService {

    private final AudiencePackageMapper packageMapper;
    private final AudienceRuleMapper ruleMapper;
    private final AudienceSegmentationService segmentationService;
    private final UserProfileQueryMapper queryMapper;

    private static final Map<String, String> AGE_LABELS = new LinkedHashMap<>();
    static {
        AGE_LABELS.put("0-17","0-17岁");AGE_LABELS.put("18-24","18-24岁");
        AGE_LABELS.put("25-30","25-30岁");AGE_LABELS.put("31-40","31-40岁");
        AGE_LABELS.put("41-50","41-50岁");AGE_LABELS.put("50+","50岁以上");
    }

    public ProfileComparisonService(AudiencePackageMapper pm, AudienceRuleMapper rm,
                                     AudienceSegmentationService ss, UserProfileQueryMapper qm) {
        this.packageMapper=pm;this.ruleMapper=rm;this.segmentationService=ss;this.queryMapper=qm;
    }

    public ComparisonResultVO compareProfiles(Long groupAId, Long groupBId) {
        AudiencePackage pkgA=packageMapper.selectById(groupAId);
        AudiencePackage pkgB=packageMapper.selectById(groupBId);
        if(pkgA==null)throw new BusinessException(ResultCode.NOT_FOUND,"人群包 A 不存在");
        if(pkgB==null)throw new BusinessException(ResultCode.NOT_FOUND,"人群包 B 不存在");
        List<Long> idsA=getUserIds(groupAId),idsB=getUserIds(groupBId);
        int ca=idsA.size(),cb=idsB.size();
        if(ca==0&&cb==0)throw new BusinessException(ResultCode.BAD_REQUEST,"两个人群包均为空");
        List<DimensionStat> dims=new ArrayList<>();
        dims.add(cmpGender(idsA,ca,idsB,cb));
        dims.add(cmpAge(idsA,ca,idsB,cb));
        dims.add(cmpSegment(idsA,ca,idsB,cb));
        dims.add(cmpConsume(idsA,ca,idsB,cb));
        dims.add(cmpTags(idsA,ca,idsB,cb));
        return new ComparisonResultVO(pkgA.getPackageName(),pkgB.getPackageName(),ca,cb,dims);
    }

    private DimensionStat cmpGender(List<Long> a,int ta,List<Long> b,int tb){return stat("gender","性别分布",a,ta,b,tb,queryMapper::compareGender);}
    private DimensionStat cmpAge(List<Long> a,int ta,List<Long> b,int tb){Map<String,Integer> mA=safe(a,queryMapper::compareAge),mB=safe(b,queryMapper::compareAge);List<DimensionItem> items=new ArrayList<>();for(String k:AGE_LABELS.keySet()){int ca=mA.getOrDefault(k,0),cb=mB.getOrDefault(k,0);items.add(makeItem(AGE_LABELS.get(k),ca,ta,cb,tb));}return new DimensionStat("age","年龄段分布",items);}
    private DimensionStat cmpSegment(List<Long> a,int ta,List<Long> b,int tb){return stat("segment","用户分层分布",a,ta,b,tb,queryMapper::compareSegment);}
    private DimensionStat cmpConsume(List<Long> a,int ta,List<Long> b,int tb){Map<String,Integer> mA=safe(a,queryMapper::compareConsumptionLevel),mB=safe(b,queryMapper::compareConsumptionLevel);List<DimensionItem> items=new ArrayList<>();for(String k:List.of("High","Medium","Low")){int ca=mA.getOrDefault(k,0),cb=mB.getOrDefault(k,0);items.add(makeItem(labelConsume(k),ca,ta,cb,tb));}return new DimensionStat("consumption","消费力等级分布",items);}
    private DimensionStat cmpTags(List<Long> a,int ta,List<Long> b,int tb){return stat("tag","品类偏好分布",a,ta,b,tb,queryMapper::compareTags);}

    private DimensionStat stat(String dim,String label,List<Long> a,int ta,List<Long> b,int tb,java.util.function.Function<List<Long>,List<Map<String,Object>>> fn){Map<String,Integer> mA=safe(a,fn),mB=safe(b,fn);Set<String> k=new LinkedHashSet<>(mA.keySet());k.addAll(mB.keySet());return buildStat(dim,label,k,mA,ta,mB,tb);}
    private Map<String,Integer> safe(List<Long> ids,java.util.function.Function<List<Long>,List<Map<String,Object>>> fn){return ids.isEmpty()?Map.of():toMap(fn.apply(ids));}

    private String labelConsume(String k){return switch(k){case"High"->"高消费";case"Medium"->"中等消费";case"Low"->"低消费";default->k;};}
    private Map<String,Integer> toMap(List<Map<String,Object>> rows){Map<String,Integer> m=new LinkedHashMap<>();for(Map<String,Object> r:rows)m.put(String.valueOf(r.get("label")),((Number)r.get("cnt")).intValue());return m;}
    private DimensionStat buildStat(String dim,String label,Set<String> keys,Map<String,Integer> mA,int tA,Map<String,Integer> mB,int tB){List<DimensionItem> items=new ArrayList<>();for(String k:keys){int ca=mA.getOrDefault(k,0),cb=mB.getOrDefault(k,0);items.add(makeItem(k,ca,tA,cb,tB));}return new DimensionStat(dim,label,items);}
    private DimensionItem makeItem(String label,int ca,int ta,int cb,int tb){BigDecimal ra=ta>0?div(ca,ta):BigDecimal.ZERO,rb=tb>0?div(cb,tb):BigDecimal.ZERO;return new DimensionItem(label,ca,cb,ra,rb,ra.subtract(rb));}
    private BigDecimal div(int n,int d){return BigDecimal.valueOf(n).divide(BigDecimal.valueOf(d),4,RoundingMode.HALF_UP);}

    private List<Long> getUserIds(Long pid){
        try{
            List<AudienceRule> rules=ruleMapper.selectList(new LambdaQueryWrapper<AudienceRule>().eq(AudienceRule::getPackageId,pid).orderByAsc(AudienceRule::getSortOrder));
            if(rules.isEmpty())return Collections.emptyList();
            List<ConditionDTO> conds=new ArrayList<>();String logic="AND";
            for(AudienceRule r:rules){logic=r.getLogicOp()!=null?r.getLogicOp():"AND";conds.add(new ConditionDTO(r.getFieldName(),r.getOperator(),r.getValue()));}
            return segmentationService.segmentUsers(conds,logic,0,10000).getRecords().stream().map(p->p.userId()).collect(Collectors.toList());
        }catch(Exception e){return Collections.emptyList();}
    }
}
