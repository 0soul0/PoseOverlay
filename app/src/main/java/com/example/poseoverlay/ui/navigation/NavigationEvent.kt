package com.example.poseoverlay.ui.navigation

sealed class NavigationEvent {
    data class NavigateToImageEdit(val imageId: String) : NavigationEvent()
    data class NavigateToDetail(val uriString: String) : NavigationEvent()
    object NavigateBack : NavigationEvent()
}