package com.example.poseoverlay.feature.overlay

sealed class OverlayEvent  {

    object onNavigateToGallery : OverlayEvent()
    data class toggleLock(
        val isLocked: Boolean
    ) : OverlayEvent()

}