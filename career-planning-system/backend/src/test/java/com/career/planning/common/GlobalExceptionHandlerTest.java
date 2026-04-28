package com.career.planning.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 测试
 * 验证各类异常被正确转换为 CommonResult
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("BusinessException 应返回 code=500")
    void testHandleBusinessException() {
        CommonResult<String> result = handler.handleBusiness(new BusinessException("学生不存在"));
        assertEquals(500, result.getCode());
        assertEquals("学生不存在", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("NullPointerException 应返回友好消息")
    void testHandleNPE() {
        CommonResult<String> result = handler.handleNPE(new NullPointerException("test"));
        assertEquals(500, result.getCode());
        assertEquals("系统内部错误，请稍后重试", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("IllegalArgumentException 应返回友好消息")
    void testHandleIllegalArg() {
        CommonResult<String> result = handler.handleIllegalArg(new IllegalArgumentException("bad arg"));
        assertEquals(500, result.getCode());
        assertEquals("系统内部错误，请稍后重试", result.getMessage());
    }

    @Test
    @DisplayName("通用 Exception 应返回友好消息")
    void testHandleGenericException() {
        CommonResult<String> result = handler.handleException(new RuntimeException("unknown"));
        assertEquals(500, result.getCode());
        assertEquals("系统繁忙，请稍后重试", result.getMessage());
    }
}
