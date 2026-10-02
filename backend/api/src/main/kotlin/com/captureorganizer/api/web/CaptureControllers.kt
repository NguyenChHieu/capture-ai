package com.captureorganizer.api.web

import com.captureorganizer.api.security.currentUserId
import com.captureorganizer.domain.CaptureStatus
import com.captureorganizer.domain.ItemStatus
import com.captureorganizer.domain.entity.Category
import com.captureorganizer.domain.entity.ItemCategory
import com.captureorganizer.domain.entity.ItemCategoryId
import com.captureorganizer.domain.repo.*
import com.captureorganizer.domain.service.CaptureIngestService
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/v1/captures")
class CaptureController(
    private val captureIngestService: CaptureIngestService,
    private val captureRepository: CaptureRepository,
    private val itemRepository: ItemRepository,
) {
    @PostMapping
    fun create(@RequestBody body: CreateCaptureRequest): CaptureResponse {
        val userId = currentUserId()
        val capture =
            captureIngestService.ingest(
                userId = userId,
                source = body.source,
                rawText = body.rawText,
                sourceUrl = body.sourceUrl,
                conversationTitle = body.conversationTitle,
                clientCaptureId = body.clientCaptureId,
            )
        return capture.toResponse(emptyList())
    }

    @GetMapping
    fun list(): List<CaptureResponse> {
        val userId = currentUserId()
        return captureRepository.findByUserIdOrderByCreatedAtDesc(userId).map { cap ->
            cap.toResponse(itemRepository.findByCaptureId(cap.id))
        }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): CaptureResponse {
        val userId = currentUserId()
        val capture =
            captureRepository.findById(id).orElseThrow()
        if (capture.userId != userId) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return capture.toResponse(itemRepository.findByCaptureId(capture.id))
    }

    @GetMapping("/{id}/status")
    fun status(@PathVariable id: UUID): CaptureStatusResponse {
        val userId = currentUserId()
        val capture =
            captureRepository.findById(id).orElseThrow()
        if (capture.userId != userId) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return CaptureStatusResponse(capture.id, capture.status, capture.errorMessage, capture.updatedAt)
    }
}

@RestController
@RequestMapping("/v1/items")
class ItemController(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val itemCategoryRepository: ItemCategoryRepository,
    private val proposedCategoryRepository: ProposedCategoryRepository,
) {
    @GetMapping
    fun list(@RequestParam(required = false) status: ItemStatus?): List<ItemResponse> {
        val userId = currentUserId()
        val items =
            if (status != null) {
                itemRepository.findByUserIdAndStatus(userId, status)
            } else {
                itemRepository.findByUserIdOrderByUpdatedAtDesc(userId)
            }
        return items.map { it.toResponse(categoryRepository, itemCategoryRepository, proposedCategoryRepository) }
    }

    @GetMapping("/search")
    fun search(@RequestParam q: String): List<ItemResponse> {
        val userId = currentUserId()
        return itemRepository.searchText(userId, q).map {
            it.toResponse(categoryRepository, itemCategoryRepository, proposedCategoryRepository)
        }
    }

    @PostMapping("/{id}/accept")
    fun accept(@PathVariable id: UUID): ItemResponse {
        val userId = currentUserId()
        val item = itemRepository.findById(id).orElseThrow()
        if (item.userId != userId) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        item.status = ItemStatus.FILED
        itemRepository.save(item)
        return item.toResponse(categoryRepository, itemCategoryRepository, proposedCategoryRepository)
    }

    @PostMapping("/{id}/categories")
    fun assignCategory(
        @PathVariable id: UUID,
        @RequestBody body: AssignCategoryRequest,
    ): ItemResponse {
        val userId = currentUserId()
        val item = itemRepository.findById(id).orElseThrow()
        if (item.userId != userId) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val category =
            categoryRepository.findById(body.categoryId).orElseThrow()
        if (category.userId != userId) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        itemCategoryRepository.save(ItemCategory(ItemCategoryId(item.id, category.id)))
        item.status = ItemStatus.FILED
        itemRepository.save(item)
        proposedCategoryRepository.deleteByItemId(item.id)
        return item.toResponse(categoryRepository, itemCategoryRepository, proposedCategoryRepository)
    }
}

@RestController
@RequestMapping("/v1/categories")
class CategoryController(
    private val categoryRepository: CategoryRepository,
) {
    @GetMapping
    fun list(): List<CategoryResponse> {
        val userId = currentUserId()
        return categoryRepository.findByUserIdOrderBySortOrderAscNameAsc(userId).map { CategoryResponse(it.id, it.name, it.icon, it.sortOrder) }
    }

    @PostMapping
    fun create(@RequestBody body: CreateCategoryRequest): CategoryResponse {
        val userId = currentUserId()
        val saved =
            categoryRepository.save(
                Category(userId = userId, name = body.name, icon = body.icon, sortOrder = body.sortOrder ?: 0),
            )
        return CategoryResponse(saved.id, saved.name, saved.icon, saved.sortOrder)
    }
}

data class CreateCaptureRequest(
    @field:NotBlank val source: String,
    val rawText: String? = null,
    val sourceUrl: String? = null,
    val conversationTitle: String? = null,
    val clientCaptureId: String? = null,
)

data class CaptureResponse(
    val id: UUID,
    val source: String,
    val rawText: String?,
    val status: CaptureStatus,
    val errorMessage: String?,
    val createdAt: java.time.Instant,
    val items: List<ItemSummary>,
)

data class CaptureStatusResponse(
    val id: UUID,
    val status: CaptureStatus,
    val errorMessage: String?,
    val updatedAt: java.time.Instant,
)

data class ItemSummary(val id: UUID, val title: String, val status: ItemStatus)

data class ItemResponse(
    val id: UUID,
    val captureId: UUID,
    val title: String,
    val body: String,
    val status: ItemStatus,
    val confidence: Double?,
    val categoryIds: List<UUID>,
    val proposedCategories: List<String>,
)

data class CategoryResponse(val id: UUID, val name: String, val icon: String?, val sortOrder: Int)
data class CreateCategoryRequest(@field:NotBlank val name: String, val icon: String? = null, val sortOrder: Int? = null)
data class AssignCategoryRequest(val categoryId: UUID)

private fun com.captureorganizer.domain.entity.Capture.toResponse(items: List<com.captureorganizer.domain.entity.Item>) =
    CaptureResponse(
        id = id,
        source = source,
        rawText = rawText,
        status = status,
        errorMessage = errorMessage,
        createdAt = createdAt,
        items = items.map { ItemSummary(it.id, it.title, it.status) },
    )

private fun com.captureorganizer.domain.entity.Item.toResponse(
    categoryRepository: CategoryRepository,
    itemCategoryRepository: ItemCategoryRepository,
    proposedCategoryRepository: ProposedCategoryRepository,
): ItemResponse {
    val categoryIds = itemCategoryRepository.findByIdItemId(id).map { it.id.categoryId }
    val proposed = proposedCategoryRepository.findByItemId(id).map { it.name }
    return ItemResponse(id, captureId, title, body, status, confidence, categoryIds, proposed)
}
