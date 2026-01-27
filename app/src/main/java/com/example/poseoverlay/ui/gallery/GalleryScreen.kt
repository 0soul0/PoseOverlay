package com.example.poseoverlay.ui.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.poseoverlay.data.*
import com.example.poseoverlay.ui.theme.PoseOverlayTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onImageSelect: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val images by viewModel.images.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var imageToDelete by remember { mutableStateOf<ImageEntity?>(null) }

    val addImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AddImageActivity.RESULT_CODE_SAVED) {
            result.data?.let { data ->
                val category = data.getStringExtra(AddImageActivity.EXTRA_CATEGORY) ?: "Uncategorized"
                val description = data.getStringExtra(AddImageActivity.EXTRA_DESCRIPTION) ?: ""
                val uriString = data.getStringExtra(AddImageActivity.EXTRA_IMAGE_URI)
                if (uriString != null) {
                    viewModel.addImage(Uri.parse(uriString), category, "", description)
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val predefined = listOf("Uncategorized", "Portrait", "Full Body", "Hand", "Selfie")
            val updatedCategories = (predefined + categories).distinct()

            val intent = android.content.Intent(context, AddImageActivity::class.java).apply {
                putExtra(AddImageActivity.EXTRA_IMAGE_URI, uri.toString())
                putStringArrayListExtra(AddImageActivity.EXTRA_CATEGORIES, ArrayList(updatedCategories))
            }
            addImageLauncher.launch(intent)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (imageToDelete != null) {
        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Delete Image") },
            text = { Text("Remove this image from library?") },
            confirmButton = {
                TextButton(
                    onClick = { imageToDelete?.let { viewModel.deleteImage(it) }; imageToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { imageToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Pose Library",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Select an overlay",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.alpha(0.7f)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { imagePicker.launch(arrayOf("image/*")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Photo")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingInfo ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInfo)
        ) {
            // Category Tabs
            val allCategories = listOf("All") + categories
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(allCategories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Image Grid
            if (images.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Image, // Or some other icon
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .alpha(0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No images yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        Text(
                            "Tap + to add photos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(images) { image ->
                        GalleryItem(
                            image = image,
                            onClick = { onImageSelect(image.uriString) },
                            onLongClick = { imageToDelete = image }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryItem(
    image: ImageEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = image.uriString,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Preview(showBackground = true)
@Composable
fun GalleryScreenPreview() {
    val viewModelFactory = GalleryViewModelFactory(
        application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
        repository = ImageRepository(AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current).imageDao()))

    PoseOverlayTheme {
        GalleryScreen(
            viewModel = viewModel(factory = viewModelFactory),
            onImageSelect = {}
        )
    }
}

