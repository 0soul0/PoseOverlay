package com.example.poseoverlay.ui.gallery.screens

import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poseoverlay.ui.common.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddImageScreen(
    uri: Uri,
    categories: List<String>,
    initialCategory: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var newCategory by remember { mutableStateOf(initialCategory) }
    var description by remember { mutableStateOf(initialDescription) }
    var selectedChip by remember { mutableStateOf(initialCategory.ifBlank { "" }) }

    LaunchedEffect(selectedChip) {
        newCategory = selectedChip
    }

    // Style constants: Light Theme
    val backgroundColor = Color.White
    val surfaceColor = Color(0xFFF5F5F7) // iOS-style light gray
    val accentColor = Color(0xFF007AFF) // standard Blue
    val textColor = Color.Black

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Edit Overlayss", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val finalCat = newCategory.ifBlank { AppConstants.Default_CATEGROY }
                        onConfirm(finalCat, description)
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
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Image Preview (Matching ImageEdit Screen Style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceColor)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri)
                        .crossfade(true)
                        .allowHardware(false) // Common fix for local URIs
                        .listener(
                            onError = { _, result ->
                                Log.e("PoseOverlay", "AsyncImage Error: ${result.throwable}")
                            },
                            onSuccess = { _, _ ->
                                Log.d("PoseOverlay", "AsyncImage Success")
                            }
                        )
                        .build(),
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text("Album", color = textColor.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)

                // Combined Dropdown + TextField
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it; expanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = { Text("Enter or select album name") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = backgroundColor,
                            unfocusedContainerColor = backgroundColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedIndicatorColor = accentColor
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )

                    if (categories.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(backgroundColor)
                        ) {
                            categories.filter { it.contains(newCategory, ignoreCase = true) }.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        newCategory = selectionOption
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }

                Text("Description", color = textColor.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Optional notes...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = backgroundColor,
                        unfocusedContainerColor = backgroundColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedIndicatorColor = accentColor
                    ),
                    maxLines = 5
                )
            }
        }
    }
}