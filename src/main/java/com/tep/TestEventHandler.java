package com.tep;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.EventMessageHandler;

@Slf4j
public class TestEventHandler implements EventMessageHandler {

    @Override
    public Object handle(EventMessage<?> eventMessage) {
        log.info("event is consumed {}", eventMessage);
        return null;
    }
}