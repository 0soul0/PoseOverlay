package com.example.poseoverlay.ui.gallery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.poseoverlay.ui.theme.PoseOverlayTheme

class AddImageActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_CATEGORIES = "extra_categories"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val RESULT_CODE_SAVED = Activity.RESULT_OK
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)?.let { Uri.parse(it) }
        val categories = intent.getStringArrayListExtra(EXTRA_CATEGORIES) ?: arrayListOf("Uncategorized")
        
        if (imageUri == null) {
            finish()
            return
        }
        
        setContent {
            PoseOverlayTheme {
                AddImageScreen(
                    uri = imageUri,
                    categories = categories,
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
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var newCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf("Uncategorized") }

    LaunchedEffect(selectedChip) {
        newCategory = selectedChip
    }

    // Style constants for this specific screen (Dark Theme / Premium)
    val backgroundColor = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val accentColor = Color(0xFF00E5FF) // Teal/Cyan accent
    val textColor = Color.White

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Edit Overlay", color = textColor) },
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
                        val finalCat = if (newCategory.isBlank()) "Uncategorized" else newCategory
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Image Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Category", color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)

                // Quick Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = newCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedChip = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = backgroundColor,
                                labelColor = textColor,
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                // Custom Category Input
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    placeholder = { Text("Or type custom category", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = backgroundColor,
                        unfocusedContainerColor = backgroundColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = Color.Gray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text("Description", color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Add notes...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = backgroundColor,
                        unfocusedContainerColor = backgroundColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = Color.Gray
                    ),
                    maxLines = 3
                )
            }
        }
    }
}
