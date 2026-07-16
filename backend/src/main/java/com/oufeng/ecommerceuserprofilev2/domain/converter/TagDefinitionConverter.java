package com.oufeng.ecommerceuserprofilev2.domain.converter;

import com.oufeng.ecommerceuserprofilev2.domain.dto.tag.TagDefinitionResponse;
import com.oufeng.ecommerceuserprofilev2.domain.entity.ProfileTagDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * ProfileTagDefinition Entity ↔ DTO 转换器。
 */
@Mapper
public interface TagDefinitionConverter {

    TagDefinitionConverter INSTANCE = Mappers.getMapper(TagDefinitionConverter.class);

    /** Entity → Response DTO */
    TagDefinitionResponse toResponse(ProfileTagDefinition entity);
}
