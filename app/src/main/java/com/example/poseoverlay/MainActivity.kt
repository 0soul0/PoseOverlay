package com.example.poseoverlay

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poseoverlay.data.AppDatabase
import com.example.poseoverlay.data.ImageRepository
import com.example.poseoverlay.ui.gallery.*
import com.example.poseoverlay.ui.home.HomeScreen
import com.example.poseoverlay.ui.theme.PoseOverlayTheme

@OptIn(ExperimentalMaterial3Api::class) // For BadgedBox if used, or just suppression
class MainActivity : ComponentActivity() {

    // ... (Permissions code remains same, omitted for brevity, will include full file on rewrite if needed, but here replacing onCreate logic primarily)

    // Simple state to update UI based on permission status
    private var hasOverlayPermission by mutableStateOf(false)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermission()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private lateinit var database: AppDatabase
    private lateinit var repository: ImageRepository
    private lateinit var viewModelFactory: GalleryViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(applicationContext)
        repository = ImageRepository(database.imageDao())
        viewModelFactory = GalleryViewModelFactory(application, repository)

        checkPermission()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            PoseOverlayTheme {
                MainAppScaffold(viewModelFactory = viewModelFactory, onLaunchOverlay = {
                    if (hasOverlayPermission) {
                        launchOverlay(it)
                    } else {
                        requestOverlayPermission()
                    }
                })
            }
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

    private fun launchOverlay(uriString: String) {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SET_IMAGE
            putExtra(OverlayService.EXTRA_IMAGE_URI, uriString)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        moveTaskToBack(true)
    }
}

@Composable
fun MainAppScaffold(
    viewModelFactory: GalleryViewModelFactory,
    onLaunchOverlay: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val viewModel: GalleryViewModel = viewModel(factory = viewModelFactory)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Library") },
                    label = { Text("Library") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(onStartClick = { selectedTab = 1 }) // Enter goes to library
                1 -> GalleryScreen(
                    viewModel = viewModel,
                    onImageSelect = onLaunchOverlay
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppScaffoldPreview() {
    PoseOverlayTheme {
        // Mock factory for preview - won't actually work but allows UI preview
        MainAppScaffold(
            viewModelFactory = GalleryViewModelFactory(
                application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
                repository = ImageRepository(AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current).imageDao())
            ),
            onLaunchOverlay = {}
        )
    }
}


//@Composable
//fun PermissionScreen(onRequestPermission: () -> Unit) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text("Overlay Permission Required", style = MaterialTheme.typography.headlineMedium)
//        Spacer(modifier = Modifier.height(16.dp))
//        Text("To use PoseOverlay, please grant the permission to display over other apps.")
//        Spacer(modifier = Modifier.height(32.dp))
//        Button(onClick = onRequestPermission) {
//            Text("Grant Permission")
//        }
//    }
//}

//
//@Composable
//fun MainScreen(
//    hasPermission: Boolean,
//    onRequestPermission: () -> Unit,
//    onStartService: () -> Unit,
//    onStopService: () -> Unit,
//    onCloseApp: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(
//            text = "Pose Overlay",
//            style = MaterialTheme.typography.headlineMedium
//        )
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        if (!hasPermission) {
//            Text("Overlay permission is required to function.")
//            Spacer(modifier = Modifier.height(16.dp))
//            Button(onClick = onRequestPermission) {
//                Text("Grant Overlay Permission")
//            }
//        } else {
//            Button(onClick = onStartService) {
//                Text("Start Overlay")
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Button(onClick = onStopService) {
//                Text("Stop Overlay")
//            }
//        }
//
//        Spacer(modifier = Modifier.height(48.dp))
//
//        Button(
//            onClick = onCloseApp,
//            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
//                containerColor = MaterialTheme.colorScheme.error
//            )
//        ) {
//            Text("Close App")
//        }
//    }
//}
//
//@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
//@Composable
//fun MainScreenPreview() {
//    PoseOverlayTheme {
//        MainScreen(
//            hasPermission = false,
//            onRequestPermission = {},
//            onStartService = {},
//            onStopService = {},
//            onCloseApp = {}
//        )
//    }
//}
//
//@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
//@Composable
//fun MainScreenPermissionGrantedPreview() {
//    PoseOverlayTheme {
//        MainScreen(
//            hasPermission = true,
//            onRequestPermission = {},
//            onStartService = {},
//            onStopService = {},
//            onCloseApp = {}
//        )
//    }
//}