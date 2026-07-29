package pt.hotelbooking.notification.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            KafkaOperations<?, ?> kafkaOperations,
            @Value("${notification.kafka.retry-delay-ms:1000}") long retryDelayMs,
            @Value("${notification.kafka.max-retries:3}") long maximumRetries) {
        DeadLetterPublishingRecoverer deadLetterRecoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        deadLetterRecoverer.setFailIfSendResultIsError(true);

        return new DefaultErrorHandler(deadLetterRecoverer, new FixedBackOff(retryDelayMs, maximumRetries));
    }
}
