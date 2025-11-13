package com.lyl.service.message.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class BroadcastMessageDTO extends MessageDTO {
    private String traceId;

    public BroadcastMessageDTO(MessageDTO messageDTO, String traceId) {
        BeanUtils.copyProperties(messageDTO, this);
        this.traceId = traceId;
    }
}
