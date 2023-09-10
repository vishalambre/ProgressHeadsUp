package com.vishal.headsupnotificationprogress.ui

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vishal.headsupnotificationprogress.BuildConfig
import com.vishal.headsupnotificationprogress.R
import com.vishal.headsupnotificationprogress.databinding.ActivityMainBinding
import com.vishal.headsupnotificationprogress.prefserver.PrefServer
import com.vishal.headsupnotificationprogress.utils.navigateToPlayStore
import com.vishal.headsupnotificationprogress.utils.showToast
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val toolbarBinding
        get() = binding.toolbar
    private val permissionsBinding
        get() = binding.sectionMainBody.sectionPermission

    private val customizeHeightBinding
        get() = binding.sectionMainBody.sectionCustomization.sectionCustomizeHeight

    private val heightChangeSeekBar
        get() = binding.sectionMainBody.sectionCustomization.sectionCustomizeHeight.sbProgressBarHeight

    private val triggerTestNotificationBtn
        get() = binding.sectionMainBody.triggerTestNotification.llTriggerTestNotification

    private val progressBarHeightPref: PrefServer<Int> by inject()
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        initToolbar()
        initListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressBarHeightPref.removePrefChangeListener(handlePbHeightPrefChanged)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            showNotification()
        }
    }

    private fun initViews() {
        customizeHeightBinding.tvProgressBarHeight.text = getFormattedProgressHeight(progressBarHeightPref.get())
        customizeHeightBinding.sbProgressBarHeight.progress = progressBarHeightPref.get()
    }

    private fun initToolbar() = toolbarBinding.tlMain.run {
        inflateMenu(R.menu.main)
        setOnMenuItemClickListener(::handleMenuItemClick)
    }

    private fun initListeners() {
        permissionsBinding.llPermissionNotification.setOnClickListener(handleNotificationPermission)
        permissionsBinding.llPermissionOverlay.setOnClickListener(handleOverlayPermission)
        triggerTestNotificationBtn.setOnClickListener(triggerTestNotification)
        progressBarHeightPref.addPrefChangeListener(handlePbHeightPrefChanged)
        heightChangeSeekBar.setOnSeekBarChangeListener(handleProgressBarHeightChanged)
    }

    private fun handleMenuItemClick(item: MenuItem) = when (item.itemId) {
        R.id.about -> {
            showAboutDialog()
            true
        }
        R.id.rate_app -> {
            navigateToPlayStore()
            true
        }
        else -> false
    }

    private fun showAboutDialog() {
        val dialog = AlertDialog.Builder(this, R.style.AlertDialog)
            .setTitle("${getString(R.string.app_name)} v${BuildConfig.VERSION_NAME}")
            .setMessage("Developed by Vishal Ambre")
            .setNegativeButton("Dismiss", null)
            .show()

        // Todo: refactor
        dialog.findViewById<TextView>(android.R.id.message)?.textSize = 14F
    }

    private val handleNotificationPermission = { _: View ->
        when (NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)) {
            true -> showToast("Permission already granted")
            else -> startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private val handleProgressBarHeightChanged = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            progressBarHeightPref.put(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private val handleOverlayPermission = { _: View ->
        when (Settings.canDrawOverlays(this)) {
            true -> showToast("Permission already granted")
            else -> startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                ),
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
            false -> showToast("Please allow all permissions!")
        }
        Unit
    }

    private val handlePbHeightPrefChanged = object : PrefServer.PrefChangeListener<Int> {
        override fun onPrefChanged(newPrefValue: Int) {
            customizeHeightBinding.tvProgressBarHeight.text = getFormattedProgressHeight(newPrefValue)
        }
    }

    private fun getFormattedProgressHeight(height: Int) = "${height}px"

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_NAME)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Test Notification!")
            .setProgress(MAX_PROGRESS, progress, false)
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

    companion object {
        private const val CHANNEL_NAME = "Test Notification"
        private const val CHANNEL_DESCRIPTION = "Channel for test notification"
        private const val NOTIFICATION_ID = 1
        private const val MAX_PROGRESS = 100
        private const val PROGRESS_BAR_UPDATE_DELAY_SECONDS = 1000L
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}

/* Todo:
   Feature:
 - Alert Dialog message TextSize
 - Animate progressbar removal
 - Ability to Set Custom color from color picker
 - Blacklist Apps
 - Make centered progress bar
 - Handling indeterminate progressbar
 - Centered Progress bar Feature
 Chore:
 - CleanUp and Refactoring ;)
*/
