package com.career.planning.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 空安全工具类
 * 统一处理各种空指针场景，避免NPE
 */
public final class NullSafeUtil {

    private NullSafeUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== 对象操作 ====================

    /**
     * 安全获取对象，如果为null返回默认值
     */
    public static <T> T getOrDefault(T obj, T defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    /**
     * 安全获取对象，如果为null返回Supplier提供的值
     */
    public static <T> T getOrGet(T obj, Supplier<T> defaultSupplier) {
        return obj != null ? obj : defaultSupplier.get();
    }

    /**
     * 安全获取对象，如果为null返回null（显式声明）
     */
    public static <T> T getOrNull(T obj) {
        return obj;
    }

    /**
     * 判断对象是否为null
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 判断对象是否不为null
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * 转换为Optional
     */
    public static <T> Optional<T> toOptional(T obj) {
        return Optional.ofNullable(obj);
    }

    // ==================== 集合操作 ====================

    /**
     * 安全获取Collection，如果是null返回空集合
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getOrEmptyList(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 安全获取Map，如果是null返回空Map
     */
    public static <M extends Map<?, ?>> M getOrEmptyMap(M map) {
        return map != null ? map : (M) java.util.Collections.emptyMap();
    }

    /**
     * 安全获取集合大小
     */
    public static int sizeOf(Collection<?> collection) {
        return collection != null ? collection.size() : 0;
    }

    /**
     * 安全获取Map大小
     */
    public static int sizeOf(Map<?, ?> map) {
        return map != null ? map.size() : 0;
    }

    /**
     * 判断集合是否为空（null或size为0）
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    // ==================== 数组操作 ====================

    /**
     * 安全获取数组长度
     */
    public static int lengthOf(Object[] array) {
        return array != null ? array.length : 0;
    }

    /**
     * 判断数组是否为空
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 判断数组是否不为空
     */
    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    // ==================== 链式调用支持（核心防NPE） ====================

    /**
     * 安全调用多层嵌套get方法
     * 示例: NullSafeUtil.safeGet(() -> user.getProfile().getAddress().getCity(), "未知")
     * 当发生NPE或返回null时，都返回默认值
     */
    public static <T> T safeGet(Supplier<T> supplier, T defaultValue) {
        try {
            T result = supplier.get();
            return result != null ? result : defaultValue;
        } catch (NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * 安全调用多层嵌套get方法，null时抛业务异常
     */
    public static <T> T safeGetOrThrow(Supplier<T> supplier, String errorMessage) {
        try {
            T result = supplier.get();
            if (result == null) {
                throw new IllegalStateException(errorMessage);
            }
            return result;
        } catch (NullPointerException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }

    /**
     * 安全调用多层嵌套get方法，null时抛自定义异常
     */
    public static <T, E extends RuntimeException> T safeGetOrThrow(
            Supplier<T> supplier, Supplier<E> exceptionSupplier) {
        try {
            T result = supplier.get();
            if (result == null) {
                throw exceptionSupplier.get();
            }
            return result;
        } catch (NullPointerException e) {
            throw exceptionSupplier.get();
        }
    }

    // ==================== 字符串操作 ====================

    /**
     * 安全获取字符串，如果是null返回空字符串
     */
    public static String getOrEmpty(String str) {
        return str != null ? str : "";
    }

    /**
     * 安全获取字符串，如果是null或空白返回默认值
     */
    public static String getOrDefaultIfBlank(String str, String defaultValue) {
        return StringUtil.isNotBlank(str) ? str : defaultValue;
    }

    // ==================== Map操作 ====================

    /**
     * 安全获取Map中的值
     */
    public static <K, V> V getFromMap(Map<K, V> map, K key) {
        return map != null ? map.get(key) : null;
    }

    /**
     * 安全获取Map中的值，为null时返回默认值
     */
    public static <K, V> V getFromMapOrDefault(Map<K, V> map, K key, V defaultValue) {
        return map != null ? map.getOrDefault(key, defaultValue) : defaultValue;
    }

    /**
     * 安全获取Map中的值，为null时抛异常
     */
    public static <K, V> V getFromMapOrThrow(Map<K, V> map, K key, String errorMessage) {
        if (map == null) {
            throw new IllegalStateException(errorMessage);
        }
        V value = map.get(key);
        if (value == null && !map.containsKey(key)) {
            throw new IllegalStateException(errorMessage);
        }
        return value;
    }
}
