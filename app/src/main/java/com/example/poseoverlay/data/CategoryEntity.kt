package com.example.poseoverlay.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CategoryEntity(@PrimaryKey val name: String)
