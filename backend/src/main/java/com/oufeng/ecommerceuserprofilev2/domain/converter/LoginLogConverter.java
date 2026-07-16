package com.oufeng.ecommerceuserprofilev2.domain.converter;

import com.oufeng.ecommerceuserprofilev2.domain.dto.user.LoginLogResponse;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SystemLoginLog;
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
