package com.captureorganizer.api.web

import com.captureorganizer.api.security.JwtService
import com.captureorganizer.api.security.currentUserId
import com.captureorganizer.domain.entity.AppUser
import com.captureorganizer.domain.entity.PersonalAccessToken
import com.captureorganizer.domain.repo.*
import com.captureorganizer.domain.service.QuotaService
import com.captureorganizer.domain.service.StarterCategoriesService
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping("/v1/auth")
class AuthController(
    private val userRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val quotaService: QuotaService,
    private val starterCategoriesService: StarterCategoriesService,
) {
    @PostMapping("/register")
    fun register(@RequestBody body: RegisterRequest): AuthResponse {
        if (userRepository.findByEmail(body.email.lowercase()) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already registered")
        }
        val user =
            userRepository.save(
                AppUser(
                    email = body.email.lowercase(),
                    passwordHash = passwordEncoder.encode(body.password),
                ),
            )
        quotaService.ensureQuota(user.id)
        starterCategoriesService.seedIfEmpty(user.id)
        return AuthResponse(jwtService.issue(user.id))
    }

    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequest): AuthResponse {
        val user =
            userRepository.findByEmail(body.email.lowercase())
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        if (!passwordEncoder.matches(body.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }
        return AuthResponse(jwtService.issue(user.id))
    }
}

@RestController
@RequestMapping("/v1/me")
class MeController(
    private val userRepository: AppUserRepository,
    private val captureRepository: CaptureRepository,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val itemCategoryRepository: ItemCategoryRepository,
    private val proposedCategoryRepository: ProposedCategoryRepository,
    private val patRepository: PersonalAccessTokenRepository,
    private val quotaRepository: UserQuotaRepository,
) {
    @GetMapping("/export")
    fun export(): Map<String, Any> {
        val userId = currentUserId()
        return mapOf(
            "user" to userRepository.findById(userId).orElseThrow(),
            "captures" to captureRepository.findByUserIdOrderByCreatedAtDesc(userId),
            "items" to itemRepository.findByUserIdOrderByUpdatedAtDesc(userId),
            "categories" to categoryRepository.findByUserIdOrderBySortOrderAscNameAsc(userId),
        )
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAccount() {
        val userId = currentUserId()
        userRepository.deleteById(userId)
    }

    @PostMapping("/tokens")
    fun createPat(@RequestBody body: CreatePatRequest): CreatePatResponse {
        val userId = currentUserId()
        val raw = "co_" + randomToken()
        val prefix = raw.take(12)
        val entity =
            patRepository.save(
                PersonalAccessToken(
                    userId = userId,
                    name = body.name,
                    tokenPrefix = prefix,
                    tokenHash = sha256(raw),
                ),
            )
        return CreatePatResponse(id = entity.id, token = raw, prefix = prefix, name = body.name)
    }

    @GetMapping("/tokens")
    fun listPats(): List<PatSummary> {
        val userId = currentUserId()
        return patRepository.findByUserId(userId).map {
            PatSummary(it.id, it.name, it.tokenPrefix, it.createdAt)
        }
    }

    @GetMapping("/quota")
    fun quota() = quotaRepository.findById(currentUserId()).orElseThrow()

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.getEncoder().encodeToString(digest.digest(value.toByteArray(StandardCharsets.UTF_8)))
    }
}

data class RegisterRequest(@field:Email val email: String, @field:NotBlank val password: String)
data class LoginRequest(@field:Email val email: String, @field:NotBlank val password: String)
data class AuthResponse(val token: String)
data class CreatePatRequest(@field:NotBlank val name: String)
data class CreatePatResponse(val id: UUID, val token: String, val prefix: String, val name: String)
data class PatSummary(val id: UUID, val name: String, val prefix: String, val createdAt: java.time.Instant)
