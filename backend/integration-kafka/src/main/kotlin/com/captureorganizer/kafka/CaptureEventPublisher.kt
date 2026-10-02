package com.captureorganizer.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class CaptureEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${app.kafka.topic.capture-ingested:capture.ingested}") private val captureIngestedTopic: String,
    @Value("\${app.kafka.topic.capture-processed:capture.processed}") private val captureProcessedTopic: String,
) {
    fun publishCaptureIngested(jsonPayload: String) {
        kafkaTemplate.send(ProducerRecord(captureIngestedTopic, jsonPayload))
    }

    fun publishCaptureProcessed(jsonPayload: String) {
        kafkaTemplate.send(ProducerRecord(captureProcessedTopic, jsonPayload))
    }
}
