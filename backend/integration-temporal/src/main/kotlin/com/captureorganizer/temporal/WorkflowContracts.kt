package com.captureorganizer.temporal

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import java.util.UUID

@WorkflowInterface
interface ProcessCaptureWorkflow {
    @WorkflowMethod
    fun run(captureId: UUID, userId: UUID)
}

data class ProcessCaptureResult(
    val itemCount: Int,
    val status: String,
)

@ActivityInterface
interface CaptureActivities {
    @ActivityMethod
    fun fetchCapture(captureId: UUID): CaptureActivityDto

    @ActivityMethod
    fun markProcessing(captureId: UUID)

    @ActivityMethod
    fun extractAndPersist(captureId: UUID, userId: UUID): ProcessCaptureResult

    @ActivityMethod
    fun markFailed(captureId: UUID, message: String)

    @ActivityMethod
    fun publishProcessed(captureId: UUID, userId: UUID, result: ProcessCaptureResult)
}

data class CaptureActivityDto(
    val id: UUID,
    val userId: UUID,
    val rawText: String?,
    val source: String,
)
