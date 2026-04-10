package com.example.poseoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.poseoverlay.data.AppDatabase
import com.example.poseoverlay.data.ImageRepository
import com.example.poseoverlay.ui.gallery.*
import com.example.poseoverlay.ui.gallery.screens.AddImageScreen
import com.example.poseoverlay.ui.gallery.screens.ImageEditScreen
import com.example.poseoverlay.ui.gallery.GalleryScreen
import com.example.poseoverlay.ui.navigation.Screen
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
        repository = ImageRepository(database.imageDao(),database.categoryDao())
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
        startForegroundService(serviceIntent)
        moveTaskToBack(true)
    }
}

@Composable
fun MainAppScaffold(
    viewModelFactory: GalleryViewModelFactory?,
    onLaunchOverlay: ((String) -> Unit)?
) {
    val viewModel: GalleryViewModel = if (viewModelFactory != null) {
        viewModel(factory = viewModelFactory)
    } else {
        return
    }

    if (onLaunchOverlay == null) return

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Gallery.route
    ) {
        composable(Screen.Gallery.route) {
            GalleryScreen(
                viewModel = viewModel,
                onImageSelect = onLaunchOverlay,
                onEditImage = { img ->
                    val encodedUri = Uri.encode(img.uriString)
                    navController.navigate(
                        Screen.ImageEdit.createRoute(encodedUri, img.category, img.description)
                    )
                },
                onAddImage = { uri ->
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate(Screen.ImageAdd.createRoute(encodedUri))
                }
            )
        }

        composable(
            route = Screen.ImageEdit.route,
            arguments = listOf(
                navArgument(Screen.ImageEdit.argUri) { type = NavType.StringType },
                navArgument(Screen.ImageEdit.argCategory) { type = NavType.StringType; defaultValue = "" },
                navArgument(Screen.ImageEdit.argDescription) { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val decoded = Uri.decode(backStackEntry.arguments?.getString(Screen.ImageEdit.argUri))
            val uri = Uri.parse(decoded)
            val initialCategory = backStackEntry.arguments?.getString(Screen.ImageEdit.argCategory) ?: ""
            val initialDescription = backStackEntry.arguments?.getString(Screen.ImageEdit.argDescription) ?: ""
            val categories by viewModel.categories.collectAsState()

            ImageEditScreen(
                uri = uri,
                initialCategory = initialCategory,
                initialDescription = initialDescription,
                existingCategories = categories,
                onDismiss = { navController.popBackStack() },
                onConfirm = { cat, desc ->
                    viewModel.addImage(uri, cat, "", desc)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ImageAdd.route,
            arguments = listOf(navArgument(Screen.ImageAdd.argUri) { type = NavType.StringType })
        ) { backStackEntry ->
            val decoded = Uri.decode(backStackEntry.arguments?.getString(Screen.ImageAdd.argUri))
            val uri = decoded.toUri()
            val categories by viewModel.categories.collectAsState()

            AddImageScreen(
                uri = uri,
                categories = categories,
                onDismiss = { navController.popBackStack() },
                onConfirm = { cat, desc ->
                    viewModel.addImage(uri, cat, "", desc)
                    navController.popBackStack()
                }
            )
        }
    }
}


@Preview(showBackground = true, name = "Main App — Home Tab")
@Composable
fun MainAppScaffoldPreview() {
    PoseOverlayTheme {
        MainAppScaffold(null, null)
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
