package com.oufeng.ecommerceuserprofilev2.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SystemUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 系统用户 Mapper。
 */
@Mapper
public interface SystemUserMapper extends BaseMapper<SystemUser> {

    /** 按登录用户名查询系统用户。 */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    Optional<SystemUser> findByUsername(@Param("username") String username);

    /** 判断登录用户名是否已存在。 */
    @Select("SELECT COUNT(*) > 0 FROM sys_user WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);
}
