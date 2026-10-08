package com.example

import com.example.data.OpenWebRxParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenWebRxParserTest {

    @Test
    fun parse_validAdsbUpdateMessage_returnsAircraftList() {
        val sampleJson = """
            {
                "type": "update",
                "value": [
                    {
                        "icao": "4B821A",
                        "callsign": "AFL1420",
                        "mode": "ADSB",
                        "location": {
                            "lat": 55.7558,
                            "lon": 37.6173
                        },
                        "altitude": 32000,
                        "speed": 450,
                        "course": 180
                    },
                    {
                        "icao": "112233",
                        "callsign": "TEST_OTHER",
                        "mode": "OGN",
                        "location": {
                            "lat": 55.0,
                            "lon": 37.0
                        }
                    }
                ]
            }
        """.trimIndent()

        val parsed = OpenWebRxParser.parse(sampleJson)

        assertEquals("update", parsed.type)
        assertEquals(1, parsed.aircraftList.size)

        val plane = parsed.aircraftList[0]
        assertEquals("4B821A", plane.icao)
        assertEquals("AFL1420", plane.callsign)
        assertEquals(55.7558, plane.latitude, 0.0001)
        assertEquals(37.6173, plane.longitude, 0.0001)
        assertEquals(32000, plane.altitude)
        assertEquals(450, plane.speed)
        assertEquals(180f, plane.course, 0.1f)
    }

    @Test
    fun parse_arrayLocation_handledCorrectly() {
        val sampleJson = """
            {
                "type": "update",
                "value": [
                    {
                        "icao": "ABCDEF",
                        "mode": "adsb",
                        "location": [59.9343, 30.3351],
                        "altitude": 10000,
                        "speed": 280,
                        "course": 90
                    }
                ]
            }
        """.trimIndent()

        val parsed = OpenWebRxParser.parse(sampleJson)
        assertEquals(1, parsed.aircraftList.size)
        val plane = parsed.aircraftList[0]
        assertEquals("ABCDEF", plane.icao)
        assertEquals(59.9343, plane.latitude, 0.0001)
        assertEquals(30.3351, plane.longitude, 0.0001)
    }

    @Test
    fun parse_nonUpdateMessage_returnsEmptyAircraftList() {
        val handshakeJson = """
            {
                "type": "config",
                "value": { "sample_rate": 2400000 }
            }
        """.trimIndent()

        val parsed = OpenWebRxParser.parse(handshakeJson)
        assertEquals("config", parsed.type)
        assertTrue(parsed.aircraftList.isEmpty())
    }
}
