package com.oufeng.ecommerceuserprofile.common;

/**
 * 系统统一业务状态码。
 * HTTP 状态码用于表达协议层结果，本枚举用于表达前端可识别的业务结果。
 */
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "用户未登录或登录状态已失效"),
    FORBIDDEN(40300, "没有权限执行该操作"),
    NOT_FOUND(40400, "请求的资源不存在"),
    INTERNAL_ERROR(50000, "系统内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }

    public String getMessage() { return message; }
}
