package com.lyl.service.message.dto;

import com.lyl.domain.dto.BaseDTO;
import com.lyl.enums.MessageTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MessageDTO extends BaseDTO {
    /**
     * 消息类型
     */
    private MessageTypeEnum type;

    private Long senderId;

    private Long receiverId;

    private String content;
}
