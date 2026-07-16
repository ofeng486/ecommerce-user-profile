package com.oufeng.ecommerceuserprofilev2.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oufeng.ecommerceuserprofilev2.domain.entity.ProfileTagDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 画像标签定义 Mapper。
 */
@Mapper
public interface ProfileTagDefinitionMapper extends BaseMapper<ProfileTagDefinition> {

    /** 按标签编码查找。 */
    @Select("SELECT * FROM profile_tag_definition WHERE tag_code = #{tagCode}")
    Optional<ProfileTagDefinition> findByTagCode(@Param("tagCode") String tagCode);

    /** 判断指定 tagCode 是否已存在。 */
    @Select("SELECT COUNT(*) > 0 FROM profile_tag_definition WHERE tag_code = #{tagCode}")
    boolean existsByTagCode(@Param("tagCode") String tagCode);
}
