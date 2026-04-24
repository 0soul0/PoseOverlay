package com.example.poseoverlay.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poseoverlay.navigation.NavigationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

fun ViewModel.emitNav(flow: MutableSharedFlow<NavigationEvent>, event: NavigationEvent) {

    viewModelScope.launch {
        flow.emit(event)
    }

}