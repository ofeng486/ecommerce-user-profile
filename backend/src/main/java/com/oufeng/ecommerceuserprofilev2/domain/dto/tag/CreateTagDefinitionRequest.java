package com.oufeng.ecommerceuserprofilev2.domain.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建标签定义请求。
 */
public record CreateTagDefinitionRequest(
        @NotBlank(message = "标签编码不能为空")
        @Size(max = 50, message = "标签编码不能超过50个字符")
        String tagCode,

        @NotBlank(message = "标签名称不能为空")
        @Size(max = 100, message = "标签名称不能超过100个字符")
        String tagName,

        @NotBlank(message = "标签分类不能为空")
        @Size(max = 50, message = "标签分类不能超过50个字符")
        String tagCategory,

        @NotBlank(message = "值类型不能为空")
        @Size(max = 20, message = "值类型不能超过20个字符")
        String valueType,

        @Size(max = 2000, message = "计算规则不能超过2000个字符")
        String calculationRule
) {}
