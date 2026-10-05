package com.bancoxyz.core.kafka;

import com.bancoxyz.core.event.RetiroEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración del productor Kafka para publicar eventos de retiro.
 * Se usa JsonSerializer para serializar RetiroEvent como JSON en el topic.
 */
@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, RetiroEvent> retiroProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, RetiroEvent> retiroKafkaTemplate() {
        return new KafkaTemplate<>(retiroProducerFactory());
    }
}
