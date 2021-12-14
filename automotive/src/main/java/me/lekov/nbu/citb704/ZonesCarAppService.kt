package me.lekov.nbu.citb704

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class ZonesCarAppService: CarAppService() {
    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags.and(ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession() = object: Session() {
        override fun onCreateScreen(intent: Intent) = ZonesScreen(carContext)
    }
}