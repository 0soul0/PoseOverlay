package com.example.poseoverlay.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images")
    fun getAllImages(): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE uriString = :uri LIMIT 1")
    suspend fun getImageByUri(uri: String): ImageEntity?

    @Query("SELECT * FROM images WHERE category = :category ORDER BY uriString DESC")
    fun getImagesByCategory(category: String): Flow<List<ImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    @Delete
    suspend fun deleteImage(image: ImageEntity)
    
    @Query("SELECT DISTINCT category FROM images")
    fun getAllCategories(): Flow<List<String>>

    @Query("UPDATE images SET category = 'Uncategorized' WHERE category = :category")
    suspend fun moveCategoryToUncategorized(category: String)
}
