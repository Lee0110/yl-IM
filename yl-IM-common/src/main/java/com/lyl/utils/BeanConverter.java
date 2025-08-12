package com.lyl.utils;

import com.lyl.enums.IBaseEnum;
import com.lyl.exception.OcsErrorCode;
import com.lyl.exception.OcsException;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对象转换工具类
 * 支持不同对象之间的转换，处理枚举类型转换
 * 枚举类型需要实现IBaseEnum接口
 *
 * @see IBaseEnum
 */
public class BeanConverter {

    /**
     * 通常用于DTO、BO转数据库实体<br>
     * 将源对象转换为目标类型对象
     *
     * @param source      源对象
     * @param targetClass 目标类Class
     * @param <T>         目标类泛型
     * @param <S>         源类泛型
     * @return 目标对象
     */
    public static <T, S> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.newInstance();
            BeanUtils.copyProperties(source, target);

            // 处理枚举类型
            Field[] fields = source.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(source);

                // 如果字段值是枚举类型且实现了IBaseEnum接口
                if (value != null && value.getClass().isEnum() && value instanceof IBaseEnum) {
                    String fieldName = field.getName();
                    Field targetField = ReflectionUtils.findField(targetClass, fieldName);
                    if (targetField != null) {
                        targetField.setAccessible(true);
                        // 使用枚举的getValue方法获取值
                        targetField.set(target, ((IBaseEnum<?>) value).getValue());
                    }
                }
            }

            return target;
        } catch (Exception e) {
            throw new OcsException(OcsErrorCode.BEAN_CONVERT_ERROR, e);
        }
    }

    /**
     * DTO、BO转数据库实体，将源对象列表转换为目标类型对象列表
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标类Class
     * @param <T>         目标类泛型
     * @param <S>         源类泛型
     * @return 目标对象列表
     */
    public static <T, S> List<T> convertList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) {
            return null;
        }
        return sourceList.stream()
                .map(source -> convert(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * 通常用于数据库实体转VO<br>
     * 将源对象转换为目标类型对象，支持将普通值转换为枚举
     *
     * @param source      源对象
     * @param targetClass 目标类Class
     * @param <T>         目标类泛型
     * @param <S>         源类泛型
     * @return 目标对象
     */
    public static <T, S> T convertWithEnum(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.newInstance();
            BeanUtils.copyProperties(source, target);

            // 处理目标对象中的枚举类型字段
            Field[] targetFields = targetClass.getDeclaredFields();
            copyToEnumField(source, target, targetFields);

            return target;
        } catch (Exception e) {
            throw new OcsException(OcsErrorCode.BEAN_CONVERT_ERROR, e);
        }
    }

    /**
     * 通常用于数据库实体转VO<br>
     * 将源对象列表转换为目标类型对象列表，支持将普通值转换为枚举
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标类Class
     * @param <T>         目标类泛型
     * @param <S>         源类泛型
     * @return 目标对象列表
     */
    public static <T, S> List<T> convertListWithEnum(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) {
            return null;
        }
        return sourceList.stream()
                .map(source -> convertWithEnum(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * 将源对象的值覆盖到目标对象上，枚举类型字段会转换为对应的值
     *
     * @param source 源对象
     * @param target 目标对象
     * @param <T>    目标类泛型
     * @param <S>    源类泛型
     */
    public static <T, S> void copy(S source, T target) {
        if (source == null || target == null) {
            return;
        }
        try {
            BeanUtils.copyProperties(source, target);
            // 处理枚举类型
            Field[] fields = source.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(source);

                // 如果字段值是枚举类型且实现了IBaseEnum接口
                if (value != null && value.getClass().isEnum() && value instanceof IBaseEnum) {
                    String fieldName = field.getName();
                    Field targetField = ReflectionUtils.findField(target.getClass(), fieldName);
                    if (targetField != null) {
                        targetField.setAccessible(true);
                        // 使用枚举的getValue方法获取值
                        targetField.set(target, ((IBaseEnum<?>) value).getValue());
                    }
                }
            }

        } catch (Exception e) {
            throw new OcsException(OcsErrorCode.BEAN_CONVERT_ERROR, e);
        }
    }

    /**
     * 将源对象的值覆盖到目标对象上，支持将普通值转换为枚举
     *
     * @param source      源对象
     * @param target      目标对象
     * @param <T>         目标类泛型
     * @param <S>         源类泛型
     */
    public static <T, S> void copyWithEnum(S source, T target) {
        if (source == null || target == null) {
            return;
        }
        try {
            BeanUtils.copyProperties(source, target);

            // 处理目标对象中的枚举类型字段
            Field[] targetFields = target.getClass().getDeclaredFields();
            copyToEnumField(source, target, targetFields);

        } catch (Exception e) {
            throw new OcsException(OcsErrorCode.BEAN_CONVERT_ERROR, e);
        }
    }

    /**
     * 将源对象的值覆盖到目标对象上，忽略指定的字段
     *
     * @param source        源对象
     * @param target        目标对象
     * @param ignoreFields  需要忽略的字段名称数组
     * @param <T>           目标类泛型
     * @param <S>           源类泛型
     */
    public static <T, S> void copy(S source, T target, String... ignoreFields) {
        if (source == null || target == null) {
            return;
        }
        Set<String> ignoreFieldsSet = new HashSet<>(Arrays.asList(ignoreFields));
        try {
            BeanUtils.copyProperties(source, target, ignoreFields);
            // 处理枚举类型
            Field[] fields = source.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();

                // 如果字段在忽略列表中，则跳过
                if (ignoreFieldsSet.contains(fieldName)) {
                    continue;
                }

                Object value = field.get(source);

                // 如果字段值是枚举类型且实现了IBaseEnum接口
                if (value != null && value.getClass().isEnum() && value instanceof IBaseEnum) {
                    Field targetField = ReflectionUtils.findField(target.getClass(), fieldName);
                    if (targetField != null) {
                        targetField.setAccessible(true);
                        // 使用枚举的getValue方法获取值
                        targetField.set(target, ((IBaseEnum<?>) value).getValue());
                    }
                }
            }

        } catch (Exception e) {
            throw new OcsException(OcsErrorCode.BEAN_CONVERT_ERROR, e);
        }
    }

    /**
     * 将源对象的值覆盖到目标对象上，支持将普通值转换为枚举，并忽略指定的字段
     *
     * @param source        源对象
     * @param target        目标对象
     * @param ignoreFields  需要忽略的字段名称数组
     * @param <T>           目标类泛型
     * @param <S>           源类泛型
     */
    public static <T, S> void copyWithEnum(S source, T target, String... ignoreFields) {
        if (source == null || target == null) {
            return;
        }
        Set<String> ignoreFieldsSet = new HashSet<>(Arrays.asList(ignoreFields));
        try {
            BeanUtils.copyProperties(source, target, ignoreFields);

            // 处理目标对象中的枚举类型字段，但忽略指定字段
            Field[] targetFields = target.getClass().getDeclaredFields();
            // 过滤掉忽略的字段
            Field[] filteredFields = new Field[targetFields.length];
            int index = 0;
            for (Field field : targetFields) {
                if (ignoreFieldsSet.contains(field.getName())) {
                    filteredFields[index++] = field;
                }
            }

            // 创建一个不包含忽略字段的新数组
            Field[] finalFields = new Field[index];
            System.arraycopy(filteredFields, 0, finalFields, 0, index);

            // 使用现有的 copyToEnumField 工具方法
            copyToEnumField(source, target, finalFields);

        } catch (Exception e) {
            throw new OcsException(OcsErrorCode.BEAN_CONVERT_ERROR, e);
        }
    }

    /**
     * 将源对象中的普通值复制到目标对象的枚举字段中
     *
     * @param source        源对象
     * @param target        目标对象
     * @param targetFields  目标对象的字段数组
     * @param <T>           目标类泛型
     * @param <S>           源类泛型
     * @throws Exception    反射操作可能抛出的异常
     */
    private static <T, S> void copyToEnumField(S source, T target, Field[] targetFields) throws Exception {
        for (Field targetField : targetFields) {
            // 如果目标字段是枚举类型且实现了IBaseEnum接口
            Class<?> fieldType = targetField.getType();
            if (fieldType.isEnum() && IBaseEnum.class.isAssignableFrom(fieldType)) {
                // 获取字段名称
                String fieldName = targetField.getName();
                // 查找源对象中对应名称的字段
                Field sourceField = ReflectionUtils.findField(source.getClass(), fieldName);

                if (sourceField != null) {
                    sourceField.setAccessible(true);
                    targetField.setAccessible(true);

                    // 获取源字段的值
                    Object value = sourceField.get(source);

                    if (value != null) {
                        // 使用IBaseEnum.fromValue方法将值转换为枚举
                        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) fieldType;

                        // 需要正确处理泛型，创建具有正确类型参数的调用
                        Enum<?> enumValue = IBaseEnum.fromValue((Class) enumClass, value);

                        if (enumValue != null) {
                            // 设置转换后的枚举值到目标字段
                            targetField.set(target, enumValue);
                        }
                    }
                }
            }
        }
    }
}
