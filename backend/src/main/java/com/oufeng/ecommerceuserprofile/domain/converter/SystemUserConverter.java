package com.oufeng.ecommerceuserprofile.domain.converter;

import com.oufeng.ecommerceuserprofile.domain.dto.user.SystemUserResponse;
import com.oufeng.ecommerceuserprofile.domain.entity.SystemUser;
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
    @Mapping(target = "role", source = "role")
    @Mapping(target = "enabled", expression = "java(entity.isEnabled())")
    @Mapping(target = "lastLoginAt", source = "lastLoginAt")
    SystemUserResponse toResponse(SystemUser entity);
}
