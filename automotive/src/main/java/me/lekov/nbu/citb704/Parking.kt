package me.lekov.nbu.citb704

import android.database.Cursor
import android.location.Location
import android.location.LocationManager
import org.json.JSONObject

data class Parking(val zone: String, val name: String, val freeSpots: Int, val location: Location) {

    companion object {
        fun fromJson(json: JSONObject): Parking {
            val locationObj = json.getJSONObject("location")
            return Parking(
                json.getString("zone"),
                json.getString("name"),
                json.getInt("free_places"),
                Location(LocationManager.PASSIVE_PROVIDER).also {
                    it.latitude = locationObj.getDouble("lat")
                    it.longitude = locationObj.getDouble("lon")
                }
            )
        }

        fun cursorToParking(cursor: Cursor): List<Parking> {
            val res = mutableListOf<Parking>()
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                res.add(fromCursor(cursor))
                cursor.moveToNext()
            }
            return res
        }

        fun fromCursor(cursor: Cursor): Parking {
            return Parking(
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ZONE)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.NAME)),
                0,
                Location(LocationManager.PASSIVE_PROVIDER).also {
                    it.latitude =
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.LATITUDE))
                    it.longitude =
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.LONGITUDE))
                }
            )
        }
    }
}