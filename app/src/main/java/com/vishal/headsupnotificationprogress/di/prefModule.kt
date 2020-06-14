package com.vishal.headsupnotificationprogress.di

import com.vishal.headsupnotificationprogress.constants.PrefConstants
import com.vishal.headsupnotificationprogress.prefserver.PrefServer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** @author Vishal Ambre */

val prefModule = module {
    single {
        PrefServer.from<Int>(
            PrefConstants.PREF_FILE_NAME,
            PrefConstants.PROGRESS_BAR_HEIGHT,
            androidContext(),
            2
        )
    }
}