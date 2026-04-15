package com.example.poseoverlay.ui.gallery.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.ui.gallery.DetailActionItem
import com.example.poseoverlay.ui.gallery.GalleryViewModel


@Composable
fun ImageDetailScreen(modifier: Modifier = Modifier, viewModel: GalleryViewModel) {

    val selectedImage by viewModel.selectedImage.collectAsState()

    ImageDetailContent(
        modifier = modifier,
        selectedImage = selectedImage ?: ImageEntity("", "", "", "", ""),
        onNavigateBack = { viewModel.onNavigateBack() }
    )
}


@Composable
fun ImageDetailContent(modifier: Modifier, selectedImage: ImageEntity, onNavigateBack: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Image Preview (Fill)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(selectedImage.uriString)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onNavigateBack() }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
//            IconButton(onClick = { showInfoDialog = true }) {
//                Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.White)
//            }
        }

        // Bottom Actions (Based on image1 reference)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = Color.Black.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailActionItem(Icons.Outlined.Edit, "Edit") {
//                    onEditImage(img) // 真正執行 Activity 跳轉
//                    selectedImageForDetail = null
                }
                DetailActionItem(Icons.Outlined.Delete, "Delete") {
//                    imageToDelete = img
//                    selectedImageForDetail = null
                }
                DetailActionItem(Icons.Outlined.PlayArrow, "Start") {
//                    onImageSelect(img.uriString)
//                    selectedImageForDetail = null
                }
            }
        }
    }
}