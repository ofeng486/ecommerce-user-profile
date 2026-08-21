package com.oufeng.ecommerceuserprofile.domain.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个用户的画像汇总、分层信息与行为标签列表。
 * 普通 POJO（含无参构造），供 MyBatis resultType 自动映射；tags 由 Service 二次补充。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String userCode;
    private String gender;
    private Integer age;
    private String province;
    private String city;
    private long totalOrderCount;
    private BigDecimal totalPaymentAmount;
    private BigDecimal averageOrderAmount;
    private long browseCount30d;
    private long loginCount30d;
    private LocalDateTime lastActiveAt;
    private String segmentCode;
    private String segmentName;
    private BigDecimal segmentScore;
    private List<TagItem> tags;
}
