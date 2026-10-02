package com.captureorganizer.domain

enum class CaptureStatus {
    QUEUED,
    PROCESSING,
    READY,
    FAILED,
    NO_ITEMS,
}

enum class ItemStatus {
    PENDING_REVIEW,
    FILED,
    ARCHIVED,
}
