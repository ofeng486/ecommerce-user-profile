package com.oufeng.ecommerceuserprofile.domain.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 画像列表页核心指标（用于顶部指标卡）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileMetricsResponse {

    /** 已画像用户总数 */
    private long totalUsers;

    /** 画像用户累计订单数 */
    private long totalOrders;

    /** 画像用户累计消费金额 */
    private double totalAmount;

    /** 流失风险用户数（AT_RISK 分层） */
    private long atRiskUsers;
}
