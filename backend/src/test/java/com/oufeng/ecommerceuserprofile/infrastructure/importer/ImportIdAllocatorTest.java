package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ImportIdAllocator 单元测试：id 冲突避让与自动递增分配逻辑。
 */
@DisplayName("ImportIdAllocator id 分配测试")
class ImportIdAllocatorTest {

    private ImportIdAllocator allocator(Set<Long> existing) {
        return new ImportIdAllocator(new HashSet<>(existing));
    }

    @Test
    @DisplayName("库为空时：空 id 从 1 开始递增")
    void allocateFromOneWhenEmpty() {
        ImportIdAllocator a = allocator(Set.of());
        assertThat(a.allocate(null)).isEqualTo(1);
        assertThat(a.allocate("")).isEqualTo(2);
        assertThat(a.allocate("  ")).isEqualTo(3);
    }

    @Test
    @DisplayName("合法且未占用的 id 原样保留")
    void keepFreeId() {
        ImportIdAllocator a = allocator(Set.of(1L, 2L));
        assertThat(a.allocate("100")).isEqualTo(100);
        // 批次内不重复
        assertThat(a.allocate("100")).isEqualTo(101);
    }

    @Test
    @DisplayName("冲突 id 自动递增避让（不覆盖已有数据）")
    void rebaseConflictingId() {
        ImportIdAllocator a = allocator(Set.of(1L, 2L, 3L, 100L));
        assertThat(a.allocate("1")).isEqualTo(101);
        assertThat(a.allocate("2")).isEqualTo(102);
        assertThat(a.allocate("3")).isEqualTo(103);
    }

    @Test
    @DisplayName("id 为 0/负数/非数字时自动分配")
    void allocateForInvalidId() {
        ImportIdAllocator a = allocator(Set.of(5L));
        assertThat(a.allocate("0")).isEqualTo(6);
        assertThat(a.allocate("-5")).isEqualTo(7);
        assertThat(a.allocate("abc")).isEqualTo(8);
    }

    @Test
    @DisplayName("maxId 从已有数据最大值继续")
    void continueFromMax() {
        ImportIdAllocator a = allocator(Set.of(10L, 20L, 15L));
        assertThat(a.allocate(null)).isEqualTo(21);
        assertThat(a.getMaxId()).isEqualTo(21);
    }
}
