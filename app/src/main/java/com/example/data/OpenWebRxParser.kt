package com.example.data

import com.example.model.Aircraft
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object OpenWebRxParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    data class ParsedMessage(
        val type: String?,
        val aircraftList: List<Aircraft>
    )

    fun parse(rawJson: String): ParsedMessage {
        val rootElement = try {
            json.parseToJsonElement(rawJson)
        } catch (_: Exception) {
            return ParsedMessage(null, emptyList())
        }

        if (rootElement !is JsonObject) {
            return ParsedMessage(null, emptyList())
        }

        val type = rootElement["type"]?.jsonPrimitive?.contentOrNull
        if (type != "update") {
            return ParsedMessage(type, emptyList())
        }

        val valueElement = rootElement["value"] ?: return ParsedMessage(type, emptyList())
        val aircrafts = mutableListOf<Aircraft>()
        val now = System.currentTimeMillis()

        when (valueElement) {
            is JsonArray -> {
                for (item in valueElement) {
                    if (item is JsonObject) {
                        parseSingleAircraft(item, now)?.let { aircrafts.add(it) }
                    }
                }
            }
            is JsonObject -> {
                // In some implementations value might be a map of icao -> aircraft or single object
                parseSingleAircraft(valueElement, now)?.let { aircrafts.add(it) }
            }
            else -> {}
        }

        return ParsedMessage(type, aircrafts)
    }

    private fun parseSingleAircraft(obj: JsonObject, timestamp: Long): Aircraft? {
        val mode = obj["mode"]?.jsonPrimitive?.contentOrNull ?: ""
        // Server specifies: take only mode ADSB (case-insensitive check)
        if (!mode.contains("adsb", ignoreCase = true)) {
            return null
        }

        val icaoRaw = obj["icao"]?.jsonPrimitive?.contentOrNull
            ?: obj["id"]?.jsonPrimitive?.contentOrNull
            ?: return null

        val icao = icaoRaw.trim().uppercase()
        if (icao.isBlank()) return null

        val callsign = obj["callsign"]?.jsonPrimitive?.contentOrNull
            ?: obj["flight"]?.jsonPrimitive?.contentOrNull

        // Extract coordinates flexibly
        var lat: Double? = null
        var lon: Double? = null

        val locationElement = obj["location"]
        if (locationElement is JsonObject) {
            lat = locationElement["lat"]?.jsonPrimitive?.doubleOrNull
                ?: locationElement["latitude"]?.jsonPrimitive?.doubleOrNull
            lon = locationElement["lon"]?.jsonPrimitive?.doubleOrNull
                ?: locationElement["lng"]?.jsonPrimitive?.doubleOrNull
                ?: locationElement["longitude"]?.jsonPrimitive?.doubleOrNull
        } else if (locationElement is JsonArray && locationElement.size >= 2) {
            lat = locationElement[0].jsonPrimitive.doubleOrNull
            lon = locationElement[1].jsonPrimitive.doubleOrNull
        }

        if (lat == null || lon == null) {
            lat = obj["lat"]?.jsonPrimitive?.doubleOrNull
                ?: obj["latitude"]?.jsonPrimitive?.doubleOrNull
            lon = obj["lon"]?.jsonPrimitive?.doubleOrNull
                ?: obj["lng"]?.jsonPrimitive?.doubleOrNull
                ?: obj["longitude"]?.jsonPrimitive?.doubleOrNull
        }

        // Must have valid geographic coordinates
        if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            return null
        }

        val altitude = obj["altitude"]?.jsonPrimitive?.intOrNull
            ?: obj["alt"]?.jsonPrimitive?.intOrNull

        val speed = obj["speed"]?.jsonPrimitive?.intOrNull
            ?: obj["spd"]?.jsonPrimitive?.intOrNull
            ?: obj["velocity"]?.jsonPrimitive?.intOrNull

        val course = obj["course"]?.jsonPrimitive?.doubleOrNull
            ?: obj["heading"]?.jsonPrimitive?.doubleOrNull
            ?: obj["track"]?.jsonPrimitive?.doubleOrNull
            ?: 0.0

        return Aircraft(
            icao = icao,
            callsign = callsign?.trim()?.takeIf { it.isNotBlank() },
            latitude = lat,
            longitude = lon,
            altitude = altitude,
            speed = speed,
            course = course.toFloat(),
            mode = mode.uppercase(),
            lastSeenTimestamp = timestamp
        )
    }
}
