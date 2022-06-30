package com.tep;

import com.tep.config.AppConfig;
import com.tep.event.DepositedEvent;
import com.tep.event.WalletCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.MetaData;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@Slf4j
public class EventGenerator implements CommandLineRunner {
    public static String TYPE = "com.poc.domain.Wallet";

    private final EventGateway eventGateway;
    private final AppConfig.EventGeneration eventGeneration;

    public EventGenerator(EventGateway eventGateway, AppConfig appConfig) {
        this.eventGateway = eventGateway;
        this.eventGeneration = appConfig.getEventGeneration();
    }

    @Override
    public void run(String... args) {
        if (eventGeneration.getEnabled()) {

            log.info("Event generation is started");
            var iterationCount = eventGeneration.getCount();

            IntStream.range(1, iterationCount)
                    .mapToObj(this::generateEvents)
                    .parallel()
                    .forEach(eventGateway::publish);

            log.info("Event generation is finished");
        }
    }

    private List<GenericDomainEventMessage> generateEvents(int iterationCount) {
        final MetaData metaData = MetaData.with("thread", Thread.currentThread().getName())
                .and("iterationCount", iterationCount);
        final String walletId = UUID.randomUUID().toString();
        var createdEvent = new GenericDomainEventMessage<>(TYPE, walletId, 0L, new WalletCreatedEvent(walletId, BigDecimal.ZERO), metaData);
        var depositedEvent = new GenericDomainEventMessage<>(TYPE, walletId, 1L, new DepositedEvent(BigDecimal.TEN), metaData);
        return Arrays.asList(createdEvent, depositedEvent);
    }
}