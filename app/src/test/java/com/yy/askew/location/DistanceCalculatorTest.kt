package com.yy.askew.location

import org.junit.Assert.*
import org.junit.Test

class DistanceCalculatorTest {

    @Test
    fun calculateGpsDistance_knownCities_isReasonable() {
        // New York to Los Angeles ~ 3936 km
        val nyLat = 40.7128
        val nyLon = -74.0060
        val laLat = 34.0522
        val laLon = -118.2437

        val d = DistanceCalculator.calculateGpsDistance(nyLat, nyLon, laLat, laLon)
        assertTrue("Distance should be around 3,936,000m", d in 3_300_000.0..4_500_000.0)
    }

    @Test
    fun getRssiDistanceLevel_thresholds() {
        assertEquals(DistanceLevel.IMMEDIATE, DistanceCalculator.getRssiDistanceLevel(-25))
        assertEquals(DistanceLevel.NEAR, DistanceCalculator.getRssiDistanceLevel(-45))
        assertEquals(DistanceLevel.FAR, DistanceCalculator.getRssiDistanceLevel(-65))
        assertEquals(DistanceLevel.VERY_FAR, DistanceCalculator.getRssiDistanceLevel(-80))
    }

    @Test
    fun getCombinedDistance_onlyBluetooth() {
        val result = DistanceCalculator.getCombinedDistance(
            gpsDistance = null,
            bluetoothDistance = 2.5,
            gpsAccuracy = null
        )
        assertEquals(DistanceMethod.BLUETOOTH, result.method)
        assertEquals(EstimatedAccuracy.MEDIUM, result.accuracy)
        assertEquals(2.5, result.distance, 0.0)
        assertTrue(result.isValid())
    }

    @Test
    fun getCombinedDistance_onlyGps_accuracyLevels() {
        val high = DistanceCalculator.getCombinedDistance(10.0, null, 3f)
        assertEquals(DistanceMethod.GPS, high.method)
        assertEquals(EstimatedAccuracy.HIGH, high.accuracy)

        val medium = DistanceCalculator.getCombinedDistance(10.0, null, 10f)
        assertEquals(EstimatedAccuracy.MEDIUM, medium.accuracy)

        val low = DistanceCalculator.getCombinedDistance(10.0, null, 30f)
        assertEquals(EstimatedAccuracy.LOW, low.accuracy)
    }

    @Test
    fun getCombinedDistance_bothSources_priorityRules() {
        // Far distance -> GPS priority
        val far = DistanceCalculator.getCombinedDistance(120.0, 5.0, 12f)
        assertEquals(DistanceMethod.COMBINED_GPS_PRIORITY, far.method)

        // Near distance -> Bluetooth priority (if GPS not highly accurate)
        val near = DistanceCalculator.getCombinedDistance(20.0, 3.0, 20f)
        assertEquals(DistanceMethod.COMBINED_BLUETOOTH_PRIORITY, near.method)

        // Near distance but GPS high accuracy -> GPS priority
        val nearHighAcc = DistanceCalculator.getCombinedDistance(20.0, 3.0, 5f)
        assertEquals(DistanceMethod.COMBINED_GPS_PRIORITY, nearHighAcc.method)
    }

    @Test
    fun getCombinedDistance_none() {
        val result = DistanceCalculator.getCombinedDistance(null, null, null)
        assertEquals(DistanceMethod.UNKNOWN, result.method)
        assertFalse(result.isValid())
    }
}

