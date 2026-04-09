package com.example.poseoverlay.ui.gallery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.poseoverlay.ui.theme.PoseOverlayTheme
import androidx.core.net.toUri

class AddImageActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_CATEGORIES = "extra_categories"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val RESULT_CODE_SAVED = RESULT_OK
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)?.toUri()
        // 移除預設資料夾，只使用資料庫現有的
        val initialCategory = intent.getStringExtra(EXTRA_CATEGORY) ?: ""
        val initialDescription = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        val categories = intent.getStringArrayListExtra(EXTRA_CATEGORIES) ?: arrayListOf()
        
        if (imageUri == null) {
            finish()
            return
        }

        setContent {
            PoseOverlayTheme {
                AddImageScreen(
                    uri = imageUri,
                    categories = categories,
                    initialCategory = initialCategory,
                    initialDescription = initialDescription,
                    onDismiss = { finish() },
                    onConfirm = { category, description ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_CATEGORY, category)
                            putExtra(EXTRA_DESCRIPTION, description)
                            putExtra(EXTRA_IMAGE_URI, imageUri.toString())
                        }
                        setResult(RESULT_CODE_SAVED, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

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
    var selectedChip by remember { mutableStateOf(if (initialCategory.isBlank()) "Uncategorized" else initialCategory) }

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
                        val finalCat = newCategory.ifBlank { "All" }
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
                    model = uri,
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
