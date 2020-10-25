package com.vishal.headsupnotificationprogress.service

import android.animation.ValueAnimator
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import com.vishal.headsupnotificationprogress.R
import com.vishal.headsupnotificationprogress.prefserver.PrefServer
import com.vishal.headsupnotificationprogress.utils.getScreenWidth
import org.koin.android.ext.android.inject
import java.lang.ref.WeakReference

class ProgressBarOverlayService : Service() {

    private val progressBarHeightPref: PrefServer<Int> by inject()
    var progressBarHeight = progressBarHeightPref.get()
    private lateinit var rootLinearLayout: LinearLayout
    private val progressBarMap = hashMapOf<Int, WeakReference<View>>()
    private val onProgressBarHeightChangedListener = object : PrefServer.PrefChangeListener<Int> {
        override fun onPrefChanged(newPrefValue: Int) {
            progressBarHeight = newPrefValue
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return OverlayServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        progressBarHeightPref.addPrefChangeListener(onProgressBarHeightChangedListener)
        rootLinearLayout = addRootView()
    }

    override fun onDestroy() {
        progressBarHeightPref.removePrefChangeListener(onProgressBarHeightChangedListener)
        removeRootView()
        super.onDestroy()
    }

    fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isProgressBarNotification(sbn)) {
            onProgressBarNotificationPosted(sbn)
        }
    }

    private fun onProgressBarNotificationPosted(sbn: StatusBarNotification) =
        with(sbn.notification?.run { getProgressStatus(this) }) {
            this?.also {
                if (it.second == 0) removeProgressBarOverlay(sbn.id)
                else addProgressBar(it, sbn.id, sbn.notification.color)
            }
        }

    private fun addProgressBar(
        progressPair: Pair<Int, Int>,
        notificationId: Int,
        @ColorInt color: Int
    ) {
        if (progressBarMap[notificationId]?.get() == null) {
            progressBarMap[notificationId] = WeakReference(addProgressBarView(color))
        }
        animateViewWidth(
            progressBarMap[notificationId]?.get()!!,
            progressPair.first,
            progressPair.second
        )
    }

    private fun addRootView(): LinearLayout {
        val windowManager = getSystemService(WindowManager::class.java)
        val rootLinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val layoutParams = getLayoutParams()
        layoutParams.gravity = Gravity.LEFT or Gravity.TOP
        windowManager?.addView(rootLinearLayout, layoutParams)
        return rootLinearLayout
    }

    private fun removeRootView() {
        val windowManager = getSystemService(WindowManager::class.java)
        windowManager?.removeView(rootLinearLayout)
    }

    private fun addProgressBarView(@ColorInt progressBarColor: Int?): View {
        val finalColor = if (progressBarColor == null || progressBarColor == 0)
            getColor(R.color.colorWhite)
        else progressBarColor

        val view = getViewAsProgressBar(finalColor)
        rootLinearLayout.addView(view)
        return view
    }

    private fun removeProgressBarView(view: View?) {
        // The view shows itself even after it is removed when a new view is added; a temporary workaround for now
        view?.visibility = View.INVISIBLE
        rootLinearLayout.post { rootLinearLayout.removeView(view) }
    }

    private fun getViewAsProgressBar(@ColorInt color: Int): View {
        val progressView = View(this)
        val layoutParams = LinearLayout.LayoutParams(
            0,
            progressBarHeight
        )
        progressView.setBackgroundColor(color)
        progressView.layoutParams = layoutParams
        return progressView
    }

    //Todo: Refactor
    private fun animateViewWidth(view: View, currProgress: Int, maxProgress: Int) {
        ValueAnimator.ofInt(view.width, getInterpolatedViewWidth(currProgress, maxProgress)).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }.addUpdateListener {
            val layoutParams = view.layoutParams
            layoutParams.width = it.animatedValue as Int
            view.layoutParams = layoutParams
        }
    }

    private fun getInterpolatedViewWidth(currProgress: Int, maxProgress: Int) =
        (currProgress * getScreenWidth()) / maxProgress

    fun removeProgressBarOverlay(notificationId: Int) {
        if (progressBarMap[notificationId]?.get() != null) {
            removeProgressBarView(progressBarMap[notificationId]?.get())
            progressBarMap.remove(notificationId)
        }
    }

    private fun getLayoutType(): Int {
        return when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            true -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else -> WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun getLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun isProgressBarNotification(sbn: StatusBarNotification?): Boolean {
        if (sbn == null) return false
        return sbn.notification.extras.get(Notification.EXTRA_PROGRESS_MAX) != null &&
                sbn.notification.extras.get(Notification.EXTRA_PROGRESS) != null
    }

    private fun getProgressStatus(notification: Notification): Pair<Int, Int> {
        val maxProgress: Int? = notification.extras?.getInt(Notification.EXTRA_PROGRESS_MAX)
        val currentProgress: Int? = notification.extras?.getInt(Notification.EXTRA_PROGRESS)
        return Pair(currentProgress ?: 0, maxProgress ?: 0)
    }

    inner class OverlayServiceBinder : Binder() {
        fun getService() = this@ProgressBarOverlayService
    }
}