package com.example.fileexplorer

import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.File
import java.util.concurrent.Flow

@Dao
interface FileDao {
    @Upsert
    suspend fun upsertFile(file: FileModel)
    @Delete
    suspend fun deleteFile(file: FileModel)
    @Query("SELECT * FROM FilesHash WHERE name = :name")
    suspend fun getFile(name: String): FileModel?
    @Query("SELECT * FROM FilesHash")
    suspend fun getAllFiles(): List<FileModel>
    @Query("UPDATE FilesHash SET oldHash = newHash WHERE name = :name")
    suspend fun replaceOldHashWithNewHash(name: String)
    @Query("UPDATE FilesHash SET newHash = :newHash WHERE name = :name")
    suspend fun updateNewHash(name: String, newHash: String)
}