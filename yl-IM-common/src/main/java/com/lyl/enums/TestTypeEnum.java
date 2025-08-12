package com.lyl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestTypeEnum implements IBaseEnum<Integer> {
    /**
     * 测试类型枚举
     */
    TEST1(1, "测试"),
    TEST2(2, "测试2");

    private final Integer value;
    private final String desc;
}
