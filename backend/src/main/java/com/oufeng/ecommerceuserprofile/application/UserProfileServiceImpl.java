package com.oufeng.ecommerceuserprofile.application;

import com.oufeng.ecommerceuserprofile.domain.dto.profile.*;
import com.oufeng.ecommerceuserprofile.domain.dto.profile.TagDistributionResponse;
import com.oufeng.ecommerceuserprofile.infrastructure.mapper.UserProfileQueryMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户画像查询服务。
 * 画像结果由 Spark 写入 MySQL，本服务只读查询。
 * 对概览/分布统计等高频查询使用 @Cacheable 缓存，减少数据库压力。
 */
@Service
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements IUserProfileService {

    private final UserProfileQueryMapper queryMapper;

    public UserProfileServiceImpl(UserProfileQueryMapper queryMapper) {
        this.queryMapper = queryMapper;
    }

    /** 运营总览统计：查询轻量（毫秒级），不缓存保证数据实时 */
    public ProfileOverviewResponse getOverview() {
        return queryMapper.queryOverview();
    }

    /** 分层分布统计：实时查询，避免改库后页面显示旧数据 */
    public List<SegmentDistributionResponse> getSegmentDistribution() {
        return queryMapper.querySegmentDistribution();
    }

    /** 标签分布统计（含品类名称 join）：实时查询，避免改库后页面显示旧数据 */
    public List<TagDistributionResponse> getTagDistribution(String tagCode) {
        return queryMapper.queryTagDistribution(tagCode);
    }

    /** 标签交叉矩阵：活跃档 × 消费档（同一用户两次 JOIN user_profile_tag） */
    public List<TagCrossResponse> getTagCross() {
        return queryMapper.queryTagCross();
    }

    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserProfileListItemResponse>
            listProfiles(String keyword, String segmentCode, String province, Double minAmount, Double maxAmount,
                         String tagCode, String tagValue, String orderBy, String orderDir, int page, int size) {
        long offset = (long) page * Math.min(size, 100);
        long limit = Math.min(size, 100);
        String col = sortColumn(orderBy);
        String dir = "asc".equalsIgnoreCase(orderDir) ? "ASC" : "DESC";
        List<UserProfileListItemResponse> records =
                queryMapper.queryProfiles(keyword, segmentCode, province, minAmount, maxAmount, tagCode, tagValue, col, dir, offset, limit);
        long total = queryMapper.countProfiles(keyword, segmentCode, province, minAmount, maxAmount, tagCode, tagValue);
        var result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserProfileListItemResponse>(page, limit, total);
        result.setRecords(records);
        return result;
    }

    /** 排序白名单：orderBy 仅允许映射到固定列，防 SQL 注入（列表与导出共用） */
    private String sortColumn(String orderBy) {
        return switch (orderBy == null ? "" : orderBy) {
            case "totalPaymentAmount" -> "p.total_payment_amount";
            case "totalOrderCount" -> "p.total_order_count";
            case "segmentScore" -> "s.segment_score";
            case "age" -> "u.age";
            default -> "p.total_payment_amount"; // 默认按消费金额
        };
    }

    /** 画像列表导出 CSV（UTF-8 BOM，分页拉全量，供 Excel 直接打开） */
    public byte[] exportUsersCsv(String keyword, String segmentCode, String province,
                                 Double minAmount, Double maxAmount, String tagCode, String tagValue,
                                 String orderBy, String orderDir) {
        String col = sortColumn(orderBy);
        String dir = "asc".equalsIgnoreCase(orderDir) ? "ASC" : "DESC";
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("用户编码,性别,年龄,省份,城市,订单数,消费金额,用户分层,分层评分\r\n");
        long page = 0;
        while (true) {
            List<UserProfileListItemResponse> records =
                    queryMapper.queryProfiles(keyword, segmentCode, province, minAmount, maxAmount, tagCode, tagValue, col, dir, page * 1000, 1000);
            if (records.isEmpty()) break;
            for (var r : records) {
                sb.append(csv(r.userCode())).append(',')
                  .append(csv(r.gender())).append(',')
                  .append(r.age() == null ? "" : r.age()).append(',')
                  .append(csv(r.province())).append(',')
                  .append(csv(r.city())).append(',')
                  .append(r.totalOrderCount()).append(',')
                  .append(r.totalPaymentAmount() == null ? "" : r.totalPaymentAmount().toPlainString()).append(',')
                  .append(csv(r.segmentName())).append(',')
                  .append(r.segmentScore() == null ? "" : r.segmentScore().toPlainString()).append("\r\n");
            }
            page++;
            if (records.size() < 1000) break;
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** CSV 字段转义：含逗号/引号时用双引号包裹 */
    private static String csv(String v) {
        if (v == null) return "";
        return v.contains(",") || v.contains("\"") ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }

    /** 画像核心指标（总数/订单/消费/流失风险） */
    public ProfileMetricsResponse getProfileMetrics() {
        return queryMapper.queryProfileMetrics();
    }

    /** TOP 省份消费排名 */
    public java.util.List<java.util.Map<String, Object>> getProvinceAmountRanking() {
        return queryMapper.queryProvinceAmountRanking();
    }

    public UserProfileResponse getUserProfile(Long userId) {
        UserProfileResponse base = queryMapper.queryUserProfile(userId)
                .orElseThrow(() -> new com.oufeng.ecommerceuserprofile.common.BusinessException(
                        com.oufeng.ecommerceuserprofile.common.ResultCode.NOT_FOUND, "电商用户不存在"));
        // 补充用户行为标签列表（活跃度/消费能力/偏好品类/RFM 分层）
        List<TagItem> tags = queryMapper.queryUserTags(userId).stream()
                .map(m -> new TagItem(
                        String.valueOf(m.get("tagCode")),
                        String.valueOf(m.get("tagName")),
                        String.valueOf(m.get("tagValue"))))
                .toList();
        return new UserProfileResponse(base.getUserId(), base.getUserCode(), base.getGender(), base.getAge(),
                base.getProvince(), base.getCity(), base.getTotalOrderCount(),
                base.getTotalPaymentAmount(), base.getAverageOrderAmount(),
                base.getBrowseCount30d(), base.getLoginCount30d(), base.getLastActiveAt(),
                base.getSegmentCode(), base.getSegmentName(), base.getSegmentScore(), tags);
    }
}
