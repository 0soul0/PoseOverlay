package com.example.poseoverlay.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ImageRepository(
    private val imageDao: ImageDao,
    private val categoryDao: CategoryDao
) {

    fun getAllImages(): Flow<List<ImageEntity>> = imageDao.getAllImages()

    fun getImagesByCategory(category: String): Flow<List<ImageEntity>> = imageDao.getImagesByCategory(category)

    suspend fun getImageByUri(uri: String): ImageEntity? = imageDao.getImageByUri(uri)

    /**
     * Merged categories = custom_categories table UNION distinct categories from images,
     * so the list always includes both "empty" custom categories and any category
     * that images already belong to.
     */
    fun getAllCategories(): Flow<List<String>> =
        combine(
            categoryDao.getAllCustomCategories(),
            imageDao.getAllCategories()
        ) { custom, fromImages ->
            val merged = (custom + fromImages).filter { it.isNotBlank() }.distinct().sorted()
            if (merged.isEmpty()) listOf("Recent") else merged
        }

    suspend fun insertImage(uri: String, category: String, tags: String = "", description: String = "") {
        imageDao.insertImage(
            ImageEntity(
                uriString = uri,
                name = "", // Or extract name from URI if needed
                category = category,
                tags = tags,
                description = description
            )
        )
        // Auto-register the category so it survives even if all images are deleted
        categoryDao.insertCategory(CategoryEntity(category))
    }

    suspend fun deleteImage(image: ImageEntity) {
        imageDao.deleteImage(image)
    }

    suspend fun addCategory(name: String) {
        categoryDao.insertCategory(CategoryEntity(name))
    }

    /**
     * Delete category from custom list.
     * Images that belong to it are moved to "Uncategorized" (they stay in the library).
     */
    suspend fun deleteCategory(name: String) {
        categoryDao.deleteCategory(name)
        imageDao.moveCategoryToUncategorized(name)
    }
}
