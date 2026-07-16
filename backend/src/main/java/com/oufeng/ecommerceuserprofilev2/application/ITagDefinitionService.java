package com.oufeng.ecommerceuserprofilev2.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofilev2.domain.dto.tag.CreateTagDefinitionRequest;
import com.oufeng.ecommerceuserprofilev2.domain.dto.tag.TagDefinitionResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.tag.UpdateTagDefinitionRequest;

import java.util.List;

/**
 * 用户画像标签定义管理接口。
 */
public interface ITagDefinitionService {

    /** 分页查询所有标签定义。 */
    Page<TagDefinitionResponse> listTags(int page, int size);

    /** 查询所有启用的标签定义。 */
    List<TagDefinitionResponse> listEnabledTags();

    /** 创建标签定义。 */
    TagDefinitionResponse createTag(CreateTagDefinitionRequest request, Long createdBy);

    /** 更新标签定义。 */
    TagDefinitionResponse updateTag(Long tagId, UpdateTagDefinitionRequest request);

    /** 启用/停用标签。 */
    TagDefinitionResponse updateStatus(Long tagId, boolean enabled);
}
