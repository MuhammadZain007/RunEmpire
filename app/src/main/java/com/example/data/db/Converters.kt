package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.LatLngPoint
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromLatLngList(value: List<LatLngPoint>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        for (point in value) {
            val obj = JSONObject()
            obj.put("lat", point.latitude)
            obj.put("lng", point.longitude)
            obj.put("t", point.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toLatLngList(value: String?): List<LatLngPoint> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<LatLngPoint>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LatLngPoint(
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", 0.0),
                        timestamp = obj.optLong("t", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
