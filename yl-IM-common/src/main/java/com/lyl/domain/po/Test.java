package com.lyl.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lyl.enums.TestTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test")
public class Test extends BasePO {
    @TableField("name")
    private String name;

    @TableField("age")
    private Integer age;

    @TableField("type")
    private TestTypeEnum type;
}
