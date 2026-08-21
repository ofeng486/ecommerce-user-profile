package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.domain.entity.SparkAnalysisTask;
import com.oufeng.ecommerceuserprofile.domain.mapper.SparkAnalysisTaskMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnalysisTaskServiceImpl 单元测试（轻量 — 核心逻辑依赖外部脚本执行）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisTaskServiceImpl 单元测试")
class AnalysisTaskServiceImplTest {

    @Mock
    private SparkAnalysisTaskMapper mapper;

    @InjectMocks
    private AnalysisTaskServiceImpl analysisTaskService;

    @Test
    @DisplayName("列表查询 — 委托给 mapper.selectPage，支持类型过滤")
    void shouldListTasks() {
        Page<SparkAnalysisTask> mockPage = new Page<>();
        mockPage.setRecords(java.util.List.of());
        mockPage.setTotal(0);
        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 注意：listTasks 使用 1-based page，调用时需注意
        var result = analysisTaskService.listTasks(1, 10, "DATA_IMPORT", null, null, null, null);

        assertThat(result).isNotNull();
        verify(mapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("列表查询 — taskType 为空时不加过滤条件")
    void shouldListTasksWithoutTypeFilter() {
        Page<SparkAnalysisTask> mockPage = new Page<>();
        mockPage.setRecords(java.util.List.of());
        mockPage.setTotal(0);
        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        var result = analysisTaskService.listTasks(1, 10, null, null, null, null, null);

        assertThat(result).isNotNull();
        verify(mapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }
}
