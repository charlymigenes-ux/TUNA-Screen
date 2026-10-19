package com.nopal.tunascreen

import android.app.Application
import com.nopal.tunascreen.core.notifications.NotificationCenter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TunaScreenApp : Application() {
    @Inject lateinit var notificationCenter: NotificationCenter

    override fun onCreate() {
        super.onCreate()
        notificationCenter.start()
    }
}
