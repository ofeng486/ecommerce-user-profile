package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.domain.dto.tag.CreateTagDefinitionRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.tag.TagDefinitionResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.tag.UpdateTagDefinitionRequest;

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

    /** 删除标签定义（系统预设标签受保护不可删；删除同时清理该标签的画像结果数据）。 */
    void deleteTag(Long tagId);
}
