package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String?, // Null means root folder
    val createdAt: Long,
    val remoteSynced: Boolean
)

@Entity(tableName = "media_objects")
data class MediaObjectEntity(
    @PrimaryKey val id: String,
    val folderId: String?, // Null means root folder
    val name: String, // Decrypted file name
    val mimeType: String, // e.g. "image/png", "audio/mp3", "text/plain"
    val contentSeed: Int, // Deterministic seed for generating mock assets
    val infoTag: String, // e.g. "Personal", "Confidential", "OTA Hub"
    val encryptedContentBase64: String, // Base64 of IV + Ciphertext
    val fileSize: Long,
    val remoteSynced: Boolean,
    val createdAt: Long,
    val isOtaReceived: Boolean
)
