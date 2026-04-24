package com.example.poseoverlay.feature.overlay

import android.graphics.Rect
import android.net.Uri
import androidx.compose.runtime.*
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.common.AppConstants

class OverlayState {
    var imageUri by mutableStateOf<Uri?>(null)
    var alpha by mutableFloatStateOf(0.5f)
    var isMinimized by mutableStateOf(false)
    var isLocked by mutableStateOf(false)
    var noteText by mutableStateOf("")
    var isVisible by mutableStateOf(true)
    var isNoteVisible by mutableStateOf(false)

    // In-Overlay Gallery
    var categories by mutableStateOf(listOf<String>())
    var selectedCategory by mutableStateOf(AppConstants.Default_CATEGROY)
    var images by mutableStateOf(listOf<ImageEntity>())

    // 儲存所有可互動組件的區域 (用於實現點擊透傳)
    // 使用 SnapshotStateMap (mutableStateMapOf) 確保多組件更新時的執行緒/重繪安全
    val interactiveBounds = mutableStateMapOf<String, Rect>()
}
