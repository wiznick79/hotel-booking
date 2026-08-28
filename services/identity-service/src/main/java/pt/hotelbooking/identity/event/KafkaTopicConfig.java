package pt.hotelbooking.identity.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(
        name = "identity-events.topics.auto-create",
        havingValue = "true",
        matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    NewTopic identityEventsTopic(@Value("${identity-events.topic:identity-events}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
