package com.oufeng.ecommerceuserprofilev2.domain.converter;

import com.oufeng.ecommerceuserprofilev2.domain.dto.user.SystemUserResponse;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SystemUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * SystemUser Entity ↔ DTO 转换器。
 */
@Mapper
public interface SystemUserConverter {

    SystemUserConverter INSTANCE = Mappers.getMapper(SystemUserConverter.class);

    /** Entity → Response DTO */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "role", expression = "java(entity.getRoleEnum().name())")
    @Mapping(target = "enabled", expression = "java(entity.isEnabled())")
    @Mapping(target = "lastLoginAt", source = "lastLoginAt")
    SystemUserResponse toResponse(SystemUser entity);
}
