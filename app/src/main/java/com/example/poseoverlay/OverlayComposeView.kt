package com.example.poseoverlay

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.AddPhotoAlternate
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun OverlayComposeView(
    state: OverlayState,
    onToggleTouch: (Boolean) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onPickImage: () -> Unit
) {
    if (state.isMinimized) {
        FloatingBubble(onClick = onMinimize)
    } else {
        OverlayContent(
            state = state,
            onToggleTouch = onToggleTouch,
            onMinimize = onMinimize,
            onClose = onClose,
            onPickImage = onPickImage
        )
    }
}

@Composable
fun FloatingBubble(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
            .clickable { onClick() },
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
    onPickImage: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Transparent Image Layer
        TransformableImage(state = state)

        // Note Layer
        if (state.isNoteVisible) {
            NoteBox(
                text = state.noteText,
                onTextChange = { state.noteText = it },
                onClose = { state.isNoteVisible = false },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Modern Controls Layer (Floating Pill at Bottom)
        if (!state.isLocked) {
             ControlPanel(
                 modifier = Modifier
                     .align(Alignment.BottomCenter)
                     .padding(bottom = 32.dp),
                 alpha = state.alpha,
                 onAlphaChange = { state.alpha = it },
                 isLocked = state.isLocked,
                 onLockToggle = {
                     val newLockState = !state.isLocked
                     state.isLocked = newLockState
                     onToggleTouch(!newLockState)
                 },
                 onMinimize = onMinimize,
                 onPickImage = onPickImage,
                 onNoteToggle = { state.isNoteVisible = !state.isNoteVisible },
                 onClose = onClose
             )
        }
    }
}

@Composable
fun ControlPanel(
    modifier: Modifier = Modifier,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onMinimize: () -> Unit,
    onPickImage: () -> Unit,
    onNoteToggle: () -> Unit,
    onClose: () -> Unit
) {
    // A sleek, glass-morphism style pill
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp)),
        color = Color.Black.copy(alpha = 0.7f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Functional Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                 IconButton(onClick = onPickImage) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick Image", tint = Color.White)
                }
                
                IconButton(onClick = onNoteToggle) {
                    Icon(Icons.Default.Edit, contentDescription = "Note", tint = Color.White)
                }

                IconButton(onClick = onLockToggle) {
                    Icon(
                        if (isLocked) Icons.Default.Lock else Icons.Outlined.LockOpen,
                        contentDescription = "Lock", 
                        tint = if(isLocked) MaterialTheme.colorScheme.primary else Color.White
                    )
                }

                IconButton(onClick = onMinimize) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            // Bottom Row: Alpha Slider (Compact)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(280.dp)
            ) {
                Text(
                    text = "Opacity",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Slider(
                    value = alpha,
                    onValueChange = onAlphaChange,
                    valueRange = 0.1f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                )
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
                        Icons.Default.AddPhotoAlternate, 
                        contentDescription = null, 
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Select an image to overlay", color = Color.White.copy(alpha = 0.5f))
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
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .padding(32.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
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
                placeholder = { Text("Enter shooting tips...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )
        }
    }
}
