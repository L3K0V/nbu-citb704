package me.lekov.nbu.citb704

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.text.SpannableString
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.min

class ParkingScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver,
    LocationListener {

    var currentLocation: Location? = null
    private var dao: ParkingDAO = ParkingDAO(carContext)
    private var template: Template? = null

    init {
        lifecycle.addObserver(dao)
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (carContext.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && carContext.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            finish()
            return
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStart(owner: LifecycleOwner) {
        val locationManager = carContext
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager

        currentLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 50.0f, this)

        observeParking()
    }

    override fun onGetTemplate(): Template {
        return template ?: loadingTemplate()
    }

    private fun observeParking() {
        dao.list.observe(this, {

            if (currentLocation == null) {
                loadingTemplate()
                invalidate()
                return@observe
            }

            val listBuilder = ItemList.Builder()
            val items = min(it.size - 1, 6)

            for (item in 0..items) {
                val parking = it[item]
                val distanceMeters = parking.location.distanceTo(currentLocation)
                val distanceKm = distanceMeters / 1000

                val address = SpannableString(
                    "  \u00b7 ${parking.zone}"
                )
                val distanceSpan = DistanceSpan.create(
                    Distance.create(
                        distanceKm.toDouble(), Distance.UNIT_KILOMETERS
                    )
                )
                address.setSpan(distanceSpan, 0, 1, SPAN_INCLUSIVE_INCLUSIVE)

                listBuilder.addItem(
                    Row.Builder()
                        .setOnClickListener { onClickPlace(parking) }
                        .setTitle(parking.name)
                        .addText(parking.freeSpotsText)
                        .addText(address)
                        .setMetadata(
                            Metadata.Builder().setPlace(
                                Place.Builder(CarLocation.create(parking.location))
                                    .setMarker(
                                        PlaceMarker.Builder()
                                            .setColor(zoneToColor(parking.zone)).build()
                                    ).build()
                            ).build()
                        ).build()
                )

                val anchor = Place.Builder(
                    CarLocation.create(
                        currentLocation!!.latitude,
                        currentLocation!!.longitude
                    )
                ).setMarker(PlaceMarker.Builder().setColor(CarColor.BLUE).build()).build()

                val builder: PlaceListMapTemplate.Builder = PlaceListMapTemplate.Builder()
                    .setTitle("Буферни паркинги")
                    .setAnchor(anchor)
                    .setCurrentLocationEnabled(true)
                    .setActionStrip(
                        ActionStrip.Builder().addAction(
                            Action.Builder().setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(
                                        carContext,
                                        R.drawable.refresh_circle
                                    )
                                ).build()
                            ).setTitle("Refresh").setOnClickListener {
                                dao.refresh()
                            }.build()
                        ).build()
                    )

                template = builder.setItemList(listBuilder.build()).build()

                invalidate()
            }
        })
    }

    private fun loadingTemplate(): Template {
        val pane = Pane.Builder().setLoading(true).build()
        return PaneTemplate.Builder(pane)
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    private fun onClickPlace(parking: Parking) {
        screenManager.push(ParkingDetails(carContext, parking))
    }

    private fun zoneToColor(zone: String?): CarColor {
        return when (zone) {
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