package me.lekov.nbu.citb704

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class Parking(val zone: String, val name: String, val freeSpots: Int, val location: Location) {

    val freeSpotsText = when {
        freeSpots < 0 -> "извън работно време"
        freeSpots > 0 -> "$freeSpots места"
        else -> "няма свободни места"
    }

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
    }
}

class ParkingDAO(private val context: Context) : DefaultLifecycleObserver {

    private lateinit var db: SQLiteDatabase
    private val livedata = MutableLiveData<List<Parking>>(emptyList())
    val list: LiveData<List<Parking>> = livedata

    override fun onStart(owner: LifecycleOwner) {
        db = DatabaseHelper(context).writableDatabase

        val rows = DatabaseUtils.queryNumEntries(db, PARKING_TABLE_NAME)

        if (rows == 0L) {
            fetchData()
        } else {
            getData()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        db.close()
    }

    fun refresh() {
        fetchData()
    }

    private fun getData() {
        val cursor = db.query(
            PARKING_TABLE_NAME,
            PROJECTION,
            null,
            null,
            null,
            null,
            BaseColumns._ID
        )

        livedata.postValue(cursor.toParkingList().also {
            cursor.close()
        })
    }

    private fun fetchData() {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val json = fetchJson("https://www.sofiatraffic.bg/bg/api/v1/parking/sub-zones/")
            handler.post {
                json?.getJSONArray("parking_sub_zones")?.let {
                    for (i in 0 until it.length()) {
                        val p = Parking.fromJson(it.getJSONObject(i))

                        val values = ContentValues().apply {
                            put(BaseColumns._ID, it.getJSONObject(i).getInt("id"))
                            put(ZONE, p.zone)
                            put(NAME, p.name)
                            put(FREE_SPOTS, p.freeSpots)
                            put(LATITUDE, p.location.latitude)
                            put(LONGITUDE, p.location.longitude)
                        }
                        db.insertWithOnConflict(PARKING_TABLE_NAME, "", values, SQLiteDatabase.CONFLICT_REPLACE)
                        Log.i("Zones", "Added parking $values")
                    }
                    getData()
                }
            }
        }
    }

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_DB_TABLE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $PARKING_TABLE_NAME")
            onCreate(db)
        }
    }

    companion object {
        const val ZONE = "zone"
        const val NAME = "name"
        const val FREE_SPOTS = "free_spots"
        const val LATITUDE = "lat"
        const val LONGITUDE = "lon"

        val PROJECTION = arrayOf(BaseColumns._ID, ZONE, NAME, FREE_SPOTS, LATITUDE, LONGITUDE)

        const val DATABASE_NAME = "Parking"
        const val PARKING_TABLE_NAME = "parking"
        const val DATABASE_VERSION = 1
        const val CREATE_DB_TABLE = " CREATE TABLE " + PARKING_TABLE_NAME +
                " (${BaseColumns._ID} INTEGER PRIMARY KEY NOT NULL, " +
                " $ZONE TEXT NOT NULL, " +
                " $NAME TEXT NOT NULL, " +
                " $FREE_SPOTS INT NOT NULL, " +
                " $LATITUDE NUMERIC NOT NULL, " +
                " $LONGITUDE NUMERIC NOT NULL);"
    }
}

fun Cursor.toParking() = Parking(
    this.getString(this.getColumnIndexOrThrow(ParkingDAO.ZONE)),
    this.getString(this.getColumnIndexOrThrow(ParkingDAO.NAME)),
    this.getInt(this.getColumnIndexOrThrow(ParkingDAO.FREE_SPOTS)),
    Location(LocationManager.PASSIVE_PROVIDER).also {
        it.latitude =
            this.getDouble(this.getColumnIndexOrThrow(ParkingDAO.LATITUDE))
        it.longitude =
            this.getDouble(this.getColumnIndexOrThrow(ParkingDAO.LONGITUDE))
    }
)

fun Cursor.toParkingList(): MutableList<Parking> {
    val res = mutableListOf<Parking>()
    this.moveToFirst()
    while (!this.isAfterLast) {
        res.add(this.toParking())
        this.moveToNext()
    }
    return res
}