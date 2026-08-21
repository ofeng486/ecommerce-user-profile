package com.oufeng.ecommerceuserprofile.domain.dto.profile;

/** 标签交叉统计（活跃档 × 消费档的用户矩阵单元）。 */
public record TagCrossResponse(String activeLevel, String consumeLevel, long userCount) {}
