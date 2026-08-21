package com.oufeng.ecommerceuserprofile.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.application.AudienceSegmentationService;
import com.oufeng.ecommerceuserprofile.application.ProfileComparisonService;
import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.domain.dto.audience.PackageWithRulesVO;
import com.oufeng.ecommerceuserprofile.domain.dto.comparison.ComparisonResultVO;
import com.oufeng.ecommerceuserprofile.domain.dto.profile.UserProfileListItemResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.segmentation.ConditionDTO;
import com.oufeng.ecommerceuserprofile.domain.entity.AudiencePackage;
import com.oufeng.ecommerceuserprofile.domain.entity.AudiencePackageUser;
import com.oufeng.ecommerceuserprofile.domain.entity.AudienceRule;
import com.oufeng.ecommerceuserprofile.domain.mapper.AudiencePackageMapper;
import com.oufeng.ecommerceuserprofile.domain.mapper.AudiencePackageUserMapper;
import com.oufeng.ecommerceuserprofile.domain.mapper.AudienceRuleMapper;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    private final AudiencePackageUserMapper packageUserMapper;
    private final com.oufeng.ecommerceuserprofile.infrastructure.mapper.UserProfileQueryMapper queryMapper;

    public AdminAudienceController(AudienceSegmentationService segmentationService,
                                    ProfileComparisonService comparisonService,
                                    AudiencePackageMapper packageMapper,
                                    AudienceRuleMapper ruleMapper,
                                    AudiencePackageUserMapper packageUserMapper,
                                    com.oufeng.ecommerceuserprofile.infrastructure.mapper.UserProfileQueryMapper queryMapper) {
        this.segmentationService = segmentationService;
        this.comparisonService = comparisonService;
        this.packageMapper = packageMapper;
        this.ruleMapper = ruleMapper;
        this.packageUserMapper = packageUserMapper;
        this.queryMapper = queryMapper;
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

    @Operation(summary = "圈选结果导出 CSV（复用圈选条件，UTF-8 BOM 供 Excel 打开）")
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody SearchRequest request) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("用户编码,性别,年龄,省份,城市,订单数,消费金额,用户分层,分层评分\r\n");
        long page = 0;
        while (true) {
            var p = segmentationService.segmentUsers(request.conditions(), request.logic(), (int) page, 1000);
            if (p.getRecords().isEmpty()) break;
            for (UserProfileListItemResponse r : p.getRecords()) {
                sb.append(r.userCode()).append(',')
                  .append("Male".equals(r.gender()) ? "男" : "Female".equals(r.gender()) ? "女" : String.valueOf(r.gender())).append(',')
                  .append(r.age() == null ? "" : r.age()).append(',')
                  .append(r.province() == null ? "" : r.province()).append(',')
                  .append(r.city() == null ? "" : r.city()).append(',')
                  .append(r.totalOrderCount()).append(',')
                  .append(r.totalPaymentAmount()).append(',')
                  .append(r.segmentName() == null ? "" : r.segmentName()).append(',')
                  .append(r.segmentScore() == null ? "" : r.segmentScore())
                  .append("\r\n");
            }
            page++;
            if (page > 500) break; // 50 万行保护
        }
        byte[] data = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audience_export.csv")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }

    @GetMapping("/packages")
    public Result<List<PackageWithRulesVO>> listPackages() {
        List<AudiencePackage> packages = packageMapper.selectList(null);
        if (packages.isEmpty()) return Result.success(java.util.List.of());
        // 批量拉取全部规则，按 packageId 分组，避免 N+1
        java.util.Map<Long, List<AudienceRule>> rulesByPkg = ruleMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.groupingBy(AudienceRule::getPackageId));
        return Result.success(packages.stream().map(p -> {
            List<AudienceRule> rules = rulesByPkg.getOrDefault(p.getId(), java.util.List.of());
            List<PackageWithRulesVO.RuleVO> ruleVOs = rules.stream()
                    .sorted(java.util.Comparator.comparingInt(r -> r.getSortOrder() == null ? 0 : r.getSortOrder()))
                    .map(r -> new PackageWithRulesVO.RuleVO(
                            r.getFieldName(), r.getOperator(), r.getValue(), r.getLogicOp()))
                    .toList();
            return new PackageWithRulesVO(p.getId(), p.getPackageName(), p.getDescription(),
                    p.getTotalCount(), p.getStatus(), p.getCreatedAt(), p.getUpdatedAt(), ruleVOs);
        }).toList());
    }

    @Transactional
    @PostMapping("/packages")
    public Result<AudiencePackage> savePackage(@RequestBody SavePackageRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        AudiencePackage pkg = new AudiencePackage();
        pkg.setPackageName(request.packageName());
        pkg.setDescription(request.description());
        pkg.setStatus(1);
        pkg.setCreatedBy(user.userId());
        pkg.setCreatedAt(java.time.LocalDateTime.now());
        pkg.setUpdatedAt(java.time.LocalDateTime.now());
        // 指定用户人群包：直接从画像列表批量选人创建（无圈选规则）
        if (request.userIds() != null && !request.userIds().isEmpty()) {
            List<Long> ids = request.userIds().stream().distinct().toList();
            pkg.setTotalCount(ids.size());
            packageMapper.insert(pkg);
            for (Long uid : ids) {
                AudiencePackageUser pu = new AudiencePackageUser();
                pu.setPackageId(pkg.getId());
                pu.setUserId(uid);
                packageUserMapper.insert(pu);
            }
            return Result.success(pkg);
        }
        // 规则圈选人群包：基于圈选条件创建
        if (request.conditions() == null || request.conditions().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "圈选条件不能为空（指定用户包请传 userIds）");
        }
        long count = segmentationService.estimateCount(request.conditions(), request.logic());
        pkg.setTotalCount((int) count);
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
            rule.setLogicOp(c.logicOp() != null ? c.logicOp() : request.logic());
            rule.setSortOrder(order++);
            ruleMapper.insert(rule);
        }
        return Result.success(pkg);
    }

    @Operation(summary = "查询人群包指定用户列表（规则圈选包按规则重算；指定用户包读关联表）")
    @GetMapping("/packages/{id}/users")
    public Result<List<Long>> listPackageUsers(@PathVariable Long id) {
        if (packageMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "人群包不存在");
        }
        return Result.success(resolvePackageUserIds(id));
    }

    /** 解析人群包用户 ID：规则圈选包按规则重算；指定用户包读关联表 */
    private List<Long> resolvePackageUserIds(Long id) {
        // 优先按规则重算（规则圈选包：audience_rule 表有规则，audience_package_users 表为空）
        List<AudienceRule> rules = ruleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AudienceRule>()
                        .eq(AudienceRule::getPackageId, id).orderByAsc(AudienceRule::getSortOrder));
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
                List<Long> batch = segmentationService.segmentUsers(conds, logic, page, 1000)
                        .getRecords().stream().map(UserProfileListItemResponse::userId).toList();
                ids.addAll(batch);
                if (batch.size() < 1000 || ids.size() >= 50000) break;
                page++;
            }
            return ids;
        }
        // 指定用户包：读关联表
        return packageUserMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AudiencePackageUser>()
                        .eq(AudiencePackageUser::getPackageId, id))
                .stream().map(AudiencePackageUser::getUserId).toList();
    }

    @Operation(summary = "导出人群包内用户 CSV（UTF-8 BOM 供 Excel 打开）")
    @GetMapping("/packages/{id}/export")
    public ResponseEntity<byte[]> exportPackageUsers(@PathVariable Long id) {
        AudiencePackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "人群包不存在");
        }
        List<Long> ids = resolvePackageUserIds(id);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("用户编码,性别,年龄,省份,城市,订单数,消费金额,用户分层,分层评分\r\n");
        // 按 2000 一批查用户信息（IN 查询过大有 SQL 限制）
        for (int i = 0; i < ids.size(); i += 2000) {
            List<Long> batchIds = ids.subList(i, Math.min(i + 2000, ids.size()));
            for (UserProfileListItemResponse r : queryMapper.queryUsersByIds(batchIds)) {
                sb.append(r.userCode()).append(',')
                  .append("Male".equals(r.gender()) ? "男" : "Female".equals(r.gender()) ? "女" : String.valueOf(r.gender())).append(',')
                  .append(r.age() == null ? "" : r.age()).append(',')
                  .append(r.province() == null ? "" : r.province()).append(',')
                  .append(r.city() == null ? "" : r.city()).append(',')
                  .append(r.totalOrderCount()).append(',')
                  .append(r.totalPaymentAmount()).append(',')
                  .append(r.segmentName() == null ? "" : r.segmentName()).append(',')
                  .append(r.segmentScore() == null ? "" : r.segmentScore())
                  .append("\r\n");
            }
        }
        byte[] data = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String fname = "人群包_" + pkg.getPackageName() + "_" + java.time.LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + java.net.URLEncoder.encode(fname, java.nio.charset.StandardCharsets.UTF_8))
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
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
    record SavePackageRequest(String packageName, String description, List<ConditionDTO> conditions,
                              String logic, List<Long> userIds) {}
    record CompareRequest(Long groupAId, Long groupBId) {}
    record UpdatePackageRequest(String packageName, String description) {}
}
