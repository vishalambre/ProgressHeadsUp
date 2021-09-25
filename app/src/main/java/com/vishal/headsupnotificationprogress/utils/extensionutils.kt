package com.vishal.headsupnotificationprogress.utils

import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast

fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

inline fun <reified T : Service> Context.startService() {
    startService(Intent(this, T::class.java))
}

fun Context.getScreenWidth() = DisplayMetrics().also {
    getSystemService(WindowManager::class.java)?.defaultDisplay?.getMetrics(it)
}.widthPixels

fun Context.navigateToPlayStore() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: ActivityNotFoundException) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
        )
    }
}