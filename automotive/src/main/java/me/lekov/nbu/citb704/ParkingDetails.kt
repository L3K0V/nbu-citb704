package me.lekov.nbu.citb704

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.CarToast.LENGTH_LONG
import androidx.car.app.HostException
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver


class ParkingDetails(carContext: CarContext, private val parking: Parking) :
    Screen(carContext), DefaultLifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    override fun onGetTemplate(): Template {
        val paneBuilder = Pane.Builder()
        paneBuilder.setImage(
            CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.parking)).setTint(
                CarColor.BLUE
            ).build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("Свободни места").addText("" + parking.freeSpotsText)
                .build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("Обслужва").addText(parking.zone).build()
        )

        paneBuilder.addAction(
            Action.Builder()
                .setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            R.drawable.directions
                        )
                    ).build()
                )
                .setTitle("Directions")
                .setOnClickListener(::onClickNavigate)
                .build()
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle(parking.name)
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun onClickNavigate() {
        val uri: Uri = Uri.parse("geo:${parking.location.latitude},${parking.location.longitude}")
        val intent = Intent(CarContext.ACTION_NAVIGATE, uri)

        try {
            carContext.startCarApp(intent)
        } catch (e: HostException) {
            CarToast.makeText(
                carContext,
                "Failure starting navigation",
                LENGTH_LONG
            )
                .show()
        }
    }
}