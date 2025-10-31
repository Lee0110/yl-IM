package com.lyl.service.message.dto;

import com.lyl.domain.dto.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MessageDTO extends BaseDTO {

    private Long senderId;

    private Long receiverId;

    private String content;
}
