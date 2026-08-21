package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.domain.converter.TagDefinitionConverter;
import com.oufeng.ecommerceuserprofile.domain.dto.tag.CreateTagDefinitionRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.tag.TagDefinitionResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.tag.UpdateTagDefinitionRequest;
import com.oufeng.ecommerceuserprofile.domain.entity.ProfileTagDefinition;
import com.oufeng.ecommerceuserprofile.domain.mapper.ProfileTagDefinitionMapper;
import com.oufeng.ecommerceuserprofile.common.BusinessException;
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

    /** 系统预设标签前缀：这些标签与 Spark 画像作业联动，删除会破坏画像一致性 */
    private static final List<String> PRESET_PREFIXES = List.of(
            "ACTIVE_LEVEL", "CONSUMPTION_LEVEL", "FAVORITE_CATEGORY", "RFM_SEGMENT");

    private final ProfileTagDefinitionMapper mapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public TagDefinitionServiceImpl(ProfileTagDefinitionMapper mapper,
                                    org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.jdbc = jdbc;
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
    /** 自动生成标签编码：TAG_ + 8位大写 UUID（无需用户手填） */
    private String autoGenerateCode() {
        String suffix = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "TAG_" + suffix;
    }

    public List<TagDefinitionResponse> listEnabledTags() {
        return mapper.selectList(new LambdaQueryWrapper<ProfileTagDefinition>()
                .eq(ProfileTagDefinition::getStatus, (byte) 1))
                .stream().map(TagDefinitionConverter.INSTANCE::toResponse).toList();
    }

    public TagDefinitionResponse createTag(CreateTagDefinitionRequest request, Long createdBy) {
        String code = (request.tagCode() == null || request.tagCode().isBlank())
                ? autoGenerateCode() : request.tagCode().trim().toUpperCase();
        if (mapper.existsByTagCode(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标签编码 " + code + " 已存在");
        }
        ProfileTagDefinition tag = new ProfileTagDefinition(
                code, request.tagName(), request.tagCategory(),
                request.valueType(), request.calculationRule(), createdBy);
        tag.setSourceTable(request.sourceTable());
        tag.setRuleExpression(request.ruleExpression());
        mapper.insert(tag);
        return TagDefinitionConverter.INSTANCE.toResponse(tag);
    }

    public TagDefinitionResponse updateTag(Long tagId, UpdateTagDefinitionRequest request) {
        ProfileTagDefinition tag = mapper.selectById(tagId);
        if (tag == null) throw new BusinessException(ResultCode.NOT_FOUND, "标签定义不存在");
        tag.update(request.tagName(), request.tagCategory(), request.valueType(), request.calculationRule());
        tag.setSourceTable(request.sourceTable());
        tag.setRuleExpression(request.ruleExpression());
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

    public void deleteTag(Long tagId) {
        ProfileTagDefinition tag = mapper.selectById(tagId);
        if (tag == null) throw new BusinessException(ResultCode.NOT_FOUND, "标签定义不存在");
        // 系统预设标签与 Spark 作业联动，禁止删除（可停用）
        String code = tag.getTagCode() == null ? "" : tag.getTagCode().toUpperCase();
        for (String prefix : PRESET_PREFIXES) {
            if (code.startsWith(prefix)) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "系统预设标签「" + tag.getTagName() + "」不允许删除，可停用（停用后重算不再生成）");
            }
        }
        // 连带清理该标签已生成的画像结果，避免孤儿数据
        jdbc.update("DELETE FROM user_profile_tag WHERE tag_id = ?", tagId);
        mapper.deleteById(tagId);
    }
}
