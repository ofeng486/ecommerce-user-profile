package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import java.util.HashMap;
import java.util.Map;

/**
 * 同批导入的跨表主键映射器。
 *
 * 当主表（用户/商品/分类等）的 id 因冲突被重分配后，从表（订单/明细/行为等）
 * 中的外键引用必须同步映射到新 id，否则会导致外键错配、数据污染。
 * 一次导入批次（最多 7 个文件）共享同一个实例。
 */
public class ImportIdMapper {

    /** 表名 → (旧 id → 新 id) */
    private final Map<String, Map<String, Long>> mapping = new HashMap<>();

    /** 记录一次主键重分配：表 table 中旧 id oldId 被重分配为 newId */
    public void record(String table, String oldId, Long newId) {
        mapping.computeIfAbsent(table, k -> new HashMap<>()).put(oldId, newId);
    }

    /** 查询映射：表 table 中 oldId 是否被重分配，返回新 id；未重分配返回 null */
    public Long lookup(String table, String oldId) {
        Map<String, Long> m = mapping.get(table);
        return m == null ? null : m.get(oldId);
    }

    /** 是否有任何重分配记录 */
    public boolean isEmpty() {
        return mapping.isEmpty();
    }
}
