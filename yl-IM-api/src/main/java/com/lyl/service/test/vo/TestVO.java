package com.lyl.service.test.vo;

import com.lyl.domain.vo.BaseVO;
import com.lyl.enums.TestTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TestVO extends BaseVO {
    private String name;

    private Integer age;

    private TestTypeEnum type;
}
