package com.example.poseoverlay.data

import kotlinx.coroutines.flow.Flow

class ImageRepository(private val imageDao: ImageDao) {

    fun getAllImages(): Flow<List<ImageEntity>> = imageDao.getAllImages()

    fun getImagesByCategory(category: String): Flow<List<ImageEntity>> = imageDao.getImagesByCategory(category)
    
    suspend fun getImageByUri(uri: String): ImageEntity? {
        return imageDao.getImageByUri(uri)
    }

    fun getAllCategories(): Flow<List<String>> = imageDao.getAllCategories()

    suspend fun insertImage(uri: String, category: String, tags: String = "", description: String = "") {
        imageDao.insertImage(ImageEntity(uri, category, tags, description))
    }

    suspend fun deleteImage(image: ImageEntity) {
        imageDao.deleteImage(image)
    }
}
