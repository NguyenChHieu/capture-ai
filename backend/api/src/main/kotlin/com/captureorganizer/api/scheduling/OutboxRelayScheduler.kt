package com.captureorganizer.api.scheduling

import com.captureorganizer.domain.repo.OutboxEventRepository
import com.captureorganizer.kafka.CaptureEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OutboxRelayScheduler(
    private val outboxRepository: OutboxEventRepository,
    private val kafkaPublisher: CaptureEventPublisher,
) {
    @Scheduled(fixedDelay = 2000)
    @Transactional
    fun relay() {
        val batch = outboxRepository.findUnpublished().take(100)
        batch.forEach { event ->
            if (event.eventType == "capture.ingested") {
                kafkaPublisher.publishCaptureIngested(event.payload)
            }
            event.publishedAt = Instant.now()
            outboxRepository.save(event)
        }
    }
}
