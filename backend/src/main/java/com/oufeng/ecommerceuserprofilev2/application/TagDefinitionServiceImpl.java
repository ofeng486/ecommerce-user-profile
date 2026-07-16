package com.oufeng.ecommerceuserprofilev2.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofilev2.common.ResultCode;
import com.oufeng.ecommerceuserprofilev2.domain.converter.TagDefinitionConverter;
import com.oufeng.ecommerceuserprofilev2.domain.dto.tag.CreateTagDefinitionRequest;
import com.oufeng.ecommerceuserprofilev2.domain.dto.tag.TagDefinitionResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.tag.UpdateTagDefinitionRequest;
import com.oufeng.ecommerceuserprofilev2.domain.entity.ProfileTagDefinition;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.ProfileTagDefinitionMapper;
import com.oufeng.ecommerceuserprofilev2.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户画像标签定义管理服务。
 * 仅管理员可调用，提供标签定义的 CRUD 和状态管理。
 */
@Service
@Transactional
public class TagDefinitionServiceImpl implements ITagDefinitionService {

    private final ProfileTagDefinitionMapper mapper;

    public TagDefinitionServiceImpl(ProfileTagDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<TagDefinitionResponse> listTags(int page, int size) {
        Page<ProfileTagDefinition> entityPage = mapper.selectPage(
                new Page<>(page, Math.min(size, 100)),
                new LambdaQueryWrapper<ProfileTagDefinition>().orderByAsc(ProfileTagDefinition::getId));
        List<TagDefinitionResponse> records = entityPage.getRecords().stream()
                .map(TagDefinitionConverter.INSTANCE::toResponse).toList();
        Page<TagDefinitionResponse> result = new Page<>(page, size, entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Transactional(readOnly = true)
    public List<TagDefinitionResponse> listEnabledTags() {
        return mapper.selectList(new LambdaQueryWrapper<ProfileTagDefinition>()
                .eq(ProfileTagDefinition::getStatus, (byte) 1))
                .stream().map(TagDefinitionConverter.INSTANCE::toResponse).toList();
    }

    public TagDefinitionResponse createTag(CreateTagDefinitionRequest request, Long createdBy) {
        if (mapper.existsByTagCode(request.tagCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标签编码 " + request.tagCode() + " 已存在");
        }
        ProfileTagDefinition tag = new ProfileTagDefinition(
                request.tagCode(), request.tagName(), request.tagCategory(),
                request.valueType(), request.calculationRule(), createdBy);
        mapper.insert(tag);
        return TagDefinitionConverter.INSTANCE.toResponse(tag);
    }

    public TagDefinitionResponse updateTag(Long tagId, UpdateTagDefinitionRequest request) {
        ProfileTagDefinition tag = mapper.selectById(tagId);
        if (tag == null) throw new BusinessException(ResultCode.NOT_FOUND, "标签定义不存在");
        tag.update(request.tagName(), request.tagCategory(), request.valueType(), request.calculationRule());
        mapper.updateById(tag);
        return TagDefinitionConverter.INSTANCE.toResponse(tag);
    }

    public TagDefinitionResponse updateStatus(Long tagId, boolean enabled) {
        ProfileTagDefinition tag = mapper.selectById(tagId);
        if (tag == null) throw new BusinessException(ResultCode.NOT_FOUND, "标签定义不存在");
        tag.updateStatus(enabled);
        mapper.updateById(tag);
        return TagDefinitionConverter.INSTANCE.toResponse(tag);
    }
}
