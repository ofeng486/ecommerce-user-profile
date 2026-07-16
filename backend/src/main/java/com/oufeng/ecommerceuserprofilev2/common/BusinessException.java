package com.oufeng.ecommerceuserprofilev2.common;

/**
 * 可预期的业务异常。
 * Service 层在业务规则不满足时抛出该异常，由全局异常处理器统一转换为 Result 响应。
 */
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() { return resultCode; }
}
