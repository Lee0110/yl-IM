package com.lyl.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author ${author}
 * @since 2025-08-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test")
public class Test extends BasePO {
    @TableField("name")
    private String name;

    @TableField("age")
    private Integer age;

    @TableField("type")
    private Integer type;
}
