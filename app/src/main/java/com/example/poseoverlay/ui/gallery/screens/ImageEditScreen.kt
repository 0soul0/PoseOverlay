package com.example.poseoverlay.ui.gallery.screens

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.ui.gallery.GalleryViewModel

@Composable
fun ImageEditScreen(modifier: Modifier = Modifier, viewModel: GalleryViewModel) {

    val selectedImage by viewModel.selectedImage.collectAsState()
    val categories by viewModel.categories.collectAsState()
    ImageEditContent(
        modifier = modifier,
        selectedImage = selectedImage ?: ImageEntity("", "", "", "", ""),
        existingCategories = categories,
        onNavigateBack = { viewModel.onNavigateBack() },
        onConfirm = { image ->
            viewModel.updateImage(image)
            viewModel.onNavigateBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageEditContent(
    modifier: Modifier,
    existingCategories: List<String>,
    selectedImage: ImageEntity,
    onNavigateBack: () -> Unit, onConfirm: (ImageEntity) -> Unit,
) {

    var category by remember(selectedImage) { mutableStateOf(selectedImage.category) }
    var description by remember(selectedImage) { mutableStateOf(selectedImage.description) }
    var expanded by remember { mutableStateOf(false) }

    // Style constants (Light Theme matching AddImageActivity)
    val backgroundColor = Color.White
    val surfaceColor = Color(0xFFF5F5F7)
    val accentColor = Color(0xFF007AFF)
    val textColor = Color.Black

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Pose", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { onNavigateBack() }) { Text("Cancel", color = accentColor) }
                },
                actions = {
                    Button(
                        onClick = { onConfirm(selectedImage.copy(category = category, description = description)) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor)
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
