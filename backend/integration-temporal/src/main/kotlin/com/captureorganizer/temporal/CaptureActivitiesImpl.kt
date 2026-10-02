package com.captureorganizer.temporal

import com.captureorganizer.domain.CaptureStatus
import com.captureorganizer.domain.repo.CaptureRepository
import com.captureorganizer.domain.repo.CategoryRepository
import com.captureorganizer.domain.service.AiExtractionService
import com.captureorganizer.domain.service.CaptureIngestService
import com.captureorganizer.domain.service.EmbeddingService
import com.captureorganizer.domain.service.ItemPersistenceService
import com.captureorganizer.kafka.CaptureEventPublisher
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.temporal.activity.Activity
import io.temporal.activity.ActivityOptions
import io.temporal.workflow.Workflow
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class CaptureActivitiesImpl(
    private val captureRepository: CaptureRepository,
    private val captureIngestService: CaptureIngestService,
    private val categoryRepository: CategoryRepository,
    private val aiExtractionService: AiExtractionService,
    private val itemPersistenceService: ItemPersistenceService,
    private val embeddingService: EmbeddingService,
    private val kafkaPublisher: CaptureEventPublisher,
) : CaptureActivities {
    private val mapper = jacksonObjectMapper()

    override fun fetchCapture(captureId: UUID): CaptureActivityDto {
        val capture = captureRepository.findById(captureId).orElseThrow()
        return CaptureActivityDto(
            id = capture.id,
            userId = capture.userId,
            rawText = capture.rawText,
            source = capture.source,
        )
    }

    override fun markProcessing(captureId: UUID) {
        captureIngestService.updateStatus(captureId, CaptureStatus.PROCESSING)
    }

    override fun markFailed(captureId: UUID, message: String) {
        captureIngestService.updateStatus(captureId, CaptureStatus.FAILED, message)
    }

    override fun extractAndPersist(captureId: UUID, userId: UUID): ProcessCaptureResult {
        val capture = captureRepository.findById(captureId).orElseThrow()
        val categories = categoryRepository.findByUserIdOrderBySortOrderAscNameAsc(userId).map { it.name }
        val drafts = aiExtractionService.extract(capture.rawText, userId, categories)

        if (drafts.isEmpty()) {
            captureIngestService.updateStatus(captureId, CaptureStatus.NO_ITEMS)
            return ProcessCaptureResult(itemCount = 0, status = CaptureStatus.NO_ITEMS.name)
        }

        val items = itemPersistenceService.persistDrafts(userId, captureId, drafts)
        embeddingService.embedItems(items)
        captureIngestService.updateStatus(captureId, CaptureStatus.READY)
        return ProcessCaptureResult(itemCount = items.size, status = CaptureStatus.READY.name)
    }

    override fun publishProcessed(captureId: UUID, userId: UUID, result: ProcessCaptureResult) {
        val payload =
            mapper.writeValueAsString(
                mapOf(
                    "captureId" to captureId.toString(),
                    "userId" to userId.toString(),
                    "itemCount" to result.itemCount,
                    "status" to result.status,
                ),
            )
        kafkaPublisher.publishCaptureProcessed(payload)
    }
}

class ProcessCaptureWorkflowImpl : ProcessCaptureWorkflow {
    private val activities: CaptureActivities =
        Workflow.newActivityStub(
            CaptureActivities::class.java,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .build(),
        )

    override fun run(captureId: UUID, userId: UUID) {
        activities.fetchCapture(captureId)
        activities.markProcessing(captureId)
        try {
            val result = activities.extractAndPersist(captureId, userId)
            activities.publishProcessed(captureId, userId, result)
        } catch (ex: Exception) {
            activities.markFailed(captureId, ex.message ?: "processing failed")
            throw ex
        }
    }
}
