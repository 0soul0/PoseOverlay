package com.example.poseoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.poseoverlay.data.AppDatabase
import com.example.poseoverlay.data.ImageRepository
import com.example.poseoverlay.ui.common.AppConstants
import com.example.poseoverlay.ui.gallery.*
import com.example.poseoverlay.ui.gallery.screens.*
import com.example.poseoverlay.ui.navigation.NavigationEvent
import com.example.poseoverlay.ui.navigation.Screen
import com.example.poseoverlay.ui.theme.PoseOverlayTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class) // For BadgedBox if used, or just suppression
class MainActivity : ComponentActivity() {

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
        repository = ImageRepository(database.imageDao(), database.categoryDao())
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

    val imagePickerLauncher = rememberImagePickerLauncher(onImageSelected = { uri ->
        val encodedUri = Uri.encode(uri.toString())
        navController.navigate(Screen.ImageAdd.createRoute(encodedUri))
    })

    BindLaunchOverlayFlow(viewModel, onLaunchOverlay)
    AppNavigation(viewModel, navController)

    NavHost(
        navController = navController,
        startDestination = Screen.Gallery.route
    ) {
        composable(Screen.Gallery.route) {
            LaunchedEffect(AppConstants.Default_CATEGROY) {
                viewModel.selectCategory(AppConstants.Default_CATEGROY)
            }
            GalleryScreen(
                viewModel = viewModel,
                imagePickerLauncher = { imagePickerLauncher.launch(arrayOf("image/*")) },
                onImageSelect = onLaunchOverlay,
            )
        }

        composable(
            route = Screen.ImageEdit.route,
            arguments = listOf(
                navArgument(Screen.ImageEdit.argUri) { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString(Screen.ImageEdit.argUri) ?: ""
            val decodedUri = Uri.decode(uri)

            LaunchedEffect(decodedUri) {
                viewModel.findSelectImage(decodedUri)
            }

            ImageEditOrAddScreen(viewModel = viewModel)
        }

        composable(
            route = Screen.ImageAdd.route,
            arguments = listOf(navArgument(Screen.ImageAdd.argUri) { type = NavType.StringType })
        ) { backStackEntry ->
            val decoded = Uri.decode(backStackEntry.arguments?.getString(Screen.ImageAdd.argUri))
            val uri = decoded.toUri()


            ImageEditOrAddScreen(
                viewModel = viewModel,
                uri = uri,
            )
        }

        composable(
            route = Screen.ImageDetail.route,
            arguments = listOf(navArgument(Screen.ImageDetail.argUrlString) { type = NavType.StringType })
        ) { backStackEntry ->

            val urlString = backStackEntry.arguments?.getString(Screen.ImageDetail.argUrlString) ?: ""
            val decodedUri = Uri.decode(urlString)

            LaunchedEffect(decodedUri) {
                viewModel.findSelectImage(decodedUri)
            }

            ImageDetailScreen(viewModel = viewModel)

        }

        composable(
            route = Screen.Albums.route,
            arguments = listOf(navArgument(Screen.Albums.argCategory) { type = NavType.StringType })
        ) { backStackEntry ->

            val category = backStackEntry.arguments?.getString(Screen.Albums.argCategory) ?: ""

            LaunchedEffect(category) {
                viewModel.selectCategory(category)
            }

            AlbumsScreen(viewModel = viewModel, imagePickerLauncher = { imagePickerLauncher.launch(arrayOf("image/*")) })

        }
    }
}


@Composable
private fun rememberImagePickerLauncher(
    onImageSelected: (Uri) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    val context = LocalContext.current

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { contentUri ->
            try {
                // 處理檔案 IO
                val tempFile = File(context.cacheDir, "img_preview_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(contentUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                onImageSelected(Uri.fromFile(tempFile))
            } catch (e: Exception) {
                Log.e("Gallery", "Failed to copy to cache", e)
                onImageSelected(contentUri)
            }
        }
    }
}

@Composable
private fun BindLaunchOverlayFlow(viewModel: GalleryViewModel, onLaunchOverlay: ((String) -> Unit)?) {
    LaunchedEffect(viewModel) {
        viewModel.onLaunchOverlay.collect {
            if (it.isEmpty()) return@collect
            onLaunchOverlay?.invoke(it)
        }
    }
}

@Composable
private fun AppNavigation(viewModel: GalleryViewModel, navController: NavHostController) {

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToImageEdit -> {
                    navController.navigate(Screen.ImageEdit.createRoute(event.uriString))
                }

                is NavigationEvent.NavigateBack -> {
                    navController.popBackStack()
                }

                is NavigationEvent.NavigateToDetail -> {
                    navController.navigate(Screen.ImageDetail.createRoute(event.uriString))
                }

                is NavigationEvent.NavigateToAlbums -> {
                    navController.navigate(Screen.Albums.createRoute(event.category))
                }
            }
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
