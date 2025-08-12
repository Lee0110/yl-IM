package com.lyl.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import java.io.Serializable;
import java.util.Objects;

/**
 * 枚举基础接口
 * 提供获取枚举值和描述的通用方法
 */
public interface IBaseEnum<T extends Serializable> extends IEnum<T> {
    /**
     * 获取枚举描述
     * @return 返回枚举描述
     */
    String getDesc();

    /**
     * 根据值获取对应的枚举实例
     *
     * @param enumClass 枚举类型
     * @param value 枚举值
     * @param <E> 枚举类型
     * @param <T> 值类型
     * @return 对应的枚举实例，如果未找到则返回null
     */
    static <E extends Enum<E> & IBaseEnum<T>, T extends Serializable> E fromValue(Class<E> enumClass, T value) {
        if (value == null) {
            return null;
        }

        for (E enumConstant : enumClass.getEnumConstants()) {
            if (Objects.equals(enumConstant.getValue(), value)) {
                return enumConstant;
            }
        }

        return null;
    }
}
