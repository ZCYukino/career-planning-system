package com.career.planning.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数校验异常（@Valid 注解触发）
     * 返回格式：参数名 + 错误描述
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<String> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return CommonResult.validateFailed(message);
    }

    /**
     * 请求参数缺失异常（@RequestParam required=true 但未传参）
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public CommonResult<String> handleMissingParam(MissingServletRequestParameterException e) {
        String message = "缺少必需参数: " + e.getParameterName();
        log.warn("请求参数缺失: {}", e.getParameterName());
        return CommonResult.validateFailed(message);
    }

    /**
     * 参数类型不匹配异常（如接收 Integer 但传了 abc）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public CommonResult<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 %s 类型错误，期望 %s",
                e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        log.warn("参数类型不匹配: {}", message);
        return CommonResult.validateFailed(message);
    }

    /**
     * 业务异常（由 Service 层主动抛出）
     * 不打印堆栈，只记录消息
     */
    @ExceptionHandler(BusinessException.class)
    public CommonResult<String> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return CommonResult.failed(e.getMessage());
    }

    /**
     * 空指针异常（内部 BUG，不对外暴露堆栈）
     */
    @ExceptionHandler(NullPointerException.class)
    public CommonResult<String> handleNPE(NullPointerException e) {
        log.error("空指针异常（系统内部错误）", e);
        return CommonResult.failed("系统内部错误，请稍后重试");
    }

    /**
     * 非法参数异常（内部 BUG）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResult<String> handleIllegalArg(IllegalArgumentException e) {
        log.error("非法参数异常（系统内部错误）", e);
        return CommonResult.failed("系统内部错误，请稍后重试");
    }

    /**
     * 通用的未分类异常（内部 BUG）
     * ⚠️ 对外只返回友好消息，完整信息记录到日志
     */
    @ExceptionHandler(Exception.class)
    public CommonResult<String> handleException(Exception e) {
        log.error("系统未处理异常", e);
        return CommonResult.failed("系统繁忙，请稍后重试");
    }
}
