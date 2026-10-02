package com.captureorganizer.domain.service

import com.captureorganizer.domain.CaptureStatus
import com.captureorganizer.domain.entity.Capture
import com.captureorganizer.domain.entity.OutboxEvent
import com.captureorganizer.domain.repo.CaptureRepository
import com.captureorganizer.domain.repo.OutboxEventRepository
import com.captureorganizer.domain.repo.UserQuotaRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CaptureIngestService(
    private val captureRepository: CaptureRepository,
    private val outboxRepository: OutboxEventRepository,
    private val quotaRepository: UserQuotaRepository,
) {
    private val mapper = jacksonObjectMapper()

    @Transactional
    fun ingest(
        userId: UUID,
        source: String,
        rawText: String?,
        sourceUrl: String?,
        conversationTitle: String?,
        clientCaptureId: String?,
    ): Capture {
        if (!clientCaptureId.isNullOrBlank()) {
            captureRepository.findByUserIdAndClientCaptureId(userId, clientCaptureId)?.let { return it }
        }

        val quota = quotaRepository.findById(userId).orElseThrow()
        val textLen = rawText?.length ?: 0
        require(textLen <= quota.maxCaptureChars) {
            "Capture exceeds max size (${quota.maxCaptureChars} chars)"
        }

        val capture =
            Capture(
                userId = userId,
                source = source,
                rawText = rawText?.trim()?.ifBlank { null },
                sourceUrl = sourceUrl,
                conversationTitle = conversationTitle,
                clientCaptureId = clientCaptureId,
                status = CaptureStatus.QUEUED,
            )
        captureRepository.save(capture)

        val payload =
            mapper.writeValueAsString(
                mapOf(
                    "captureId" to capture.id.toString(),
                    "userId" to userId.toString(),
                    "source" to source,
                    "textLength" to textLen,
                ),
            )
        outboxRepository.save(
            OutboxEvent(
                aggregateType = "capture",
                aggregateId = capture.id,
                eventType = "capture.ingested",
                payload = payload,
            ),
        )
        return capture
    }

    @Transactional
    fun updateStatus(
        captureId: UUID,
        status: CaptureStatus,
        errorMessage: String? = null,
    ) {
        val capture = captureRepository.findById(captureId).orElseThrow()
        capture.status = status
        capture.errorMessage = errorMessage
        capture.updatedAt = Instant.now()
        captureRepository.save(capture)
    }
}

@Service
class StarterCategoriesService(
    private val categoryRepository: com.captureorganizer.domain.repo.CategoryRepository,
) {
    private val defaults =
        listOf(
            "Gym and fitness",
            "Cooking and recipes",
            "Funny moments",
            "Work and career",
            "Travel ideas",
            "Money and finance tips",
            "Health and wellness",
            "Learning and books",
            "Shopping and products",
            "Relationships and social",
        )

    @Transactional
    fun seedIfEmpty(userId: UUID) {
        if (categoryRepository.findByUserIdOrderBySortOrderAscNameAsc(userId).isNotEmpty()) return
        defaults.forEachIndexed { index, name ->
            categoryRepository.save(
                com.captureorganizer.domain.entity.Category(
                    userId = userId,
                    name = name,
                    sortOrder = index,
                ),
            )
        }
    }
}
