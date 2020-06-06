package com.vishal.headsupnotificationprogress

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vishal.headsupnotificationprogress.utils.showToast
import kotlinx.android.synthetic.main.section_permission.*
import kotlinx.android.synthetic.main.section_test_notification.* //Todo use viewbinding instead

class MainActivity : AppCompatActivity() {
    companion object {
        const val CHANNEL_NAME = "Test Notification"
        const val CHANNEL_DESCRIPTION = "Channel for test notification"
        const val NOTIFICATION_ID = 1
        const val MAX_PROGRESS = 100
        const val PROGRESS_BAR_UPDATE_DELAY_SECONDS = 1000L
    }

    private var currentProgress = 0
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            if (currentProgress < MAX_PROGRESS) {
                currentProgress += 25
                showNotification(currentProgress)
                handler.postDelayed(this, PROGRESS_BAR_UPDATE_DELAY_SECONDS)
            } else {
                currentProgress = 0
                removeNotification()
                handler.removeCallbacks(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initListeners()
    }

    private fun initListeners() {
        ll_permission_notification.setOnClickListener(handleNotificationPermission)
        ll_permission_overlay.setOnClickListener(handleOverlayPermission)
        ll_trigger_test_notification.setOnClickListener(triggerTestNotification)
    }

    private val handleNotificationPermission = { _: View ->
        when (NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)) {
            true -> showToast("Permission already granted")
            else -> startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private val handleOverlayPermission = { _: View ->
        when (Settings.canDrawOverlays(this)) {
            true -> showToast("Permission already granted")
            else -> startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private val triggerTestNotification = { _: View ->
        when (areAllPermissionsGranted()) {
            true -> {
                createNotificationChannel()
                showNotification(currentProgress)
                handler.postDelayed(runnable, PROGRESS_BAR_UPDATE_DELAY_SECONDS)
            }
            false -> showToast("Some permission is missing!")
        }
        Unit
    }

    private fun areAllPermissionsGranted() =
        NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName) && Settings.canDrawOverlays(this)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_NAME
            val descriptionText = CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_NAME, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(progress: Int = 0) {
        val notification = NotificationCompat.Builder(this, CHANNEL_NAME)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Test Notification!")
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)
            .build()
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, notification)
        }
    }

    private fun removeNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
        }
    }
}


/* Todo:
   Feature:
 - Centered Progress bar Feature
 - Fix using primary color by default logic
 - Animate progressbar removal
 - Ability to Set Custom color from color picker
 - Blacklist Apps
 - Make centered progress bar
 - Handling indeterminate progressbar

 Chore:
 - CleanUp and Refactoring ;)
*/