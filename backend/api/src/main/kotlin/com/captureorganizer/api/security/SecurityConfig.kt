package com.captureorganizer.api.security

import com.captureorganizer.domain.repo.PersonalAccessTokenRepository
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.SignedJWT
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

@Component
class AuthFilter(
    private val patRepository: PersonalAccessTokenRepository,
    @Value("\${app.jwt.secret}") private val jwtSecret: String,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            resolveUserId(token)?.let { userId ->
                val auth =
                    UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USER")),
                    )
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveUserId(token: String): UUID? {
        if (token.startsWith("co_")) {
            val prefix = token.take(12)
            val hash = sha256(token)
            return patRepository.findByTokenPrefix(prefix)
                .firstOrNull { it.tokenHash == hash }
                ?.userId
        }
        return try {
            val jwt = SignedJWT.parse(token)
            val verifier = MACVerifier(jwtSecret.toByteArray(StandardCharsets.UTF_8))
            if (!jwt.verify(verifier)) return null
            UUID.fromString(jwt.jwtClaimsSet.subject)
        } catch (_: Exception) {
            null
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.getEncoder().encodeToString(digest.digest(value.toByteArray(StandardCharsets.UTF_8)))
    }
}

fun currentUserId(): UUID =
    SecurityContextHolder.getContext().authentication?.principal as? UUID
        ?: throw org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED)
