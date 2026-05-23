package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getAllFoldersFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders")
    suspend fun getAllFolders(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    @Query("DELETE FROM folders")
    suspend fun clearAllFolders()
}

@Dao
interface MediaObjectDao {
    @Query("SELECT * FROM media_objects ORDER BY createdAt DESC")
    fun getAllMediaFlow(): Flow<List<MediaObjectEntity>>

    @Query("SELECT * FROM media_objects")
    suspend fun getAllMedia(): List<MediaObjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaObjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaObjects(mediaList: List<MediaObjectEntity>)

    @Query("DELETE FROM media_objects WHERE id = :id")
    suspend fun deleteMediaById(id: String)

    @Query("UPDATE media_objects SET remoteSynced = :remoteSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: String, remoteSynced: Boolean)

    @Query("DELETE FROM media_objects")
    suspend fun clearAllMedia()
}
