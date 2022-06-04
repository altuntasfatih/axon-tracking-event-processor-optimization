package com.tep.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private EventProcessingConfig eventProcessing = new EventProcessingConfig();
    private EventGeneration eventGeneration = new EventGeneration();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventProcessingConfig {

        private String processingGroupName;
        private Boolean optimizeEventConsumption;
        private Integer parallelProcessingCount;
        private Integer segmentationCount;
        private Integer batchSize;
        private Duration cacheFetchDelay;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventGeneration {
        private Boolean enabled;
        private Integer count;
    }
}
