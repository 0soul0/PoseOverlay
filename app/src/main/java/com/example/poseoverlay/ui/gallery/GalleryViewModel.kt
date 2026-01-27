package com.example.poseoverlay.ui.gallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.data.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(
    application: Application,
    private val repository: ImageRepository
) : AndroidViewModel(application) {

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val images = combine(repository.getAllImages(), _selectedCategory) { allImages, category ->
        if (category == "All") {
            allImages
        } else {
            allImages.filter { it.category == category }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addImage(uri: Uri, category: String, tags: String = "", description: String = "") {
        viewModelScope.launch {
            try {
                 val contentResolver = getApplication<Application>().contentResolver
                 contentResolver.takePersistableUriPermission(
                     uri,
                     android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                 )
            } catch (e: Exception) {
                // Log or ignore if already granted/not possible
                e.printStackTrace()
            }
            repository.insertImage(uri.toString(), category, tags, description)
        }
    }

    fun deleteImage(image: ImageEntity) {
        viewModelScope.launch {
            repository.deleteImage(image)
        }
    }
}

class GalleryViewModelFactory(
    private val application: Application,
    private val repository: ImageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
