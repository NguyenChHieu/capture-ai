package com.captureorganizer.domain.repo

import com.captureorganizer.domain.CaptureStatus
import com.captureorganizer.domain.ItemStatus
import com.captureorganizer.domain.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface AppUserRepository : JpaRepository<AppUser, UUID> {
    fun findByEmail(email: String): AppUser?
}

interface PersonalAccessTokenRepository : JpaRepository<PersonalAccessToken, UUID> {
    fun findByTokenPrefix(prefix: String): List<PersonalAccessToken>
    fun findByUserId(userId: UUID): List<PersonalAccessToken>
}

interface CategoryRepository : JpaRepository<Category, UUID> {
    fun findByUserIdOrderBySortOrderAscNameAsc(userId: UUID): List<Category>
    fun findByUserIdAndName(userId: UUID, name: String): Category?
}

interface CaptureRepository : JpaRepository<Capture, UUID> {
    fun findByUserIdAndClientCaptureId(userId: UUID, clientCaptureId: String): Capture?
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<Capture>
    fun findByUserIdAndStatus(userId: UUID, status: CaptureStatus): List<Capture>
}

interface ItemRepository : JpaRepository<Item, UUID> {
    fun findByUserIdAndStatus(userId: UUID, status: ItemStatus): List<Item>
    fun findByCaptureId(captureId: UUID): List<Item>
    fun findByUserIdOrderByUpdatedAtDesc(userId: UUID): List<Item>

    @Query(
        value = """
            SELECT * FROM item i
            WHERE i.user_id = :userId
              AND (
                to_tsvector('english', coalesce(i.title,'') || ' ' || coalesce(i.body,'')) @@ plainto_tsquery('english', :q)
                OR i.title ILIKE '%' || :q || '%'
                OR i.body ILIKE '%' || :q || '%'
              )
            ORDER BY i.updated_at DESC
            LIMIT 50
        """,
        nativeQuery = true,
    )
    fun searchText(userId: UUID, q: String): List<Item>
}

interface ItemCategoryRepository : JpaRepository<ItemCategory, ItemCategoryId> {
    fun findByIdItemId(itemId: UUID): List<ItemCategory>
    fun deleteByIdItemId(itemId: UUID)
}

interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {
    @Query("SELECT o FROM OutboxEvent o WHERE o.publishedAt IS NULL ORDER BY o.createdAt ASC")
    fun findUnpublished(): List<OutboxEvent>
}

interface UserQuotaRepository : JpaRepository<UserQuota, UUID>

interface ProposedCategoryRepository : JpaRepository<ProposedCategory, UUID> {
    fun findByItemId(itemId: UUID): List<ProposedCategory>
    fun deleteByItemId(itemId: UUID)
}
