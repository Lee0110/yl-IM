package com.lyl.domain.dto;

import com.lyl.enums.TestTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class TestDTO extends BaseDTO {
    @NotNull
    private String name;

    @NotNull
    @Max(120)
    @Min(0)
    private Integer age;

    @NotNull
    private TestTypeEnum type;
}
