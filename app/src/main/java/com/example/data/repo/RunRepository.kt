package com.example.data.repo

import com.example.data.db.ProfileDao
import com.example.data.db.ActivityDao
import com.example.data.db.TerritoryDao
import com.example.data.model.ProfileEntity
import com.example.data.model.ActivityEntity
import com.example.data.model.TerritoryEntity
import com.example.data.model.LatLngPoint
import com.example.data.SupabaseService
import kotlinx.coroutines.flow.Flow
import kotlin.math.*

class RunRepository(
    private val profileDao: ProfileDao,
    private val activityDao: ActivityDao,
    private val territoryDao: TerritoryDao
) {
    // Shared in-memory active session (simulates the Supabase Auth session)
    private var currentUserId: String? = null
    private var currentUserEmail: String? = null

    fun setSession(userId: String, email: String) {
        currentUserId = userId
        currentUserEmail = email
    }

    fun clearSession() {
        currentUserId = null
        currentUserEmail = null
    }

    fun getCurrentUserId(): String? = currentUserId
    fun getCurrentUserEmail(): String? = currentUserEmail

    fun getProfile(userId: String): Flow<ProfileEntity?> {
        return profileDao.getProfileById(userId)
    }

    suspend fun getProfileSync(email: String): ProfileEntity? {
        return profileDao.getProfileByEmailSync(email)
    }

    suspend fun getProfileByIdSync(userId: String): ProfileEntity? {
        return profileDao.getProfileByIdSync(userId)
    }

    suspend fun insertProfile(profile: ProfileEntity) {
        profileDao.insertProfile(profile)
        if (SupabaseService.isConfigured()) {
            SupabaseService.upsertProfile(
                userId = profile.id,
                name = profile.name,
                email = profile.email,
                avatarUrl = profile.avatarUrl
            )
        }
    }

    fun getAllActivities(userId: String): Flow<List<ActivityEntity>> {
        return activityDao.getAllActivities(userId)
    }

    suspend fun insertActivity(activity: ActivityEntity) {
        activityDao.insertActivity(activity)
        if (SupabaseService.isConfigured()) {
            val converters = com.example.data.db.Converters()
            SupabaseService.insertActivity(
                userId = activity.userId,
                activityId = activity.id,
                distance = activity.distance,
                duration = activity.duration,
                calories = activity.calories,
                routeJson = converters.fromLatLngList(activity.route)
            )
        }
    }

    fun getActivityById(id: String): Flow<ActivityEntity?> {
        return activityDao.getActivityById(id)
    }

    suspend fun getActivityByIdSync(id: String): ActivityEntity? {
        return activityDao.getActivityByIdSync(id)
    }

    fun getAllTerritories(userId: String): Flow<List<TerritoryEntity>> {
        return territoryDao.getAllTerritories(userId)
    }

    suspend fun insertTerritory(territory: TerritoryEntity) {
        territoryDao.insertTerritory(territory)
        if (SupabaseService.isConfigured()) {
            val converters = com.example.data.db.Converters()
            SupabaseService.insertTerritory(
                userId = territory.userId,
                territoryId = territory.id,
                polygonJson = converters.fromLatLngList(territory.polygon),
                area = territory.area
            )
        }
    }

    /**
     * Check if start and end coordinates of route form a closed loop (Distance < 50 meters).
     */
    fun isClosedLoop(points: List<LatLngPoint>): Boolean {
        if (points.size < 4) return false // Triangle closes with 4th point matching 1st
        val start = points.first()
        val end = points.last()
        val distInMeters = calculateHaversineDistance(start.latitude, start.longitude, end.latitude, end.longitude)
        return distInMeters < 50.0
    }

    /**
     * Calculate 2D region area in square meters using Shoelace formula
     * projected onto flat coordinate space relative to polygon origin.
     */
    fun calculatePolygonArea(points: List<LatLngPoint>): Double {
        if (points.size < 3) return 0.0

        val origin = points.first()
        val earthRadius = 6371000.0 // in meters
        val latRad = Math.toRadians(origin.latitude)
        
        val xList = ArrayList<Double>(points.size)
        val yList = ArrayList<Double>(points.size)
        
        for (p in points) {
            val dLat = Math.toRadians(p.latitude - origin.latitude)
            val dLng = Math.toRadians(p.longitude - origin.longitude)
            
            val y = dLat * earthRadius
            val x = dLng * earthRadius * cos(latRad)
            xList.add(x)
            yList.add(y)
        }

        var area = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += xList[i] * yList[j]
            area -= xList[j] * yList[i]
        }
        
        return abs(area) / 2.0
    }

    fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
