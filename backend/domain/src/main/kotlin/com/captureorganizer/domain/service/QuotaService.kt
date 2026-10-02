package com.captureorganizer.domain.service

import com.captureorganizer.domain.entity.UserQuota
import com.captureorganizer.domain.repo.UserQuotaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class QuotaService(
    private val quotaRepository: UserQuotaRepository,
) {
    @Transactional
    fun ensureQuota(userId: UUID): UserQuota =
        quotaRepository.findById(userId).orElseGet {
            quotaRepository.save(UserQuota(userId = userId))
        }

    @Transactional
    fun chargeTokens(userId: UUID, tokens: Int) {
        val quota = ensureQuota(userId)
        val today = LocalDate.now()
        if (quota.quotaDay != today) {
            quota.quotaDay = today
            quota.tokensUsedToday = 0
        }
        require(quota.tokensUsedToday + tokens <= quota.dailyTokenBudget) {
            "Daily AI token budget exceeded"
        }
        quota.tokensUsedToday += tokens
        quotaRepository.save(quota)
    }
}
