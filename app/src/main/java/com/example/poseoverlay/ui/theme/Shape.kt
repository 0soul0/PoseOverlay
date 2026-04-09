package com.example.poseoverlay.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Google Photos uses Material 3 canonical shapes.
// Notably: photo grid tiles = small rounding, FAB = full circle, cards = medium
val GPhotosShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // chip, badge, snackbar
    small      = RoundedCornerShape(8.dp),    // photo grid tile, input field
    medium     = RoundedCornerShape(12.dp),   // cards, dialogs
    large      = RoundedCornerShape(16.dp),   // bottom sheet header
    extraLarge = RoundedCornerShape(28.dp)    // FAB, modal bottom sheet
)
