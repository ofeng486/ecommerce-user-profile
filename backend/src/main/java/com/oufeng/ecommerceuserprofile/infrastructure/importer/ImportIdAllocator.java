package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import java.util.Set;
import java.util.TreeSet;

/**
 * 导入时的主键分配器。
 *
 * 规则：CSV 中的 id 合法（正整数）且未被占用 → 原样使用；
 * id 为空 / 非数字 / ≤0 / 与库中已有 id 冲突 → 从库中最大 id 起自动递增分配，
 * 保证新行不会覆盖已有数据（配合 INSERT ... ON DUPLICATE KEY UPDATE 使用）。
 */
public class ImportIdAllocator {

    private final Set<Long> existingIds;
    private long maxId;

    public ImportIdAllocator(Set<Long> existingIds) {
        this.existingIds = new TreeSet<>(existingIds);
        this.maxId = this.existingIds.stream().mapToLong(Long::longValue).max().orElse(0L);
    }

    /** 当前库中最大 id（用于向用户报告起点） */
    public long getMaxId() { return maxId; }

    /**
     * 分配 id：rawId 合法且未占用则原样返回；否则分配 maxId+1 递增。
     * 调用后 maxId/existingIds 同步更新，保证本次批次内不重复。
     */
    public synchronized long allocate(String rawId) {
        if (rawId != null) {
            String trimmed = rawId.trim();
            if (!trimmed.isEmpty()) {
                try {
                    long id = Long.parseLong(trimmed);
                    if (id > 0 && !existingIds.contains(id)) {
                        existingIds.add(id);
                        maxId = Math.max(maxId, id);
                        return id;
                    }
                } catch (NumberFormatException ignored) {
                    // 非数字 id 走自动分配
                }
            }
        }
        // 自动递增分配，跳过已占用 id
        while (existingIds.contains(maxId + 1)) {
            maxId++;
        }
        maxId++;
        existingIds.add(maxId);
        return maxId;
    }
}
