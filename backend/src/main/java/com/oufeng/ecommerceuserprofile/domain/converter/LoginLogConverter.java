package com.oufeng.ecommerceuserprofile.domain.converter;

import com.oufeng.ecommerceuserprofile.domain.dto.user.LoginLogResponse;
import com.oufeng.ecommerceuserprofile.domain.entity.SystemLoginLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * SystemLoginLog Entity ↔ DTO 转换器。
 */
@Mapper
public interface LoginLogConverter {

    LoginLogConverter INSTANCE = Mappers.getMapper(LoginLogConverter.class);

    /** Entity → Response DTO */
    LoginLogResponse toResponse(SystemLoginLog entity);
}
