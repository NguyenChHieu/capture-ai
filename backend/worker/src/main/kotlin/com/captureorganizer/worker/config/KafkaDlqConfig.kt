package com.captureorganizer.worker.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class CaptureIngestedDlqListener(
    @Value("\${app.kafka.topic.dlq:capture.ingested.dlq}") private val dlqTopic: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${app.kafka.topic.dlq:capture.ingested.dlq}"], groupId = "capture-dlq-logger")
    fun onDlq(payload: String) {
        log.error("Poison message on {}: {}", dlqTopic, payload.take(500))
    }
}
