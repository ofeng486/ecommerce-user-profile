package com.oufeng.ecommerceuserprofilev2.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * RESTful API 统一响应结果。
 *
 * @param code    业务状态码
 * @param message 响应提示信息
 * @param data    响应数据；没有数据时不输出该字段
 * @param <T>     响应数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(int code, String message, T data) {

    /** 创建包含业务数据的成功响应。 */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 创建不包含业务数据的成功响应。 */
    public static Result<Void> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 根据统一状态码创建失败响应。 */
    public static <T> Result<T> failure(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 根据统一状态码和自定义提示创建失败响应。 */
    public static <T> Result<T> failure(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }
}
