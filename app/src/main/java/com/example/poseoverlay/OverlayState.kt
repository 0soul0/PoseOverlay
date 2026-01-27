package com.example.poseoverlay

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.poseoverlay.data.ImageEntity

class OverlayState {
    var imageUri by mutableStateOf<Uri?>(null)
    var alpha by mutableFloatStateOf(0.5f)
    var isMinimized by mutableStateOf(false)
    var isLocked by mutableStateOf(false)
    var noteText by mutableStateOf("")
    var isNoteVisible by mutableStateOf(false)

    // In-Overlay Gallery
    var categories by mutableStateOf(listOf<String>())
    var selectedCategory by mutableStateOf("All")
    var images by mutableStateOf(listOf<com.example.poseoverlay.data.ImageEntity>())
}
