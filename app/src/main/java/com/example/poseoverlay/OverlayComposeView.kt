package com.example.poseoverlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun OverlayComposeView(
    state: OverlayState,
    onToggleTouch: (Boolean) -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    onOpenGallery: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onCategorySelect: (String) -> Unit,
    onImageSelect: (com.example.poseoverlay.data.ImageEntity) -> Unit
) {
    if (state.isMinimized) {
        FloatingBubble(
            alpha = state.alpha,
            onMaximize = onMaximize,
            onMove = onMove,
            onAlphaChange = { diff ->
                state.alpha = (state.alpha - diff / 500f).coerceIn(0.1f, 1f)
            }
        )
    } else {
        OverlayContent(
            state = state,
            onToggleTouch = onToggleTouch,
            onMinimize = onMinimize,
            onClose = onClose,
            onOpenGallery = onOpenGallery,
            onCategorySelect = onCategorySelect,
            onImageSelect = onImageSelect
        )
    }
}

@Composable
fun FloatingBubble(
    alpha: Float,
    onMaximize: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onAlphaChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                             onMove(dragAmount.x, dragAmount.y)
                        } else {
                             onAlphaChange(dragAmount.y)
                        }
                    },
                    onDragEnd = { }
                )
            }
            .clickable { onMaximize() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add, 
            contentDescription = "Restore",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun OverlayContent(
    state: OverlayState,
    onToggleTouch: (Boolean) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onOpenGallery: () -> Unit,
    onCategorySelect: (String) -> Unit,
    onImageSelect: (com.example.poseoverlay.data.ImageEntity) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Transparent Image Layer
        TransformableImage(state = state)

        if (state.isNoteVisible) {
            NoteBox(
                text = state.noteText,
                onTextChange = { state.noteText = it },
                onClose = { state.isNoteVisible = false },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (!state.isLocked) {
             ExpandableControlPanel(
                 modifier = Modifier
                     .align(Alignment.BottomCenter)
                     .padding(bottom = 24.dp),
                 state = state,
                 onAlphaChange = { state.alpha = it },
                 onLockToggle = {
                     val newLockState = !state.isLocked
                     state.isLocked = newLockState
                     onToggleTouch(!newLockState)
                 },
                 onMinimize = onMinimize,
                 onOpenGallery = onOpenGallery,
                 onNoteToggle = { state.isNoteVisible = !state.isNoteVisible },
                 onClose = onClose,
                 onCategorySelect = onCategorySelect,
                 onImageSelect = onImageSelect
             )
        }
    }
}

@Composable
fun ExpandableControlPanel(
    modifier: Modifier = Modifier,
    state: OverlayState,
    onAlphaChange: (Float) -> Unit,
    onLockToggle: () -> Unit,
    onMinimize: () -> Unit,
    onOpenGallery: () -> Unit,
    onNoteToggle: () -> Unit,
    onClose: () -> Unit,
    onCategorySelect: (String) -> Unit,
    onImageSelect: (com.example.poseoverlay.data.ImageEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (expanded) {
            // Expanded View
            
            // Categories
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                items(state.categories.size) { index ->
                    val cat = state.categories[index]
                    androidx.compose.material3.FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick = { onCategorySelect(cat) },
                        label = { Text(cat, color = if(state.selectedCategory == cat) Color.Black else Color.White) }, 
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.2f),
                            labelColor = Color.White
                        ),
                        border = null
                    )
                }
            }

            // Images
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(state.images.size) { index ->
                    val img = state.images[index]
                    AsyncImage(
                        model = img.uriString,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray)
                            .clickable { onImageSelect(img) },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            // Controls Row (Similar to original but cleaner)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = onOpenGallery) { Icon(Icons.Default.Image, "Gallery", tint = Color.White) }
                    IconButton(onClick = onNoteToggle) { Icon(Icons.Default.Edit, "Note", tint = Color.White) }
                    IconButton(onClick = onLockToggle) { Icon(Icons.Outlined.LockOpen, "Lock", tint = Color.White) }
                    IconButton(onClick = onMinimize) { Icon(Icons.Default.KeyboardArrowDown, "Minimize", tint = Color.White) }
                }
                
                IconButton(
                    onClick = { onClose() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.size(16.dp))
            
            // Collapse Button
             IconButton(
                onClick = { expanded = false },
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "Collapse", tint = Color.White)
            }

        } else {
            // Collapsed View: Slider + Expand Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Alpha Slider (Taking most space)
                Slider(
                    value = state.alpha,
                    onValueChange = onAlphaChange,
                    valueRange = 0.1f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Expand Button
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TransformableImage(state: OverlayState) {
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        offset += panChange
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformableState)
    ) {
         if (state.imageUri != null) {
            AsyncImage(
                model = state.imageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        rotationZ = rotation,
                        translationX = offset.x,
                        translationY = offset.y,
                        alpha = state.alpha
                    )
            )
        } else {
             Box(
                 modifier = Modifier.fillMaxSize(),
                 contentAlignment = Alignment.Center
             ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        "Tap Menu > Gallery to select image", 
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                 }
             }
        }
    }
}

@Composable
fun NoteBox(
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .padding(32.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notes", color = Color.White, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Note", tint = Color.White)
                }
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = { Text("Write your thoughts...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun ExpandableControlPanelPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(20.dp).background(Color.Gray)) {
            ExpandableControlPanel(
                state = OverlayState().apply { 
                    categories = listOf("All", "Portrait", "Hand")
                    alpha = 0.7f 
                },
                onAlphaChange = {},
                onLockToggle = {},
                onMinimize = {},
                onOpenGallery = {},
                onNoteToggle = {},
                onClose = {},
                onCategorySelect = {},
                onImageSelect = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun NoteBoxPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(20.dp).background(Color.Gray)) {
            NoteBox(
                text = "Sample Note",
                onTextChange = {},
                onClose = {}
            )
        }
    }
}
