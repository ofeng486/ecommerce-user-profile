package com.oufeng.ecommerceuserprofile.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oufeng.ecommerceuserprofile.domain.entity.SystemLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统登录审计日志 Mapper。
 */
@Mapper
public interface SystemLoginLogMapper extends BaseMapper<SystemLoginLog> {
}
