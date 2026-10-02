package com.captureorganizer.worker.config

import com.captureorganizer.temporal.CaptureActivitiesImpl
import com.captureorganizer.temporal.ProcessCaptureWorkflow
import com.captureorganizer.temporal.ProcessCaptureWorkflowImpl
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.UUID

@Configuration
class TemporalConfig(
    @Value("\${app.temporal.target}") private val target: String,
    @Value("\${app.temporal.namespace}") private val namespace: String,
    @Value("\${app.temporal.task-queue}") private val taskQueue: String,
) {
    @Bean
    fun workflowServiceStubs(): WorkflowServiceStubs =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget(target).build(),
        )

    @Bean
    fun workflowClient(stubs: WorkflowServiceStubs): WorkflowClient =
        WorkflowClient.newInstance(stubs, io.temporal.client.WorkflowClientOptions.newBuilder().setNamespace(namespace).build())
}

@Component
class TemporalWorkerStarter(
    private val workflowClient: WorkflowClient,
    private val activities: CaptureActivitiesImpl,
    @Value("\${app.temporal.task-queue}") private val taskQueue: String,
) {
    @PostConstruct
    fun start() {
        val factory = WorkerFactory.newInstance(workflowClient)
        val worker = factory.newWorker(taskQueue)
        worker.registerWorkflowImplementationTypes(ProcessCaptureWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(activities)
        factory.start()
    }
}

@Component
class CaptureIngestedListener(
    private val workflowClient: WorkflowClient,
    @Value("\${app.temporal.task-queue}") private val taskQueue: String,
    @Value("\${app.kafka.topic.capture-ingested}") private val topic: String,
) {
    private val mapper = jacksonObjectMapper()

    @KafkaListener(topics = ["\${app.kafka.topic.capture-ingested}"])
    fun onCaptureIngested(payload: String) {
        val map: Map<String, Any> = mapper.readValue(payload)
        val captureId = UUID.fromString(map["captureId"].toString())
        val userId = UUID.fromString(map["userId"].toString())
        val workflowId = "process-capture-$captureId"
        val workflow =
            workflowClient.newWorkflowStub(
                ProcessCaptureWorkflow::class.java,
                WorkflowOptions.newBuilder()
                    .setWorkflowId(workflowId)
                    .setTaskQueue(taskQueue)
                    .build(),
            )
        WorkflowClient.start(workflow::run, captureId, userId)
    }
}
