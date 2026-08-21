package com.oufeng.ecommerceuserprofile.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 统一分页查询请求参数。
 * 所有分页接口复用此 DTO，避免每个 Controller 方法重复声明 page/size 默认值。
 */
public record PageQueryRequest(
        @Min(0)
        int page,

        @Min(1)
        @Max(100)
        int size
) {
    /** 默认第 0 页，每页 20 条 */
    public PageQueryRequest() {
        this(0, 20);
    }

    /** 返回 MyBatis-Plus 可用的偏移量 */
    public long offset() {
        return (long) page * size;
    }

    /** 创建实例，如果传入非法值则降级为默认值 */
    public static PageQueryRequest of(Integer page, Integer size) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 100);
        return new PageQueryRequest(p, s);
    }
}
