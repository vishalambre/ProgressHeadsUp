package com.vishal.headsupnotificationprogress.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class ProgressBarNotificationListenerService : NotificationListenerService() {

    var progressBarOverlayService: ProgressBarOverlayService? =
        null //Will be initialized once the service is connected

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            progressBarOverlayService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            progressBarOverlayService =
                (service as ProgressBarOverlayService.OverlayServiceBinder).getService()
        }

    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (Settings.canDrawOverlays(this) && progressBarOverlayService == null) {
            val intent = Intent(this, ProgressBarOverlayService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        progressBarOverlayService?.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        removeProgressbarOverlay(sbn)
    }

    private fun removeProgressbarOverlay(sbn: StatusBarNotification?) {
        progressBarOverlayService?.removeProgressBarOverlay(sbn?.id ?: -1)
    }
}