package com.oufeng.ecommerceuserprofile.application;

import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.domain.dto.tag.CreateTagDefinitionRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.tag.UpdateTagDefinitionRequest;
import com.oufeng.ecommerceuserprofile.domain.entity.ProfileTagDefinition;
import com.oufeng.ecommerceuserprofile.domain.mapper.ProfileTagDefinitionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TagDefinitionServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagDefinitionServiceImpl 单元测试")
class TagDefinitionServiceImplTest {

    @Mock
    private ProfileTagDefinitionMapper mapper;

    @InjectMocks
    private TagDefinitionServiceImpl tagService;

    @Nested
    @DisplayName("创建标签 (createTag)")
    class CreateTag {

        @Test
        @DisplayName("正常创建标签成功")
        void shouldCreateSuccessfully() {
            var request = new CreateTagDefinitionRequest("TEST_001", "测试标签", "BEHAVIOR", "String", null, null, null);
            when(mapper.existsByTagCode("TEST_001")).thenReturn(false);
            when(mapper.insert(any(ProfileTagDefinition.class))).thenReturn(1);

            var response = tagService.createTag(request, 1L);

            assertThat(response.tagCode()).isEqualTo("TEST_001");
            assertThat(response.tagName()).isEqualTo("测试标签");
        }

        @Test
        @DisplayName("标签编码重复则抛出异常")
        void shouldThrowWhenCodeExists() {
            var request = new CreateTagDefinitionRequest("DUP_CODE", "重复标签", "BEHAVIOR", "String", null, null, null);
            when(mapper.existsByTagCode("DUP_CODE")).thenReturn(true);

            assertThatThrownBy(() -> tagService.createTag(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("标签编码");
        }
    }

    @Nested
    @DisplayName("更新标签 (updateTag)")
    class UpdateTag {

        @Test
        @DisplayName("正常更新标签成功")
        void shouldUpdateSuccessfully() {
            ProfileTagDefinition existing = new ProfileTagDefinition("OLD", "旧名称", "BEHAVIOR", "String", null, 1L);
            existing.setId(1L);
            when(mapper.selectById(1L)).thenReturn(existing);

            var request = new UpdateTagDefinitionRequest("新名称", "CONSUMPTION", "Number", null, null, null);
            var response = tagService.updateTag(1L, request);

            assertThat(response.tagName()).isEqualTo("新名称");
            assertThat(response.tagCategory()).isEqualTo("CONSUMPTION");
        }

        @Test
        @DisplayName("更新不存在的标签则抛出异常")
        void shouldThrowWhenNotFound() {
            when(mapper.selectById(99L)).thenReturn(null);
            var request = new UpdateTagDefinitionRequest("随便", "BEHAVIOR", "String", null, null, null);

            assertThatThrownBy(() -> tagService.updateTag(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不存在");
        }
    }

    @Nested
    @DisplayName("状态管理 (updateStatus)")
    class UpdateStatus {

        @Test
        @DisplayName("启用/禁用标签成功")
        void shouldToggleStatus() {
            ProfileTagDefinition tag = new ProfileTagDefinition("T1", "标签1", "BEHAVIOR", "String", null, 1L);
            tag.setId(1L);
            when(mapper.selectById(1L)).thenReturn(tag);

            tagService.updateStatus(1L, false);
            verify(mapper).updateById(tag);
        }
    }
}
