package com.example.poseoverlay.feature.gallery.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.common.dialogs.DeleteImageDialog
import com.example.poseoverlay.common.dialogs.InfoImageDialog
import com.example.poseoverlay.feature.gallery.GalleryViewModel


@Composable
fun ImageDetailScreen(modifier: Modifier = Modifier, viewModel: GalleryViewModel) {

    val selectedImage by viewModel.selectedImage.collectAsState()

    ImageDetailContent(
        modifier = modifier,
        selectedImage = selectedImage ?: ImageEntity("", "", "", "", ""),
        onNavigateBack = { viewModel.onNavigateBack() },
        onNavigateToImageEdit = { viewModel.onNavigateToImageEdit(it.uriString) },
        onLaunchOverlay = { viewModel.onLaunchOverlay(it.uriString) },
        deleteImage = { viewModel.deleteImage(it) },

        )
}


@Composable
fun ImageDetailContent(
    modifier: Modifier,
    selectedImage: ImageEntity,
    onNavigateBack: () -> Unit,
    deleteImage: (ImageEntity) -> Unit,
    onNavigateToImageEdit: (ImageEntity) -> Unit,
    onLaunchOverlay: (ImageEntity) -> Unit
) {


    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

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
            IconButton(onClick = { showInfoDialog = true }) {
                Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.White)
            }
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
                BottomActionItem(Icons.Outlined.Edit, "Edit") {
                    onNavigateToImageEdit(selectedImage)
                }
                BottomActionItem(Icons.Outlined.Delete, "Delete") {
                    showDeleteDialog = true

                }
                BottomActionItem(Icons.Outlined.PlayArrow, "Start") {
                    onLaunchOverlay(selectedImage)
                }
            }
        }
    }


    if (showInfoDialog) {
        InfoImageDialog(
            selectedImage = selectedImage,
            onDismiss = { showInfoDialog = false }
        )
    }


    if (showDeleteDialog) {
        DeleteImageDialog(
            selectedImage = selectedImage,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                deleteImage(selectedImage)
                onNavigateBack()
            }
        )
    }
}


@Composable
fun BottomActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}
