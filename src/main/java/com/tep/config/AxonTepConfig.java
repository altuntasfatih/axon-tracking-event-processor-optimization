package com.tep.config;

import com.tep.TestEventHandler;
import lombok.RequiredArgsConstructor;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.common.jdbc.PersistenceExceptionResolver;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.GapAwareTrackingToken;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.messaging.StreamableMessageSource;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.config.AxonConfiguration;
import org.axonframework.springboot.util.RegisterDefaultEntities;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@RegisterDefaultEntities(packages = "org.axonframework.eventsourcing.eventstore.jpa")
public class AxonTepConfig {

    private final AppConfig appConfig;

    @Bean
    public EventStorageEngine eventStorageEngine(Serializer defaultSerializer,
                                                 PersistenceExceptionResolver persistenceExceptionResolver,
                                                 @Qualifier("eventSerializer") Serializer eventSerializer,
                                                 AxonConfiguration configuration,
                                                 EntityManagerProvider entityManagerProvider,
                                                 TransactionManager transactionManager) {


        final JpaEventStorageEngine.Builder builder = JpaEventStorageEngine.builder()
                .snapshotSerializer(defaultSerializer)
                .upcasterChain(configuration.upcasterChain())
                .persistenceExceptionResolver(persistenceExceptionResolver)
                .eventSerializer(eventSerializer)
                .snapshotFilter(configuration.snapshotFilter())
                .entityManagerProvider(entityManagerProvider)
                .transactionManager(transactionManager);
        return new CustomJpaEventStorageEngine(builder, transactionManager);
    }

    @Bean
    public TestEventHandler demoEventHandler(EventProcessingConfigurer eventProcessingConfigurer) {

        final TestEventHandler demoEventHandler = new TestEventHandler();

        final AppConfig.EventProcessingConfig eventProcessingConfig = appConfig.getEventProcessing();
        String processingGroupName = appConfig.getEventProcessing().getProcessingGroupName();

        eventProcessingConfigurer.registerEventHandler(configuration -> demoEventHandler)
                .registerListenerInvocationErrorHandler(processingGroupName, configuration -> new EventPublishLoggingErrorHandler())
                .assignHandlerTypesMatching(processingGroupName, clazz -> clazz.isAssignableFrom(TestEventHandler.class))
                .registerTrackingEventProcessor(processingGroupName, c -> {
                    EventBus eventBus = c.eventBus();
                    if (!(eventBus instanceof StreamableMessageSource)) {
                        throw new AxonConfigurationException("Cannot create Tracking Event Processor with name '" + processingGroupName + "'. " + "The available EventBus does not support tracking processors.");
                    }
                    return (StreamableMessageSource) eventBus;
                }, c -> TrackingEventProcessorConfiguration.forParallelProcessing(eventProcessingConfig.getParallelProcessingCount())
                        .andInitialSegmentsCount(eventProcessingConfig.getSegmentationCount())
                        .andBatchSize(eventProcessingConfig.getBatchSize())
                        .andInitialTrackingToken(_messageSource -> new GapAwareTrackingToken(10101235, List.of(10100734L, 10098934L, 10093797L, 10094725L, 10098865L))));
        return demoEventHandler;
    }
}