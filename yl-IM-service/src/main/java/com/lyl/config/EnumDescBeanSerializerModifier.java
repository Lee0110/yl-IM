package com.lyl.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.lyl.enums.IBaseEnum;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 为实现 IBaseEnum 的枚举字段自动追加一个 "xxxDesc" 虚拟字段，值取自 getDesc()
 * 仅在原对象上不存在同名字段时追加，避免覆盖业务自定义字段。
 */
public class EnumDescBeanSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        if (CollectionUtils.isEmpty(beanProperties)) {
            return beanProperties;
        }

        // 已存在的字段名，避免重复追加
        Set<String> nameSet = beanProperties.stream().map(BeanPropertyWriter::getName).collect(Collectors.toSet());

        return beanProperties.stream().flatMap(bpw -> {
            JavaType type = bpw.getType();
            if (Objects.isNull(type)) {
                return Stream.of(bpw);
            }
            Class<?> raw = type.getRawClass();
            if (Objects.nonNull(raw) && raw.isEnum() && IBaseEnum.class.isAssignableFrom(raw)) {
                String descName = bpw.getName() + "Desc";
                if (!nameSet.contains(descName)) {
                    nameSet.add(descName);
                    return Stream.of(bpw, new EnumDescWriter(bpw, descName));
                }
            }
            return Stream.of(bpw);
        }).collect(Collectors.toList());
    }

    /**
     * 虚拟字段写入器：把 sourceWriter 对应的枚举字段转换为 "xxxDesc" 文本输出
     */
    static class EnumDescWriter extends BeanPropertyWriter {
        private final BeanPropertyWriter sourceWriter;

        EnumDescWriter(BeanPropertyWriter base, String newName) {
            super(base, new PropertyName(newName));
            this.sourceWriter = base;
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            Object enumVal = sourceWriter.get(bean);
            if (Objects.isNull(enumVal)) {
                // 源枚举为 null 时，也输出字段，值为 null，方便前端解构
                gen.writeNullField(getName());
                return;
            }
            if (enumVal instanceof IBaseEnum) {
                String desc = ((IBaseEnum<?>) enumVal).getDesc();
                if (desc == null) {
                    gen.writeNullField(getName());
                } else {
                    gen.writeStringField(getName(), desc);
                }
            }
        }
    }
}
