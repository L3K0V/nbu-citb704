package me.lekov.nbu.citb704

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import me.lekov.nbu.citb704.RequestPermissionScreen.LocationPermissionCheckCallback


class ParkingService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags.and(ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession() = object : Session() {

        override fun onCreateScreen(intent: Intent): Screen {
            if (carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && carContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                return ParkingScreen(carContext)
            }

            val screenManager = carContext.getCarService(ScreenManager::class.java)
            screenManager.push(ParkingScreen(carContext))

            return RequestPermissionScreen(
                carContext,
                object : LocationPermissionCheckCallback {
                    override fun onPermissionGranted() {
                        screenManager.pop()
                    }
                })
        }
    }
}