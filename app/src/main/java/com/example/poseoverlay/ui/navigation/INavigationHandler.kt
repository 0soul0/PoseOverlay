package com.example.poseoverlay.ui.navigation

import kotlinx.coroutines.flow.SharedFlow

interface INavigationHandler {
    // 暴露給 UI 觀察的事件流
    val navEvent: SharedFlow<NavigationEvent>

    // 定義各種跳轉動作
    fun onNavigateToDetail(uriString: String)
    fun onNavigateToImageEdit(imageId: String)
    fun onNavigateBack()
}