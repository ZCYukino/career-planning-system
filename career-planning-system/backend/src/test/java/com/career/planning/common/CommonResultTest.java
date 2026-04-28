package com.career.planning.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommonResult 统一响应测试
 * 验证各种工厂方法产生的响应格式正确
 */
class CommonResultTest {

    @Test
    @DisplayName("success 应返回 code=200 和正确的 data")
    void testSuccess() {
        CommonResult<String> result = CommonResult.success("测试数据");
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals("测试数据", result.getData());
    }

    @Test
    @DisplayName("success 带自定义消息")
    void testSuccessWithMessage() {
        CommonResult<Integer> result = CommonResult.success(42, "自定义消息");
        assertEquals(200, result.getCode());
        assertEquals("自定义消息", result.getMessage());
        assertEquals(42, result.getData());
    }

    @Test
    @DisplayName("success 的 data 为 null 时也应正常")
    void testSuccessWithNull() {
        CommonResult<String> result = CommonResult.success(null);
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("failed 应返回 code=500")
    void testFailed() {
        CommonResult<String> result = CommonResult.failed("操作失败");
        assertEquals(500, result.getCode());
        assertEquals("操作失败", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("validateFailed 应返回 code=400")
    void testValidateFailed() {
        CommonResult<String> result = CommonResult.validateFailed("参数错误");
        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("unauthorized 应返回 code=401")
    void testUnauthorized() {
        CommonResult<String> result = CommonResult.unauthorized("未登录");
        assertEquals(401, result.getCode());
        assertEquals("未登录", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("forbidden 应返回 code=403")
    void testForbidden() {
        CommonResult<String> result = CommonResult.forbidden("无权限");
        assertEquals(403, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("notFound 应返回 code=404")
    void testNotFound() {
        CommonResult<String> result = CommonResult.notFound("资源不存在");
        assertEquals(404, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("serviceUnavailable 应返回 code=503")
    void testServiceUnavailable() {
        CommonResult<String> result = CommonResult.serviceUnavailable("服务不可用");
        assertEquals(503, result.getCode());
        assertEquals("服务不可用", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("setter 正常工作")
    void testSetters() {
        CommonResult<String> result = new CommonResult<>();
        result.setCode(999);
        result.setMessage("自定义");
        result.setData("data");
        assertEquals(999, result.getCode());
        assertEquals("自定义", result.getMessage());
        assertEquals("data", result.getData());
    }
}
