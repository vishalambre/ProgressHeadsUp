package com.vishal.headsupnotificationprogress.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast

inline fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

inline fun <reified T : Service> Context.startService() {
    startService(Intent(this, T::class.java))
}

inline fun Context.getScreenWidth() = DisplayMetrics().also {
    getSystemService(WindowManager::class.java)?.defaultDisplay?.getMetrics(it)
}.widthPixels

