package com.tep.config;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.EventMessageHandler;
import org.axonframework.eventhandling.ListenerInvocationErrorHandler;

@Slf4j
public class EventPublishLoggingErrorHandler implements ListenerInvocationErrorHandler {

    @Override
    public void onError(Exception exception, EventMessage<?> event, EventMessageHandler eventHandler) throws Exception {
        if (event instanceof DomainEventMessage) {
            log.error("Event publish failed eventId: " + event.getIdentifier(), exception);
        }
        throw exception;
    }
}