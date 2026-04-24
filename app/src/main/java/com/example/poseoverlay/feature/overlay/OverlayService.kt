package com.example.poseoverlay.feature.overlay

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.Region
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.poseoverlay.MainActivity
import com.example.poseoverlay.R
import com.example.poseoverlay.common.AppConstants
import com.example.poseoverlay.data.AppDatabase
import com.example.poseoverlay.data.ImageRepository
import com.example.poseoverlay.feature.overlay.ui.OverlayComposeView
import kotlinx.coroutines.*
import java.lang.reflect.Proxy

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



    // Dependencies
    private lateinit var repository: ImageRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        repository = ImageRepository(db.imageDao(), db.categoryDao())
        savedStateRegistryController.performRestore(null)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager


        createNotificationChannel()

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                OverlayComposeView(
                    state = overlayState,

                    onToggleTouch = { enabled -> toggleTouchability(enabled) },
                    onMinimize = { toggleMinimize(true) },
                    onMaximize = { toggleMinimize(false) },
                    onClose = { stopSelf() },
                    onOpenGallery = {
                        // Launch MainActivity to pick image
                        val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    },
                    onMove = { dx, dy ->
                        if (overlayState.isMinimized) {
                            moveWindow(dx, dy)
                        }
                    },
                    onCategorySelect = { cat -> loadImages(cat) },
                    onImageSelect = { uri ->
                        overlayState.imageUri = uri.uriString.toUri()
                        serviceScope.launch {
                            overlayState.noteText = uri.description
                        }
                    }
                )
            }
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        windowManager.addView(overlayView, params)

        setupTouchPassthrough()

        // Start observing categories
        serviceScope.launch {
            repository.getAllCategories().collect { cats ->
                overlayState.categories = listOf("All") + cats
            }
        }

        // Load initial images
        loadImages(AppConstants.Default_CATEGROY)
    }

    private var currentImageJob: Job? = null

    private fun loadImages(category: String) {
        currentImageJob?.cancel()
        currentImageJob = serviceScope.launch {
            val flow = if (category == AppConstants.Default_CATEGROY || category.isBlank()) repository.getAllImages() else repository.getImagesByCategory(category)
            flow.collect { list ->
                overlayState.images = list
            }
        }
        overlayState.selectedCategory = category
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (overlayState.isMinimized) {
            // Reposition bubble to avoid being off-screen after rotation
            val screenWidth = Resources.getSystem().displayMetrics.widthPixels
            val screenHeight = Resources.getSystem().displayMetrics.heightPixels

            // Simple logic: maintain relative position or clamp to new bounds
            params.x = params.x.coerceIn(0, screenWidth - 100)
            params.y = params.y.coerceIn(0, screenHeight - 100)
            windowManager.updateViewLayout(overlayView, params)
        } else {
            // If expanded, ensure dimensions match parent
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            windowManager.updateViewLayout(overlayView, params)
        }
    }

    private fun moveWindow(dx: Float, dy: Float) {
        params.x += dx.toInt()
        params.y += dy.toInt()
        windowManager.updateViewLayout(overlayView, params)
    }

    private fun toggleMinimize(minimize: Boolean) {
        overlayState.isMinimized = minimize

        // 為了支援全螢幕透傳，我們現在統一使用 MATCH_PARENT
        // 由 OnComputeInternalInsetsListener 決定哪個局部區域可點擊
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        windowManager.updateViewLayout(overlayView, params)
    }

    private fun setupTouchPassthrough() {
        try {
            val viewTreeObserver = overlayView.viewTreeObserver
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            val addMethod = viewTreeObserver.javaClass.getMethod("addOnComputeInternalInsetsListener", listenerClass)

            val proxy = Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "onComputeInternalInsets") {
                    val insets = args[0]
                    val setTouchableInsets = insets.javaClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType)
                    val touchableRegionField = insets.javaClass.getField("touchableRegion")
                    val region = touchableRegionField.get(insets) as Region

                    if (overlayState.isLocked) {
                        // 鎖定狀態：完全不接收任何觸摸，所有點擊穿透到背景
                        setTouchableInsets.invoke(insets, 3) // TOUCHABLE_INSETS_REGION
                        region.setEmpty()
                    } else {
                        // 非鎖定：只有有註冊區域的地方才接收觸摸
                        setTouchableInsets.invoke(insets, 3)
                        region.setEmpty()
                        overlayState.interactiveBounds.values.forEach { rect ->
                            region.union(rect)
                        }
                    }
                }
                null
            }
            addMethod.invoke(viewTreeObserver, proxy)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_UNLOCK -> {
                toggleTouchability(true)
                overlayState.isLocked = false
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, createNotification(includeUnlockAction = false))
            }

            ACTION_SET_IMAGE -> {
                val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
                if (uriString != null) {
                    overlayState.imageUri = Uri.parse(uriString)
                    // Auto-maximize when a new image is selected
                    if (overlayState.isMinimized) {
                        toggleMinimize(false)
                    }

                    serviceScope.launch {
                        val imageEntity = repository.getImageByUri(uriString)
                        overlayState.noteText = imageEntity?.description ?: ""
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Overlay Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
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
