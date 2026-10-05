package com.bancoxyz.core.kafka;

import com.bancoxyz.core.event.RetiroEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumidor Kafka: escucha el topic bancoxyz.retiros y registra en log cada
 * retiro aprobado. En un sistema real, aqui se actualizaria un sistema de
 * auditoria, se enviaria una notificacion o se desencadenaria otro flujo.
 */
@Service
public class RetiroEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RetiroEventConsumer.class);

    @KafkaListener(topics = "${kafka.topic.retiro:bancoxyz.retiros}", groupId = "core-service-group")
    public void consumirRetiro(RetiroEvent evento) {
        log.info("[KAFKA] Retiro procesado: cuentaId={} monto={} saldoAnterior={} saldoNuevo={} ts={}",
                evento.cuentaId(), evento.monto(), evento.saldoAnterior(),
                evento.saldoNuevo(), evento.timestamp());
    }
}
