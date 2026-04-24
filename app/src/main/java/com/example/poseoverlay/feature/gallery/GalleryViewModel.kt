package com.example.poseoverlay.feature.gallery

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.*
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.data.ImageRepository
import com.example.poseoverlay.common.AppConstants
import com.example.poseoverlay.navigation.INavigationHandler
import com.example.poseoverlay.navigation.NavigationHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

class GalleryViewModel(
    application: Application,
    private val repository: ImageRepository,
    private val navHandler: NavigationHandler = NavigationHandler()
) : AndroidViewModel(application), INavigationHandler by navHandler {

    private val _onLaunchOverlay = MutableStateFlow("")
    val onLaunchOverlay = _onLaunchOverlay.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AppConstants.Default_CATEGROY)
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _selectedImage = MutableStateFlow<ImageEntity?>(null)
    val selectedImage = _selectedImage.asStateFlow()

    val images = combine(repository.getAllImages(), _selectedCategory) { allImages, category ->
        if (category ==AppConstants.Default_CATEGROY) allImages else allImages.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun findSelectImage(uriString: String) {
        clearSelectImage()
        viewModelScope.launch {
            // 避免重複加載相同 ID
            if (_selectedImage.value?.uriString == uriString) return@launch

            val result = repository.getImageByUri(uriString)
            _selectedImage.value = result
        }
    }

    private fun clearSelectImage() {
        _selectedImage.value = null
    }

    fun onLaunchOverlay(uriString: String) {
        viewModelScope.launch {
            _onLaunchOverlay.emit(uriString)
        }
    }

    fun updateImage(image: ImageEntity) {
        viewModelScope.launch {
            repository.updateImage(image)
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }


    fun addImage(image: ImageEntity) {
        viewModelScope.launch {
            try {
                val uri = image.uriString.toUri()
                val context = getApplication<Application>()
                val fileName = "img_${System.currentTimeMillis()}.jpg"
                val destFile = File(context.filesDir, fileName)

                // file:// used for cache copies; content:// used as fallback
                val inputStream = when (uri.scheme) {
                    "file" -> FileInputStream(File(requireNotNull(uri.path)))
                    else -> context.contentResolver.openInputStream(uri)
                }

                inputStream?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }

                repository.insertImage(image)
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Failed to save image: ${e.message}", e)
            }
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


