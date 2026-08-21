package com.oufeng.ecommerceuserprofile.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 将 Controller 调用链中的异常统一转换为 Result 响应，避免向前端暴露堆栈等内部信息。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 处理业务规则不满足等可预期异常。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        ResultCode resultCode = exception.getResultCode();
        HttpStatus status = resolveHttpStatus(resultCode);
        return ResponseEntity.status(status)
                .body(Result.failure(resultCode, exception.getMessage()));
    }

    /** 处理 @Valid 请求体参数校验失败（如密码长度不足）。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("请求参数校验失败");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failure(ResultCode.BAD_REQUEST, message));
    }

    /** 处理 @Validated 路径/查询参数校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b).orElse("请求参数校验失败");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failure(ResultCode.BAD_REQUEST, message));
    }

    /** 处理请求体 JSON 解析失败（如编码错误、字段类型不匹配）。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failure(ResultCode.BAD_REQUEST, "请求体格式错误，请检查 JSON 格式与编码"));
    }

    /** 处理未被业务代码捕获的异常，并记录完整日志供排查。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        LOGGER.error("系统发生未处理异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(ResultCode.INTERNAL_ERROR));
    }

    /** 将业务状态码映射为对应的 HTTP 状态码。 */
    private HttpStatus resolveHttpStatus(ResultCode resultCode) {
        return switch (resultCode) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case SUCCESS -> HttpStatus.OK;
        };
    }
}
