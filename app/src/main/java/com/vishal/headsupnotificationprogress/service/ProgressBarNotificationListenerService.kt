package com.vishal.headsupnotificationprogress.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class ProgressBarNotificationListenerService : NotificationListenerService() {

    companion object {
        val TAG = ProgressBarNotificationListenerService::class.simpleName
    }

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

    //Todo: refactor this method
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (Settings.canDrawOverlays(this) && progressBarOverlayService == null) {
            val intent = Intent(this, ProgressBarOverlayService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            val progressPair = sbn?.notification?.run { getProgressStatus(this) }
            if (progressPair?.second != null && progressPair.second != 0)
                progressBarOverlayService?.onNotificationPosted(progressPair, sbn.id, sbn.packageName)
            else
                removeNotification(sbn)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        removeNotification(sbn)
    }

    private fun getProgressStatus(notification: Notification): Pair<Int, Int> {
        val maxProgress: Int? = notification.extras?.get(Notification.EXTRA_PROGRESS_MAX) as Int?
        val currentProgress: Int? = notification.extras?.get(Notification.EXTRA_PROGRESS) as Int?
        return Pair(currentProgress ?: 0, maxProgress ?: 0)
    }

    private fun removeNotification(sbn: StatusBarNotification?) {
        progressBarOverlayService?.onNotificationRemoved(sbn?.id ?: -1)
    }

}