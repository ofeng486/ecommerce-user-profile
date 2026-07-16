package com.oufeng.ecommerceuserprofilev2.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofilev2.application.AudienceSegmentationService;
import com.oufeng.ecommerceuserprofilev2.application.ProfileComparisonService;
import com.oufeng.ecommerceuserprofilev2.common.Result;
import com.oufeng.ecommerceuserprofilev2.common.ResultCode;
import com.oufeng.ecommerceuserprofilev2.common.BusinessException;
import com.oufeng.ecommerceuserprofilev2.domain.dto.comparison.ComparisonResultVO;
import com.oufeng.ecommerceuserprofilev2.domain.dto.profile.UserProfileListItemResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.segmentation.ConditionDTO;
import com.oufeng.ecommerceuserprofilev2.domain.entity.AudiencePackage;
import com.oufeng.ecommerceuserprofilev2.domain.entity.AudienceRule;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.AudiencePackageMapper;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.AudienceRuleMapper;
import com.oufeng.ecommerceuserprofilev2.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "人群圈选与对比")
@RestController
@RequestMapping("/api/v1/admin/audience")
public class AdminAudienceController {

    private final AudienceSegmentationService segmentationService;
    private final ProfileComparisonService comparisonService;
    private final AudiencePackageMapper packageMapper;
    private final AudienceRuleMapper ruleMapper;

    public AdminAudienceController(AudienceSegmentationService segmentationService,
                                    ProfileComparisonService comparisonService,
                                    AudiencePackageMapper packageMapper,
                                    AudienceRuleMapper ruleMapper) {
        this.segmentationService = segmentationService;
        this.comparisonService = comparisonService;
        this.packageMapper = packageMapper;
        this.ruleMapper = ruleMapper;
    }

    @PostMapping("/estimate")
    public Result<Map<String, Object>> estimate(@RequestBody EstimateRequest request) {
        long count = segmentationService.estimateCount(request.conditions(), request.logic());
        return Result.success(Map.of("count", count));
    }

    @PostMapping("/search")
    public Result<Page<UserProfileListItemResponse>> search(@RequestBody SearchRequest request) {
        return Result.success(segmentationService.segmentUsers(
                request.conditions(), request.logic(), request.page(), request.size()));
    }

    @GetMapping("/packages")
    public Result<List<AudiencePackage>> listPackages() {
        return Result.success(packageMapper.selectList(null));
    }

    @PostMapping("/packages")
    public Result<AudiencePackage> savePackage(@RequestBody SavePackageRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        long count = segmentationService.estimateCount(request.conditions(), request.logic());
        AudiencePackage pkg = new AudiencePackage();
        pkg.setPackageName(request.packageName());
        pkg.setDescription(request.description());
        pkg.setTotalCount((int) count);
        pkg.setStatus(1);
        pkg.setCreatedBy(user.userId());
        pkg.setCreatedAt(java.time.LocalDateTime.now());
        pkg.setUpdatedAt(java.time.LocalDateTime.now());
        packageMapper.insert(pkg);
        // 同步写入圈选规则
        int order = 0;
        for (ConditionDTO c : request.conditions()) {
            AudienceRule rule = new AudienceRule();
            rule.setPackageId(pkg.getId());
            rule.setRuleGroup("root");
            rule.setFieldName(c.field());
            rule.setOperator(c.operator());
            rule.setValue(c.value() instanceof List<?> l ? l.toString() : String.valueOf(c.value()));
            rule.setLogicOp(request.logic());
            rule.setSortOrder(order++);
            ruleMapper.insert(rule);
        }
        return Result.success(pkg);
    }

    @PostMapping("/compare")
    public Result<ComparisonResultVO> compare(@RequestBody CompareRequest request) {
        return Result.success(comparisonService.compareProfiles(request.groupAId(), request.groupBId()));
    }

    @PutMapping("/packages/{id}")
    public Result<AudiencePackage> updatePackage(@PathVariable Long id,
                                                  @RequestBody UpdatePackageRequest request) {
        AudiencePackage pkg = packageMapper.selectById(id);
        if (pkg == null) throw new BusinessException(ResultCode.NOT_FOUND, "人群包不存在");
        if (request.packageName() != null && !request.packageName().isBlank())
            pkg.setPackageName(request.packageName().trim());
        if (request.description() != null)
            pkg.setDescription(request.description().trim());
        pkg.setUpdatedAt(java.time.LocalDateTime.now());
        packageMapper.updateById(pkg);
        return Result.success(pkg);
    }

    @DeleteMapping("/packages/{id}")
    public Result<Void> deletePackage(@PathVariable Long id) {
        AudiencePackage pkg = packageMapper.selectById(id);
        if (pkg == null) throw new BusinessException(ResultCode.NOT_FOUND, "人群包不存在");
        pkg.setStatus(0);
        pkg.setUpdatedAt(java.time.LocalDateTime.now());
        packageMapper.updateById(pkg);
        return Result.success(null);
    }

    record EstimateRequest(List<ConditionDTO> conditions, String logic) {}
    record SearchRequest(List<ConditionDTO> conditions, String logic, int page, int size) {}
    record SavePackageRequest(String packageName, String description, List<ConditionDTO> conditions, String logic) {}
    record CompareRequest(Long groupAId, Long groupBId) {}
    record UpdatePackageRequest(String packageName, String description) {}
}
