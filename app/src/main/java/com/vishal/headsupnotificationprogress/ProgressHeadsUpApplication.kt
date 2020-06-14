package com.vishal.headsupnotificationprogress

import android.app.Application
import com.vishal.headsupnotificationprogress.di.prefModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ProgressHeadsUpApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ProgressHeadsUpApplication)
            modules(prefModule)
        }
    }
}