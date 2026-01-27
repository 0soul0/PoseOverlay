package com.example.poseoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.poseoverlay.ui.theme.PoseOverlayTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    
    // Simple state to update UI based on permission status
    private var hasOverlayPermission by mutableStateOf(false)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermission()
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
         // Notification permission granted or denied. Service will try to start anyway but might be limited.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermission()
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Handle picking image if started via intent
        if (intent?.action == OverlayService.ACTION_PICK_IMAGE) {
            imagePickerLauncher.launch("image/*")
        }

        setContent {
            PoseOverlayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        hasPermission = hasOverlayPermission,
                        onRequestPermission = { requestOverlayPermission() },
                        onStartService = { startOverlayService() },
                        onStopService = { stopOverlayService() },
                        onCloseApp = { finishAndRemoveTask() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == OverlayService.ACTION_PICK_IMAGE) {
             imagePickerLauncher.launch("image/*")
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> // Use full type to avoid ambiguity if needed, though Uri? is inferred
        uri?.let {
             // Pass URI back to Service
             val serviceIntent = Intent(this, OverlayService::class.java).apply {
                 action = OverlayService.ACTION_SET_IMAGE
                 putExtra(OverlayService.EXTRA_IMAGE_URI, it.toString())
             }
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 startForegroundService(serviceIntent)
             } else {
                 startService(serviceIntent)
             }
        }
        // If launched via intent, move task to back or finish to return to overlay
        if (intent?.action == OverlayService.ACTION_PICK_IMAGE) {
             moveTaskToBack(true)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun checkPermission() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun startOverlayService() {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, OverlayService::class.java)
            startForegroundService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
    }
}

@Composable
fun MainScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onCloseApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Pose Overlay",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        if (!hasPermission) {
            Text("Overlay permission is required to function.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Overlay Permission")
            }
        } else {
            Button(onClick = onStartService) {
                Text("Start Overlay")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onStopService) {
                Text("Stop Overlay")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onCloseApp,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Close App")
        }
    }
}