package com.example.poseoverlay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT name FROM custom_categories ORDER BY name ASC")
    fun getAllCustomCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("DELETE FROM custom_categories WHERE name = :name")
    suspend fun deleteCategory(name: String)
}
