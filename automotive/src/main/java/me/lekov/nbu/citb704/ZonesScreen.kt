package me.lekov.nbu.citb704

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.provider.BaseColumns
import android.text.SpannableString
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.json.JSONObject
import kotlin.math.min
import androidx.car.app.model.CarColor

import androidx.car.app.model.PlaceMarker

import androidx.car.app.model.CarLocation

import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate

class ZonesScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver,
    LocationListener {

    init {
        lifecycle.addObserver(this)
    }

    companion object {
        val zones = JSONObject(
            "{\"calculated_at\":\"2021-12-14T18:54:09\",\"parking_sub_zones\":[{\"id\":1,\"zone\":\"Синя зона\",\"name\":\"Храм-пам. Ал. Невски\",\"free_places\":15,\"is_parking_free\":false,\"capacity\":1000,\"occupancy_level\":\"red\",\"location\":{\"lat\":42.69623761717473,\"lon\":23.3330684048453}},{\"id\":2,\"zone\":\"Синя зона\",\"name\":\"пл. Княз Александър I\",\"free_places\":0,\"is_parking_free\":false,\"capacity\":1000,\"occupancy_level\":\"red\",\"location\":{\"lat\":42.6961548219309,\"lon\":23.32656136577391}},{\"id\":3,\"zone\":\"Синя зона\",\"name\":\"бул. П. Евтимий\",\"free_places\":0,\"is_parking_free\":false,\"capacity\":1000,\"occupancy_level\":\"red\",\"location\":{\"lat\":42.68882897731066,\"lon\":23.32098773544396}},{\"id\":4,\"zone\":\"Синя зона\",\"name\":\"пл. Народно събрание\",\"free_places\":0,\"is_parking_free\":false,\"capacity\":1000,\"occupancy_level\":\"red\",\"location\":{\"lat\":42.69378722553881,\"lon\":23.33235225503783}},{\"id\":5,\"zone\":\"Синя зона\",\"name\":\"ул. Гурко — Тел. палата\",\"free_places\":18,\"is_parking_free\":false,\"capacity\":1000,\"occupancy_level\":\"red\",\"location\":{\"lat\":42.69407899083664,\"lon\":23.32426271265147}},{\"id\":6,\"zone\":\"Синя зона\",\"name\":\"бул. Ал. Стамболийски\",\"free_places\":9,\"is_parking_free\":false,\"capacity\":1000,\"occupancy_level\":\"red\",\"location\":{\"lat\":42.69724790737709,\"lon\":23.31806680982541}}]}"
        )
    }

    var currentLocation: Location? = null
    lateinit var db: SQLiteDatabase

    @SuppressLint("MissingPermission")
    override fun onCreate(owner: LifecycleOwner) {
        val locationManager = carContext
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        currentLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        Log.i("Zones", currentLocation.toString())
    }

    @SuppressLint("MissingPermission")
    override fun onStart(owner: LifecycleOwner) {
        val locationManager = carContext
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 50.0f, this)

        db = DatabaseHelper(carContext).readableDatabase
    }

    override fun onStop(owner: LifecycleOwner) {
        db.close()
    }

    override fun onGetTemplate(): Template {
        currentLocation?.let {
            val listBuilder = ItemList.Builder()

            val cursor = db.query(
                DatabaseHelper.PARKING_TABLE_NAME,
                DatabaseHelper.PROJECTION,
                null,
                null,
                null,
                null,
                BaseColumns._ID
            )

            val parkingData = Parking.cursorToParking(cursor)

            cursor.close()

            val zoneParking = zones.getJSONArray("parking_sub_zones")
            val items = min(parkingData.size - 1, 6)

            for (item in 0..items) {
                val parking = zoneParking.getJSONObject(item)
                val parkingLocation = Location(LocationManager.PASSIVE_PROVIDER).also {
                    it.latitude = parking.getJSONObject("location").getDouble("lat")
                    it.longitude = parking.getJSONObject("location").getDouble("lon")
                }
                val distanceMeters = parkingLocation.distanceTo(currentLocation)
                val distanceKm = distanceMeters / 1000
                val freeSpots = parking.getInt("free_places")
                val freeSpotsText = if (freeSpots > 0) "$freeSpots места" else "няма места"

                val address = SpannableString(
                    "  \u00b7 $freeSpotsText"
                )
                val distanceSpan = DistanceSpan.create(
                    Distance.create(
                        distanceKm.toDouble(), Distance.UNIT_KILOMETERS
                    )
                )
                address.setSpan(distanceSpan, 0, 1, SPAN_INCLUSIVE_INCLUSIVE)

                listBuilder.addItem(
                    Row.Builder()
                        .setOnClickListener {
                            onClickPlace(parking)
                        }
                        .setTitle(parking.getString("name"))
                        .addText(parking.getString("zone"))
                        .addText(address)
                        .setMetadata(
                            Metadata.Builder()
                                .setPlace(
                                    Place.Builder(CarLocation.create(parkingLocation))
                                        .setMarker(PlaceMarker.Builder().setColor(zoneToColor(parking.getString("zone"))).build())
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
            }

            val anchor = Place.Builder(CarLocation.create(it.latitude, it.longitude))
                .setMarker(PlaceMarker.Builder().setColor(CarColor.BLUE).build())
                .build()

            val builder: PlaceListMapTemplate.Builder = PlaceListMapTemplate.Builder()
                .setTitle("Буферни паркинги")
                .setAnchor(anchor)
                .setCurrentLocationEnabled(true)

            return builder.setItemList(listBuilder.build()).build()
        }

        val row = Row.Builder().setTitle(currentLocation.toString()).build()
        val pane = Pane.Builder().addRow(row).build()
        return PaneTemplate.Builder(pane)
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    private fun onClickPlace(parking: JSONObject) {
        screenManager.push(ZoneParkingDetails(carContext, parking))
    }

    private fun zoneToColor(zone: String?): CarColor {
        return when(zone) {
            "Синя зона" -> CarColor.BLUE
            "Зелена зона" -> CarColor.GREEN
            else -> CarColor.DEFAULT
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.i("Zones", location.toString())
        currentLocation = location
    }
}