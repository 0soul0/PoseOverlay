package com.example.poseoverlay.navigation

sealed class NavigationEvent {
    data class NavigateToImageEdit(val uriString: String) : NavigationEvent()
    data class NavigateToDetail(val uriString: String) : NavigationEvent()
    data class NavigateToAlbums(val category: String) : NavigationEvent()
    object NavigateBack : NavigationEvent()
}