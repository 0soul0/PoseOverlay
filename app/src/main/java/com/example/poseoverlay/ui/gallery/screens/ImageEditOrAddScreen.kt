package com.example.poseoverlay.ui.gallery.screens

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.ui.common.AppConstants
import com.example.poseoverlay.ui.gallery.GalleryViewModel

const val TYPE_EDIT = "Edit"
const val TYPE_ADD = "Add"

@Composable
fun ImageEditOrAddScreen(
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel,
    uri: Uri? = null
) {

    val type = if (uri == null) {
        TYPE_EDIT
    } else {
        TYPE_ADD
    }

    val categories by viewModel.categories.collectAsState()

    val collectedImage by viewModel.selectedImage.collectAsState()

// 2. 根據類型決定最終顯示的實體
    val selectedImage = if (type == TYPE_EDIT) {
        collectedImage ?: ImageEntity("", "", "", "", "") // 如果是 null 則給空物件
    } else {
        val selectedCategory by viewModel.selectedCategory.collectAsState()
        ImageEntity(uri.toString(), "", selectedCategory, "", "")
    }


    ImageEditContent(
        modifier = modifier,
        type = type,
        selectedImage = selectedImage,
        existingCategories = categories,
        onNavigateBack = { viewModel.onNavigateBack() },
        onConfirm = { image ->
            if (type == TYPE_EDIT) {
                viewModel.updateImage(image)
            } else {
                uri?.let { viewModel.addImage(image) }
            }
            viewModel.onNavigateBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageEditContent(
    modifier: Modifier,
    type: String,
    existingCategories: List<String>,
    selectedImage: ImageEntity,
    onNavigateBack: () -> Unit, onConfirm: (ImageEntity) -> Unit,
) {


    var category by remember(selectedImage) {
        mutableStateOf(
            if (selectedImage.category == AppConstants.Default_CATEGROY) {
                ""
            } else {
                selectedImage.category
            })
    }
    var description by remember(selectedImage) { mutableStateOf(selectedImage.description) }
    var expanded by remember { mutableStateOf(false) }


    val backgroundColor = Color.White
    val surfaceColor = Color(0xFFF5F5F7)
    val accentColor = Color(0xFF007AFF)
    val textColor = Color.Black

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(type, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onConfirm(selectedImage.copy(category = category.ifBlank { AppConstants.Default_CATEGROY }, description = description))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceColor)
            ) {
                AsyncImage(
                    model = selectedImage.uriString,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Inputs Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Album", color = textColor.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it; expanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = { Text("Select or name an album") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = backgroundColor,
                            unfocusedContainerColor = backgroundColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedIndicatorColor = accentColor
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )

                    if (existingCategories.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(backgroundColor)
                        ) {
                            existingCategories.filter { it.contains(category, ignoreCase = true) }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Description", color = textColor.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Notes about this pose...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = backgroundColor,
                        unfocusedContainerColor = backgroundColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedIndicatorColor = accentColor
                    ),
                    minLines = 3,
                    maxLines = 5
                )
            }
        }
    }
}
