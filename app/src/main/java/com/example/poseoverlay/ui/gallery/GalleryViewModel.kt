package com.example.poseoverlay.ui.gallery

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.poseoverlay.data.ImageEntity
import com.example.poseoverlay.data.ImageRepository
import kotlinx.coroutines.flow.*
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
        if (category == "All") allImages else allImages.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addImage(uri: Uri, category: String, tags: String = "", description: String = "") {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val fileName = "img_${System.currentTimeMillis()}.jpg"
                val destFile = java.io.File(context.filesDir, fileName)

                // file:// used for cache copies; content:// used as fallback
                val inputStream = when (uri.scheme) {
                    "file" -> java.io.FileInputStream(java.io.File(requireNotNull(uri.path)))
                    else   -> context.contentResolver.openInputStream(uri)
                }

                inputStream?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }

                repository.insertImage(Uri.fromFile(destFile).toString(), category, tags, description)
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
