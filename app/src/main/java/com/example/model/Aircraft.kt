package com.example.model

data class Aircraft(
    val icao: String,
    val callsign: String? = null,
    val latitude: Double,
    val longitude: Double,
    val altitude: Int? = null,    // in feet
    val speed: Int? = null,       // in knots
    val course: Float = 0f,       // in degrees (0 - 360)
    val mode: String = "ADSB",
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    val displayCallsign: String
        get() = callsign?.takeIf { it.isNotBlank() } ?: icao.uppercase()

    fun secondsAgo(now: Long = System.currentTimeMillis()): Long {
        val diff = (now - lastSeenTimestamp) / 1000L
        return if (diff < 0L) 0L else diff
    }
}

enum class SortColumn(val displayName: String) {
    CALLSIGN("Позывной"),
    ICAO("ICAO"),
    ALTITUDE("Высота"),
    SPEED("Скор."),
    COURSE("Курс"),
    AGE("Время")
}

enum class SortDirection {
    ASCENDING,
    DESCENDING;

    fun toggle(): SortDirection = if (this == ASCENDING) DESCENDING else ASCENDING
}

enum class ConnectionStatus {
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

data class RawMessage(
    val id: Long,
    val timestamp: Long,
    val text: String,
    val type: String? = null
)
