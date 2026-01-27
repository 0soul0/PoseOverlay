package com.example.poseoverlay

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.*

class OverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val mViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = mViewModelStore

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView
    private lateinit var params: WindowManager.LayoutParams
    
    // State holder
    private val overlayState = OverlayState()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        createNotificationChannel()
        startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            
            setContent {
                OverlayComposeView(
                    state = overlayState,
                    onToggleTouch = { enabled -> toggleTouchability(enabled) },
                    onMinimize = { toggleMinimize(true) },
                    onClose = { stopSelf() },
                    onPickImage = {
                        // Launch MainActivity to pick image
                        val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                            action = ACTION_PICK_IMAGE
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }
                )
            }
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(overlayView, params)
    }

    private fun toggleMinimize(minimize: Boolean) {
        overlayState.isMinimized = minimize
        if (minimize) {
            // Shrink window to allow interaction behind
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.gravity = Gravity.TOP or Gravity.START
        } else {
            // Restore full screen
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.gravity = Gravity.NO_GRAVITY
        }
        windowManager.updateViewLayout(overlayView, params)
    }
    
    /**
     * Toggles touchability.
     */
    private fun toggleTouchability(enabled: Boolean) {
        if (enabled) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windowManager.updateViewLayout(overlayView, params)
        
        if (!enabled) {
            val notification = createNotification(includeUnlockAction = true)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_UNLOCK -> {
                toggleTouchability(true)
                overlayState.isLocked = false
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, createNotification(includeUnlockAction = false))
            }
            ACTION_SET_IMAGE -> {
                val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
                if (uriString != null) {
                    overlayState.imageUri = Uri.parse(uriString)
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(includeUnlockAction: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PoseOverlay Running")
            .setContentText("Tap to open app settings")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (includeUnlockAction) {
            val unlockIntent = Intent(this, OverlayService::class.java).apply {
                action = ACTION_UNLOCK
            }
            val unlockPendingIntent = PendingIntent.getService(
                this, 1, unlockIntent, PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_launcher_foreground, "Unlock Touch", unlockPendingIntent)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    companion object {
        const val CHANNEL_ID = "overlay_channel"
        const val ACTION_UNLOCK = "com.example.poseoverlay.ACTION_UNLOCK"
        const val ACTION_PICK_IMAGE = "com.example.poseoverlay.ACTION_PICK_IMAGE"
        const val ACTION_SET_IMAGE = "com.example.poseoverlay.ACTION_SET_IMAGE"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
