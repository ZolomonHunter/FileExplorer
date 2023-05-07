package com.example.fileexplorer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("FilesHash")
data class FileModel(
    @PrimaryKey
    val name: String,
    val oldHash: String,
    val newHash: String
)
