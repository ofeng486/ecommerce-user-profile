package com.oufeng.ecommerceuserprofile.domain.converter;

import com.oufeng.ecommerceuserprofile.domain.dto.tag.TagDefinitionResponse;
import com.oufeng.ecommerceuserprofile.domain.entity.ProfileTagDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * ProfileTagDefinition Entity ↔ DTO 转换器。
 */
@Mapper
public interface TagDefinitionConverter {

    TagDefinitionConverter INSTANCE = Mappers.getMapper(TagDefinitionConverter.class);

    /** Entity → Response DTO */
    @Mapping(target = "sourceTable", source = "sourceTable")
    @Mapping(target = "ruleExpression", source = "ruleExpression")
    TagDefinitionResponse toResponse(ProfileTagDefinition entity);
}
