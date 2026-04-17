package com.example.poseoverlay.ui.gallery.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.ui.common.dialogs.DeleteImageDialog
import com.example.poseoverlay.ui.gallery.GalleryViewModel
import com.example.poseoverlay.ui.gallery.components.AddImageGridItem
import com.example.poseoverlay.ui.gallery.components.GalleryItem

@Composable
fun AlbumsScreen(modifier: Modifier = Modifier,
                 viewModel: GalleryViewModel,
                 imagePickerLauncher: () -> Unit) {

    val folderImages by viewModel.images.collectAsState()

    AlbumsContent(
        modifier = modifier,
        folderImages = folderImages,
        onNavigateBack = { viewModel.onNavigateBack() },
        onNavigateToDetail = { viewModel.onNavigateToDetail(it.uriString) },
        onNavigateToImageEdit = { viewModel.onNavigateToImageEdit(it.uriString) },
        onLaunchOverlay = { viewModel.onLaunchOverlay(it.uriString) },
        onDeleteImage = { viewModel.deleteImage(it) },
        imagePickerLauncher = imagePickerLauncher,
    )
}


@Composable
fun AlbumsContent(
    modifier: Modifier,
    folderImages: List<ImageEntity>,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (ImageEntity) -> Unit,
    onNavigateToImageEdit: (ImageEntity) -> Unit,
    onLaunchOverlay: (ImageEntity) -> Unit,
    onDeleteImage: (ImageEntity) -> Unit,
    imagePickerLauncher: () -> Unit
) {

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteSelectImage by remember { mutableStateOf<ImageEntity?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(folderImages.firstOrNull()?.uriString ?: "")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)))

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)) {
                    val categoryName = folderImages.firstOrNull()?.category ?: ""
                    Text(categoryName, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("${folderImages.size} Items", color = Color.White.copy(alpha = 0.8f))
                }

                IconButton(
                    onClick = { onNavigateBack() },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
                }
            }

            // Grid in folder
            val gridWithAdd = folderImages + ImageEntity("add", "", "", "", "")
            gridWithAdd.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    row.forEach { img ->
                        if (img.uriString == "add") {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()) {
                                AddImageGridItem(onClick = {imagePickerLauncher()})
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()) {
                                GalleryItem(
                                    img,
                                    onClick = {
                                        onNavigateToDetail(img)
                                    },
                                    onDelete = {
                                        showDeleteDialog = true
                                        deleteSelectImage = img
                                    },
                                    onEdit = { onNavigateToImageEdit(img) },
                                    onStart = { onLaunchOverlay(img) }
                                )
                            }
                        }
                    }
                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showDeleteDialog && deleteSelectImage != null) {
        DeleteImageDialog(
            selectedImage = deleteSelectImage!!,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDeleteImage(deleteSelectImage!!)
                deleteSelectImage = null
            }
        )
    }
}
