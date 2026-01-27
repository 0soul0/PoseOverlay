package com.example.poseoverlay.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey val uriString: String,
    val category: String,
    val tags: String = "", // Stored as comma-separated values
    val description: String = ""
)
