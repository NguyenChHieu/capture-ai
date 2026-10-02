package com.captureorganizer.api.web

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@RestController
class RateLimitFilterConfig

@Component
class InMemoryRateLimiter {
    private val counters = ConcurrentHashMap<java.util.UUID, AtomicInteger>()

    fun allow(userId: java.util.UUID, maxPerMinute: Int = 30): Boolean {
        val count = counters.computeIfAbsent(userId) { AtomicInteger(0) }
        return count.incrementAndGet() <= maxPerMinute
    }

    @Scheduled(fixedDelay = 60_000)
    fun reset() {
        counters.clear()
    }
}

@Component
class CaptureRateLimitInterceptor(
    private val limiter: InMemoryRateLimiter,
) : org.springframework.web.servlet.HandlerInterceptor {
    override fun preHandle(
        request: jakarta.servlet.http.HttpServletRequest,
        response: jakarta.servlet.http.HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.requestURI == "/v1/captures" && request.method == "POST") {
            val auth = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
            val userId = auth?.principal as? java.util.UUID ?: return true
            if (!limiter.allow(userId)) {
                response.status = 429
                return false
            }
        }
        return true
    }
}

@Configuration
class WebMvcConfig(
    private val interceptor: CaptureRateLimitInterceptor,
) : org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
    override fun addInterceptors(registry: org.springframework.web.servlet.config.annotation.InterceptorRegistry) {
        registry.addInterceptor(interceptor)
    }
}

@RestController
class PrivacyController {
    @GetMapping("/v1/privacy")
    fun privacy(): Map<String, String> =
        mapOf(
            "summary" to "See docs/PRIVACY.md in repository",
            "subprocessors" to "Optional LLM API providers when AI enabled",
        )
}
