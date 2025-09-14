package com.yy.askew.map.data

import com.amap.api.services.core.LatLonPoint
import org.junit.Assert.*
import org.junit.Test

class LocationDataTest {

    @Test
    fun toAndFromLatLonPoint_roundTrip() {
        val lat = 23.129110
        val lon = 113.264385
        val loc = Location(lat, lon, "Guangzhou", "Center")
        val point: LatLonPoint = loc.toLatLonPoint()
        assertEquals(lat, point.latitude, 1e-6)
        assertEquals(lon, point.longitude, 1e-6)

        val back = Location.fromLatLonPoint(point, loc.address, loc.name)
        assertEquals(loc.latitude, back.latitude, 0.0)
        assertEquals(loc.longitude, back.longitude, 0.0)
        assertEquals(loc.address, back.address)
        assertEquals(loc.name, back.name)
    }
}

