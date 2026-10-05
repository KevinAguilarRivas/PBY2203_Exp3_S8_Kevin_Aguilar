package com.bancoxyz.core.kafka;

import com.bancoxyz.core.event.RetiroEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica eventos de retiro aprobado en el topic bancoxyz.retiros.
 * Este servicio desacopla la operacion de persistencia (CuentasController)
 * del canal de eventos, siguiendo el patron Event-Driven Architecture.
 */
@Service
public class RetiroEventProducer {

    private static final Logger log = LoggerFactory.getLogger(RetiroEventProducer.class);

    @Value("${kafka.topic.retiro:bancoxyz.retiros}")
    private String topicRetiro;

    private final KafkaTemplate<String, RetiroEvent> kafkaTemplate;

    public RetiroEventProducer(KafkaTemplate<String, RetiroEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarRetiro(RetiroEvent evento) {
        kafkaTemplate.send(topicRetiro, String.valueOf(evento.cuentaId()), evento);
        log.info("Evento de retiro publicado en topic={} cuentaId={} monto={}",
                topicRetiro, evento.cuentaId(), evento.monto());
    }
}
