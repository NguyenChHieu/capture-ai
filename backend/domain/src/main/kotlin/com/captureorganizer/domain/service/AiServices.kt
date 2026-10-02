package com.captureorganizer.domain.service

import com.captureorganizer.domain.ItemStatus
import com.captureorganizer.domain.entity.Item
import com.captureorganizer.domain.entity.ItemCategory
import com.captureorganizer.domain.entity.ItemCategoryId
import com.captureorganizer.domain.entity.ProposedCategory
import com.captureorganizer.domain.repo.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class ExtractedItemDraft(
    val title: String,
    val body: String,
    val confidence: Double,
    val categoryNames: List<String>,
    val proposedNewCategories: List<String>,
)

@Service
class AiExtractionService(
    private val openAiClient: OpenAiClient,
    private val quotaService: QuotaService,
) {
    fun extract(rawText: String?, userId: UUID, categoryNames: List<String>): List<ExtractedItemDraft> {
        val text = rawText?.trim().orEmpty()
        if (text.isBlank()) return emptyList()
        if (text.length < 3 || text.equals("ok", ignoreCase = true)) return emptyList()

        quotaService.chargeTokens(userId, estimateTokens(text))

        return openAiClient.extractItems(text, categoryNames)
    }

    private fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(100)
}

@Service
class ItemPersistenceService(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val itemCategoryRepository: ItemCategoryRepository,
    private val proposedCategoryRepository: ProposedCategoryRepository,
) {
    @Transactional
    fun persistDrafts(
        userId: UUID,
        captureId: UUID,
        drafts: List<ExtractedItemDraft>,
        autoFileThreshold: Double = 0.85,
    ): List<Item> {
        val existingCategories = categoryRepository.findByUserIdOrderBySortOrderAscNameAsc(userId)
        val byName = existingCategories.associateBy { it.name.lowercase() }

        return drafts.map { draft ->
            val item =
                itemRepository.save(
                    Item(
                        userId = userId,
                        captureId = captureId,
                        title = draft.title,
                        body = draft.body,
                        confidence = draft.confidence,
                        status =
                            if (draft.confidence >= autoFileThreshold && draft.categoryNames.isNotEmpty()) {
                                ItemStatus.FILED
                            } else {
                                ItemStatus.PENDING_REVIEW
                            },
                    ),
                )

            draft.categoryNames.forEach { name ->
                byName[name.lowercase()]?.let { cat ->
                    itemCategoryRepository.save(ItemCategory(ItemCategoryId(item.id, cat.id)))
                }
            }
            draft.proposedNewCategories.forEach { name ->
                proposedCategoryRepository.save(ProposedCategory(itemId = item.id, name = name))
            }
            item
        }
    }
}

@Service
class OpenAiClient(
    @org.springframework.beans.factory.annotation.Value("\${app.ai.openai-api-key:}") private val apiKey: String,
    @org.springframework.beans.factory.annotation.Value("\${app.ai.model:gpt-4o-mini}") private val model: String,
) {
    fun extractItems(text: String, categoryNames: List<String>): List<ExtractedItemDraft> {
        if (apiKey.isBlank()) {
            return heuristicExtract(text, categoryNames)
        }
        // Production: call OpenAI HTTP API with structured output
        return heuristicExtract(text, categoryNames)
    }

    private fun heuristicExtract(text: String, categoryNames: List<String>): List<ExtractedItemDraft> {
        val chunks =
            text.split("\n\n", "\n")
                .map { it.trim() }
                .filter { it.length > 2 }
                .ifEmpty { listOf(text) }

        val defaultCategory = categoryNames.firstOrNull() ?: "Learning and books"
        val gymHint = listOf("gym", "workout", "rep", "sets", "deadlift", "squat")
        val cookHint = listOf("recipe", "cook", "bake", "air fryer", "ingredients")

        return chunks.take(5).map { chunk ->
            val lower = chunk.lowercase()
            val category =
                when {
                    gymHint.any { lower.contains(it) } -> categoryNames.find { it.contains("Gym", ignoreCase = true) } ?: defaultCategory
                    cookHint.any { lower.contains(it) } -> categoryNames.find { it.contains("Cook", ignoreCase = true) } ?: defaultCategory
                    else -> defaultCategory
                }
            ExtractedItemDraft(
                title = chunk.take(80).let { if (it.length == 80) "$it…" else it },
                body = chunk,
                confidence = if (chunks.size == 1) 0.9 else 0.75,
                categoryNames = listOf(category),
                proposedNewCategories = emptyList(),
            )
        }
    }
}

@Service
class EmbeddingService(
    private val jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate,
) {
    fun embedItems(items: List<Item>) {
        items.forEach { item ->
            val vector = pseudoEmbedding(item.title + " " + item.body)
            jdbcTemplate.update(
                """
                UPDATE item SET embedding = ?::vector, embedding_model = ? WHERE id = ?
                """.trimIndent(),
                vector,
                "pseudo-384",
                item.id,
            )
        }
    }

    private fun pseudoEmbedding(text: String): String {
        val dims = 384
        val values = DoubleArray(dims)
        text.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }.forEach { token ->
            val h = token.hashCode()
            values[Math.floorMod(h, dims)] += 1.0
        }
        var norm = 0.0
        for (v in values) norm += v * v
        norm = kotlin.math.sqrt(norm).coerceAtLeast(1e-6)
        val formatted = values.joinToString(",") { (it / norm).toString() }
        return "[$formatted]"
    }
}
