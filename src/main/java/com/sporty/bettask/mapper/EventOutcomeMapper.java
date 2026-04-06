package com.sporty.bettask.mapper;

import com.sporty.bettask.domain.EventOutcomeMessage;
import com.sporty.bettask.dto.EventOutcomeRequest;
import org.mapstruct.Mapper;

@Mapper
public interface EventOutcomeMapper {

    EventOutcomeMessage toMessage(EventOutcomeRequest request);
}
