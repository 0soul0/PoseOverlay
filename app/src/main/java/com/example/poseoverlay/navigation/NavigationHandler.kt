package com.example.poseoverlay.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NavigationHandler:INavigationHandler  {
    private val _navEvent = MutableSharedFlow<NavigationEvent>()
    override val navEvent = _navEvent.asSharedFlow()

    // 這裡我們需要一個協程作用域，通常由外部傳入，或在內部處理
    // 為了簡化，我們假設這裡只是定義如何發送
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onNavigateToDetail(uriString: String) {
        scope.launch {
            _navEvent.emit(NavigationEvent.NavigateToDetail(uriString))
        }
    }

    override fun onNavigateToImageEdit(uriString: String) {
        scope.launch {
            _navEvent.emit(NavigationEvent.NavigateToImageEdit(uriString))
        }
    }

    override fun onNavigateToAlbums(category: String) {
        scope.launch {
            _navEvent.emit(NavigationEvent.NavigateToAlbums(category))
        }
    }

    override fun onNavigateBack() {
        scope.launch {
            _navEvent.emit(NavigationEvent.NavigateBack)
        }
    }

}