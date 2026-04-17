package com.example.poseoverlay.ui.gallery

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.ui.common.AppConstants
import com.example.poseoverlay.ui.common.dialogs.DeleteImageDialog
import com.example.poseoverlay.ui.gallery.components.AddImageGridItem
import com.example.poseoverlay.ui.gallery.components.GalleryItem
import com.example.poseoverlay.ui.theme.PoseOverlayTheme

// ═══════════════════════════════════════════════════════════════════════════
//  Stateful wrapper — owns ViewModel + Activity launchers
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    imagePickerLauncher: () -> Unit,
    onImageSelect: (String) -> Unit
) {
    val images by viewModel.images.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filterCategories = categories.filter { it != AppConstants.Default_CATEGROY }

    GalleryContent(
        images = images,
        categories = filterCategories,
        onImageSelect = onImageSelect,
        imagePickerLauncher = imagePickerLauncher,
        onDeleteImage = { viewModel.deleteImage(it) },
        onNavigateToDetail = { viewModel.onNavigateToDetail(it.uriString) },
        onNavigateToAlbums = { viewModel.onNavigateToAlbums(it) },
        onNavigateToImageEdit = { viewModel.onNavigateToImageEdit(it.uriString) },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryContent(
    images: List<ImageEntity>,
    categories: List<String>,
    onImageSelect: (String) -> Unit,
    imagePickerLauncher: () -> Unit,
    onDeleteImage: (ImageEntity) -> Unit,
    onNavigateToDetail: (ImageEntity) -> Unit,
    onNavigateToAlbums: (String) -> Unit,
    onNavigateToImageEdit: (ImageEntity) -> Unit,
) {

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteSelectImage by remember { mutableStateOf<ImageEntity?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val safeCategories = categories.filter { it.isNotBlank() }

    val gridItems = images + ImageEntity("add", "", "", "", "")
    val gridState = rememberLazyGridState()
    //滑動到最下面
    LaunchedEffect(gridItems.size) {
        if (gridItems.isNotEmpty()) {
            gridState.animateScrollToItem(index = gridItems.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gallery",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.White
    ) { paddingInfo ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(4), // 設定為 3 欄
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInfo),
            state = gridState,
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Albums 標題
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Albums",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            // 2. 橫向 Albums 列表
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    modifier = Modifier.height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(safeCategories) { cat ->
                        val catImages = images.filter { it.category == cat }
                        AlbumCard(
                            title = cat,
                            count = catImages.size,
                            previewUri = catImages.firstOrNull()?.uriString,
                            onClick = { onNavigateToAlbums(cat) }
                        )
                    }
                }
            }

            // 3. Photos 標題
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Photos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            // 4. 照片網格
            items(gridItems, key = { it.uriString }) { img ->
                Box(modifier = Modifier.aspectRatio(1f)) {
                    if (img.uriString == "add") {
                        AddImageGridItem(onClick = imagePickerLauncher)
                    } else {
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
                            onStart = { onImageSelect(img.uriString) }
                        )
                    }
                }
            }

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

@Composable
private fun AlbumCard(title: String, count: Int, previewUri: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(260.dp)
            .padding(0.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF2F2F7)

    ) {
        Row(
            modifier = Modifier.padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square preview image on the left
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (previewUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(previewUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info on the right
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    "$count Items",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "Gallery — Empty")
@Composable
fun GalleryContentEmptyPreview() {
    PoseOverlayTheme {
        GalleryContent(
            images = emptyList(),
            categories = listOf("Portrait", "Full Body"),
            onImageSelect = {},
            imagePickerLauncher = {},
            onNavigateToDetail = {},
            onDeleteImage = {},
            onNavigateToAlbums = {}
        ) {}
    }
}
