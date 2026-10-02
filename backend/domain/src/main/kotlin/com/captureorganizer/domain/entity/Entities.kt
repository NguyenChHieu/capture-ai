package com.captureorganizer.domain.entity

import com.captureorganizer.domain.CaptureStatus
import com.captureorganizer.domain.ItemStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "app_user")
class AppUser(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true) var email: String,
    @Column(name = "password_hash", nullable = false) var passwordHash: String,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "personal_access_token")
class PersonalAccessToken(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) val userId: UUID,
    @Column(nullable = false) var name: String,
    @Column(name = "token_prefix", nullable = false) val tokenPrefix: String,
    @Column(name = "token_hash", nullable = false) val tokenHash: String,
    @Column(nullable = false) val scopes: String = "capture:write,read",
    @Column(name = "expires_at") val expiresAt: Instant? = null,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "category")
class Category(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) val userId: UUID,
    @Column(nullable = false) var name: String,
    var icon: String? = null,
    @Column(name = "sort_order", nullable = false) var sortOrder: Int = 0,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "capture")
class Capture(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) val userId: UUID,
    @Column(nullable = false) val source: String,
    @Column(name = "raw_text", columnDefinition = "TEXT") var rawText: String? = null,
    @Column(name = "source_url", columnDefinition = "TEXT") var sourceUrl: String? = null,
    @Column(name = "conversation_title") var conversationTitle: String? = null,
    @Column(name = "image_object_key") var imageObjectKey: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CaptureStatus = CaptureStatus.QUEUED,
    @Column(name = "error_message", columnDefinition = "TEXT") var errorMessage: String? = null,
    @Column(name = "client_capture_id") val clientCaptureId: String? = null,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "item")
class Item(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) val userId: UUID,
    @Column(name = "capture_id", nullable = false) val captureId: UUID,
    @Column(nullable = false) var title: String,
    @Column(nullable = false, columnDefinition = "TEXT") var body: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ItemStatus = ItemStatus.PENDING_REVIEW,
    var confidence: Double? = null,
    @Column(name = "embedding_model") var embeddingModel: String? = null,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "item_category")
class ItemCategory(
    @EmbeddedId val id: ItemCategoryId,
)

@Embeddable
class ItemCategoryId(
    @Column(name = "item_id") val itemId: UUID,
    @Column(name = "category_id") val categoryId: UUID,
) : java.io.Serializable

@Entity
@Table(name = "outbox_event")
class OutboxEvent(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "aggregate_type", nullable = false) val aggregateType: String,
    @Column(name = "aggregate_id", nullable = false) val aggregateId: UUID,
    @Column(name = "event_type", nullable = false) val eventType: String,
    @Column(nullable = false, columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    val payload: String,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "published_at") var publishedAt: Instant? = null,
)

@Entity
@Table(name = "user_quota")
class UserQuota(
    @Id
    @Column(name = "user_id")
    val userId: UUID,
    @Column(name = "daily_token_budget", nullable = false) var dailyTokenBudget: Int = 50_000,
    @Column(name = "tokens_used_today", nullable = false) var tokensUsedToday: Int = 0,
    @Column(name = "quota_day", nullable = false) var quotaDay: java.time.LocalDate = java.time.LocalDate.now(),
    @Column(name = "max_capture_chars", nullable = false) var maxCaptureChars: Int = 50_000,
)

@Entity
@Table(name = "proposed_category")
class ProposedCategory(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "item_id", nullable = false) val itemId: UUID,
    @Column(nullable = false) val name: String,
)
