package com.vishal.headsupnotificationprogress

import android.app.Application
import com.vishal.headsupnotificationprogress.di.prefModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class ProgressHeadsUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR) //Investigate: https://github.com/InsertKoinIO/koin/issues/847
            androidContext(this@ProgressHeadsUpApplication)
            modules(
                prefModule
            )
        }
    }
}