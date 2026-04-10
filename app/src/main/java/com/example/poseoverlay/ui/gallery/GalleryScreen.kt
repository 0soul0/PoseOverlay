package com.example.poseoverlay.ui.gallery

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.ui.theme.PoseOverlayTheme

// ═══════════════════════════════════════════════════════════════════════════
//  Stateful wrapper — owns ViewModel + Activity launchers
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onAddImage: (Uri) -> Unit,
    onEditImage: (ImageEntity) -> Unit,
    onImageSelect: (String) -> Unit
) {
    val context = LocalContext.current

    val images by viewModel.images.collectAsState()

    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { contentUri ->
            try {
                // 立刻把 content:// 複製到 cacheDir，得到穩定的 file:// URI
                // 這樣 AddImageScreen 預覽就不會有 SecurityException
                val tempFile = java.io.File(context.cacheDir, "img_preview_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(contentUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val fileUri = Uri.fromFile(tempFile)
                onAddImage(fileUri)
            } catch (e: Exception) {
                android.util.Log.e("Gallery", "Failed to copy to cache: ${e.message}", e)
                onAddImage(contentUri)
            }
        }
    }

    GalleryContent(
        images           = images,
        categories       = categories,
        selectedCategory = selectedCategory,
        onSelectCategory = { viewModel.selectCategory(it) },
        onImageSelect    = onImageSelect,
        onDeleteImage     = { viewModel.deleteImage(it) },
        onAddClick        = { imagePicker.launch(arrayOf("image/*")) },
        onEditImage       = onEditImage
    )
}


// ═══════════════════════════════════════════════════════════════════════════
//  Pure UI — zero side-effects, fully previewable
// ═══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryContent(
    images: List<ImageEntity>,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onImageSelect: (String) -> Unit,
    onDeleteImage: (ImageEntity) -> Unit,
    onAddClick: () -> Unit,
    onEditImage: (ImageEntity) -> Unit,
) {
    var imageToDelete by remember { mutableStateOf<ImageEntity?>(null) }
    var selectedImageForDetail by remember { mutableStateOf<ImageEntity?>(null) }
    var inCategoryDetail by remember { mutableStateOf<String?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val safeCategories = categories.filter { it.isNotBlank() }
    // 將 "add" 按鈕放到列表最前面或最後面
    val gridItems = images + ImageEntity("add", "", "", "", "")
    val gridState = rememberLazyGridState()
    
    // 計算標題是否該切換：當第一個可見項目的索引超過 Albums 建立的索引值（例如 Albums + 標題佔了 2 個 item）
    val showAllPhotosTitle by remember {
        derivedStateOf {
            // 根據你的佈局，Albums 標題是 item(0)，Horizontal Grid 是 item(1)，All Photos 標題是 item(2)
            // 所以當索引 >= 2 時，切換標題
            gridState.firstVisibleItemIndex >= 2
        }
    }
    
    LaunchedEffect(gridItems.size) {
        if (gridItems.isNotEmpty()) {
            // gridState.animateScrollToItem(index = gridItems.size - 1)
        }
    }

    // 處理返回鍵邏輯
    if (inCategoryDetail != null) {
        BackHandler { inCategoryDetail = null }
    } else if (selectedImageForDetail != null) {
        BackHandler { selectedImageForDetail = null }
    }

    if (imageToDelete != null) {
        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Delete Image") },
            text = { Text("Remove this image from library?") },
            confirmButton = {
                TextButton(
                    onClick = { imageToDelete?.let { onDeleteImage(it) }; imageToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { imageToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // 如果進入了資料夾詳情，接管返回鍵
    if (inCategoryDetail != null) {
        BackHandler { inCategoryDetail = null }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =  "Gallery",
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
                Text("Albums",
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
                            onClick = { inCategoryDetail = cat }
                        )
                    }
                }
            }

            // 3. Photos 標題
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Photos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            // 4. 照片網格
            items(gridItems, key = { it.uriString }) { img ->
                Box(modifier = Modifier.aspectRatio(1f)) {
                    if (img.uriString == "add") {
                        AddImageGridItem(onClick = onAddClick)
                    } else {
                        GalleryItem(
                            img,
                            onClick = { selectedImageForDetail = img },
                            onLongClick = { imageToDelete = img }
                        )
                    }
                }
            }

        }
    }

    // Folder Detail (image2 style) — Opens as a "new page" overlay
    if (inCategoryDetail != null) {
        val currentCat = inCategoryDetail!!
        val folderImages = images.filter { it.category == currentCat }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Header (Banner)
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(folderImages.firstOrNull()?.uriString ?: "")
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Text(currentCat, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("${folderImages.size} Items", color = Color.White.copy(alpha = 0.8f))
                    }

                    IconButton(
                        onClick = { inCategoryDetail = null },
                        modifier = Modifier.statusBarsPadding().padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
                    }
                }

                // Grid in folder
                val gridWithAdd = folderImages + ImageEntity("add", "", "", "", "")
                gridWithAdd.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 2.dp, vertical = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        row.forEach { img ->
                            if (img.uriString == "add") {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AddImageGridItem(onClick = onAddClick)
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    GalleryItem(img, onClick = { selectedImageForDetail = img }, onLongClick = { imageToDelete = img })
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Full Screen Detail View
    if (selectedImageForDetail != null) {
        val img = selectedImageForDetail!!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image Preview (Fill)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(img.uriString)
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
                IconButton(onClick = { selectedImageForDetail = null }) {
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
                    DetailActionItem(Icons.Outlined.Edit, "Edit") {
                        onEditImage(img) // 真正執行 Activity 跳轉
                        selectedImageForDetail = null
                    }
                    DetailActionItem(Icons.Outlined.Delete, "Delete") {
                        imageToDelete = img
                        selectedImageForDetail = null
                    }
                    DetailActionItem(Icons.Outlined.PlayArrow, "Start") {
                        onImageSelect(img.uriString)
                        selectedImageForDetail = null
                    }
                }
            }
        }
    }

    if (showInfoDialog && selectedImageForDetail != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Image Info") },
            text = {
                Column {
                    Text("Category: ${selectedImageForDetail!!.category}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedImageForDetail!!.description.ifBlank { "No description provided." })
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
fun AlbumCard(title: String, count: Int, previewUri: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(260.dp).padding(0.dp),
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

@Composable
fun AddImageGridItem(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
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
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.uriString)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
@Composable
fun DetailActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Previews — only reference GalleryContent, no real ViewModel needed
// ═══════════════════════════════════════════════════════════════════════════
@Preview(showBackground = true, name = "Gallery — Empty")
@Composable
fun GalleryContentEmptyPreview() {
    PoseOverlayTheme {
        GalleryContent(
            images           = emptyList(),
            categories       = listOf("Portrait", "Full Body"),
            selectedCategory = "All",
            onSelectCategory = {},
            onImageSelect    = {},
            onDeleteImage    = {},
            onAddClick       = {},
            onEditImage = {}
        )
    }
}
