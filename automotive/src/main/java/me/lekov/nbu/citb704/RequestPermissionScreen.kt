package me.lekov.nbu.citb704

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*


class RequestPermissionScreen(
    carContext: CarContext, var mLocationPermissionCheckCallback: LocationPermissionCheckCallback
) : Screen(carContext) {

    interface LocationPermissionCheckCallback {
        fun onPermissionGranted()
    }

    override fun onGetTemplate(): Template {
        val permissions = listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
        val message = "This app needs access to location in order to show the map around you"
        val listener: OnClickListener = ParkedOnlyOnClickListener.create {
            carContext.requestPermissions(
                permissions
            ) { approved: List<String>, _: List<String> ->
                if (approved.isNotEmpty()) {
                    mLocationPermissionCheckCallback.onPermissionGranted()
                    finish()
                }
            }
        }
        val action: Action = Action.Builder()
            .setTitle("Grant Access")
            .setBackgroundColor(CarColor.GREEN)
            .setOnClickListener(listener)
            .build()
        return MessageTemplate.Builder(message).addAction(action).setHeaderAction(
            Action.APP_ICON
        ).build()
    }
}