package me.lekov.nbu.citb704

import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

fun fetchJson(url: String): JSONObject? {
    var con: HttpsURLConnection? = null
    try {
        val u = URL(url)
        con = u.openConnection() as HttpsURLConnection
        con.connect()

        val br = BufferedReader(InputStreamReader(con.inputStream))
        val sb = StringBuilder()
        var line: String
        while (br.readLine().also { line = it } != null) {
            sb.append(
                """
                    $line
                    
                    """.trimIndent()
            )
        }
        br.close()
        return JSONObject(sb.toString())
    } catch (ex: Exception) {
        ex.printStackTrace()
    } finally {
        con?.disconnect()
    }
    return null
}

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_DB_TABLE)

        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val json = fetchJson("https://www.sofiatraffic.bg/bg/api/v1/parking/sub-zones/")
            handler.post {
                json?.getJSONArray("parking_sub_zones")?.let {
                    for (i in 0..it.length()) {
                        val p = Parking.fromJson(it.getJSONObject(i))

                        val values = ContentValues().apply {
                            put(_ID, it.getJSONObject(i).getInt("id"))
                            put(ZONE, p.zone)
                            put(NAME, p.name)
                            put(LATITUDE, p.location.latitude)
                            put(LONGITUDE, p.location.longitude)
                        }
                        db.insert(PARKING_TABLE_NAME, "", values)
                    }
                }
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $PARKING_TABLE_NAME")
        onCreate(db)
    }

    companion object {
        const val ZONE = "zone"
        const val NAME = "name"
        const val LATITUDE = "lat"
        const val LONGITUDE = "lon"

        val PROJECTION = arrayOf(BaseColumns._ID, ZONE, NAME, LATITUDE, LONGITUDE)

        const val DATABASE_NAME = "Parking"
        const val PARKING_TABLE_NAME = "parking"
        const val DATABASE_VERSION = 1
        const val CREATE_DB_TABLE = " CREATE TABLE " + PARKING_TABLE_NAME +
                " (${BaseColumns._ID} INTEGER PRIMARY KEY NOT NULL, " +
                " $ZONE TEXT NOT NULL, " +
                " $NAME TEXT NOT NULL, " +
                " $LATITUDE NUMERIC NOT NULL, " +
                " $LONGITUDE NUMERIC NOT NULL);"
    }
}