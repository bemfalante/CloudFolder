package com.example.data

import com.example.cryptography.EncryptionHelper
import com.example.data.database.FolderDao
import com.example.data.database.FolderEntity
import com.example.data.database.MediaObjectDao
import com.example.data.database.MediaObjectEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class VaultRepository(
    private val folderDao: FolderDao,
    private val mediaObjectDao: MediaObjectDao
) {
    // Core data streams
    val foldersFlow: Flow<List<FolderEntity>> = folderDao.getAllFoldersFlow()
    val mediaObjectsFlow: Flow<List<MediaObjectEntity>> = mediaObjectDao.getAllMediaFlow()

    // Mock Live OTA Logs Stream & Sync state
    private val _otaLogs = MutableStateFlow<List<String>>(emptyList())
    val otaLogs: StateFlow<List<String>> = _otaLogs.asStateFlow()

    private val _isCloudConnected = MutableStateFlow(true)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _syncProgress = MutableStateFlow<Float?>(null)
    val syncProgress: StateFlow<Float?> = _syncProgress.asStateFlow()

    private val _latencyMs = MutableStateFlow(45)
    val latencyMs: StateFlow<Int> = _latencyMs.asStateFlow()

    fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val current = _otaLogs.value.toMutableList()
        current.add(0, "[$timestamp] $message")
        if (current.size > 100) {
            current.removeLast()
        }
        _otaLogs.value = current
    }

    fun clearLogs() {
        _otaLogs.value = emptyList()
    }

    suspend fun setCloudConnection(connected: Boolean) {
        _isCloudConnected.value = connected
        if (connected) {
            addLog("Cloud Sync Service connected.")
        } else {
            addLog("Cloud Sync Service disconnected (Offline Mode).")
        }
    }

    // SQLite operations
    suspend fun getFoldersList(): List<FolderEntity> {
        return folderDao.getAllFolders()
    }

    suspend fun createFolder(name: String, parentId: String?) {
        val folder = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            parentId = parentId,
            createdAt = System.currentTimeMillis(),
            remoteSynced = _isCloudConnected.value
        )
        folderDao.insertFolder(folder)
        addLog("Created folder '$name' ${if (parentId != null) "internally" else "at root"}")
        if (_isCloudConnected.value) {
            simulateSyncOperation("Syncing new folder '$name' structure to Cloud Node")
        }
    }

    suspend fun createMediaObject(
        name: String,
        folderId: String?,
        plainContent: String,
        mimeType: String,
        passphraseOnlyIfE2EE: String,
        infoTag: String = "Personal"
    ) {
        val key = EncryptionHelper.deriveKey(passphraseOnlyIfE2EE)
        val encryptedBase64 = EncryptionHelper.encrypt(plainContent, key)
        val sizeBytes = encryptedBase64.toByteArray(Charsets.UTF_8).size.toLong()

        val media = MediaObjectEntity(
            id = UUID.randomUUID().toString(),
            folderId = folderId,
            name = name,
            mimeType = mimeType,
            contentSeed = (1..1000).random(),
            infoTag = infoTag,
            encryptedContentBase64 = encryptedBase64,
            fileSize = sizeBytes,
            remoteSynced = _isCloudConnected.value,
            createdAt = System.currentTimeMillis(),
            isOtaReceived = false
        )
        mediaObjectDao.insertMedia(media)
        addLog("Encrypted and stored '$name' on-device (${sizeBytes} Bytes).")
        if (_isCloudConnected.value) {
            simulateSyncOperation("Uploading raw E2EE Ciphertext for '$name' to Cloud Server")
            mediaObjectDao.updateSyncStatus(media.id, true)
        }
    }

    suspend fun deleteFolder(folderId: String) {
        folderDao.deleteFolderById(folderId)
        addLog("Removed folder instance ID: $folderId")
    }

    suspend fun deleteMedia(mediaId: String) {
        mediaObjectDao.deleteMediaById(mediaId)
        addLog("Purged media item ID: $mediaId from secure local index")
    }

    private suspend fun simulateSyncOperation(title: String) {
        _syncProgress.value = 0.0f
        addLog("$title - starting transaction...")
        for (i in 1..4) {
            delay(150)
            _syncProgress.value = i * 0.25f
            _latencyMs.value = (30..80).random()
            addLog("Transmitting bytes... ${(i * 25)}% complete.")
        }
        _syncProgress.value = null
        addLog("$title - COMPLETE (Success).")
    }

    /**
     * Trigger simulated Over-The-Air (OTA) push update.
     * Simulates an external provider streaming key-locked encryptions to this device client.
     */
    suspend fun runOtaPushSimulation(channelName: String, secretKeyPassphrase: String) {
        _isCloudConnected.value = true
        addLog("📡 OTA Channel connected: SUB@$channelName")
        delay(300)
        
        val totalSteps = 6
        val presetMediaList = getOtaPresets(channelName, secretKeyPassphrase)
        
        for (step in 1..totalSteps) {
            _latencyMs.value = when (step) {
                1 -> {
                    addLog("OTA Network Polling active. Found secure update block.")
                    55
                }
                2 -> {
                    addLog("Initiating hand-shake via Elliptic Curve Diffie-Hellman channel...")
                    42
                }
                3 -> {
                    addLog("Downloading OTA secure blob partition (${presetMediaList.size} media payloads detected)...")
                    _syncProgress.value = 0.3f
                    98
                }
                4 -> {
                    addLog("Reassembling streaming network packets into memory buffer...")
                    _syncProgress.value = 0.6f
                    77
                }
                5 -> {
                    addLog("Decrypting partition manifest with active session Key Agreement...")
                    _syncProgress.value = 0.9f
                    31
                }
                else -> {
                    addLog("Manifest processed successfully. Writing to local SQLite schema...")
                    _syncProgress.value = 1.0f
                    15
                }
            }
            delay(400)
        }

        // Generate folder dynamic structures
        val defaultOtaFolderId = "ota_folder_$channelName"
        val existingFolders = folderDao.getAllFolders()
        val hasOtaFolder = existingFolders.any { it.id == defaultOtaFolderId }
        
        if (!hasOtaFolder) {
            val otaFolder = FolderEntity(
                id = defaultOtaFolderId,
                name = "OTA Broadcast ($channelName)",
                parentId = null,
                createdAt = System.currentTimeMillis(),
                remoteSynced = true
            )
            folderDao.insertFolder(otaFolder)
        }

        // Store received encrypted files
        val itemsToInsert = presetMediaList.map { preset ->
            MediaObjectEntity(
                id = UUID.randomUUID().toString(),
                folderId = defaultOtaFolderId,
                name = preset.name,
                mimeType = preset.mimeType,
                contentSeed = (1..100).random(),
                infoTag = "OTA Feed",
                encryptedContentBase64 = preset.encryptedContent,
                fileSize = preset.encryptedContent.toByteArray(Charsets.UTF_8).size.toLong(),
                remoteSynced = true,
                createdAt = System.currentTimeMillis(),
                isOtaReceived = true
            )
        }

        mediaObjectDao.insertMediaObjects(itemsToInsert)
        _syncProgress.value = null
        addLog("📦 OTA Integration Success: Captured ${itemsToInsert.size} secure objects inside 'OTA Broadcast ($channelName)' folder.")
    }

    private fun getOtaPresets(channelName: String, keyPassphrase: String): List<OtaPresetItem> {
        val key = EncryptionHelper.deriveKey(keyPassphrase)
        
        return when (channelName) {
            "Admin Intel" -> listOf(
                OtaPresetItem(
                    name = "operations_schedule.txt",
                    mimeType = "text/plain",
                    encryptedContent = EncryptionHelper.encrypt(
                        "OTA UPDATE - PROJECT TITAN:\n" +
                        "1. Daily data backups scheduled globally at 04:00 UTC.\n" +
                        "2. Field stations must switch to rotating master keys in Q3.\n" +
                        "3. Code phrase: 'Antigravity Vector Blue'.", key
                    )
                ),
                OtaPresetItem(
                    name = "hq_passcodes.txt",
                    mimeType = "text/plain",
                    encryptedContent = EncryptionHelper.encrypt(
                        "Vault door 2 passcode: 6891#\n" +
                        "Server room cipher: 2026-ENCRYPT-MAX\n" +
                        "Backup air corridor override code: 9942", key
                    )
                )
            )
            "Strategic Briefings" -> listOf(
                OtaPresetItem(
                    name = "intercept_brief.txt",
                    mimeType = "text/plain",
                    encryptedContent = EncryptionHelper.encrypt(
                        "RESTRICTED INSTRUCTIONS:\n" +
                        "A critical stream anomaly was identified in the cloud synchronization layer. " +
                        "Ensure standard E2EE is enabled dynamically on all nodes. Avoid plain transfers.", key
                    )
                ),
                OtaPresetItem(
                    name = "coordinate_matrix.txt",
                    mimeType = "text/plain",
                    encryptedContent = EncryptionHelper.encrypt(
                        "SATELLITE SYNC COORDINATES:\n" +
                        "Sector A: 48.8584° N, 2.2945° E\n" +
                        "Sector B: 35.6762° N, 139.6503° E\n" +
                        "System state: ACTIVE", key
                    )
                )
            )
            else -> listOf(
                OtaPresetItem(
                    name = "hello_world_payload.txt",
                    mimeType = "text/plain",
                    encryptedContent = EncryptionHelper.encrypt(
                        "Welcome to the general OTA Stream drop! This packet was received on the '$channelName' channel. " +
                        "End-to-End client decryption verified. Connection is secure.", key
                    )
                )
            )
        }
    }

    private data class OtaPresetItem(
        val name: String,
        val mimeType: String,
        val encryptedContent: String
    )

    suspend fun clearAllData() {
        folderDao.clearAllFolders()
        mediaObjectDao.clearAllMedia()
        addLog("Database fully purged. Storage cleared.")
    }

    suspend fun generateSeedData(passphrase: String) {
        clearAllData()
        addLog("Initializing secure seed directories...")
        
        // Root folders
        val personalId = "f_personal"
        val confidentialId = "f_confidential"
        val financesId = "f_finances"

        folderDao.insertFolders(
            listOf(
                FolderEntity(personalId, "Personal Vault", null, System.currentTimeMillis() - 50000, true),
                FolderEntity(confidentialId, "Confidential Items", null, System.currentTimeMillis() - 40000, true),
                FolderEntity(financesId, "Financial Reports", null, System.currentTimeMillis() - 30000, true)
            )
        )

        // Subfolders
        val taxId = "f_taxes"
        folderDao.insertFolder(
            FolderEntity(taxId, "Tax Returns", financesId, System.currentTimeMillis() - 20000, true)
        )

        delay(100)
        addLog("Folders seeded. Encrypting sample media objects on-board...")

        val key = EncryptionHelper.deriveKey(passphrase)

        // Seed files
        val files = listOf(
            Triple("private_journal.txt", "Today I explored End-to-End Encryption in Jetpack Compose. Storing data in raw formats is extremely fragile, so compiling binary envelopes via AES-256-CBC offers proper peace of mind.", personalId),
            Triple("diary_passwords.txt", "Master key pattern: alpha-omega-seattle-2026. Avoid listing actual passwords here. Set biometric access is secondary option.", personalId),
            Triple("mission_report_alpha.txt", "CRITICAL REPORT:\nSystem anomaly detected, OTA components running correctly. Payload streams successfully re-route safely during packet drop incidents.", confidentialId),
            Triple("q1_expenditure.txt", "Q1 Financial Ledger details:\nEquipment: $4,200\nNetwork Bandwidth Services: $1,450\nSecurity Auditing: $6,700\nTotal Net: $12,350", financesId),
            Triple("tax_receipts_2025.txt", "IRS Receipt Reference ID: #23049-M. Adjusted gross income has been calculated. Decrypted correctly.", taxId)
        )

        for (file in files) {
            val encryptedBase64 = EncryptionHelper.encrypt(file.second, key)
            val size = encryptedBase64.toByteArray(Charsets.UTF_8).size.toLong()
            mediaObjectDao.insertMedia(
                MediaObjectEntity(
                    id = UUID.randomUUID().toString(),
                    folderId = file.third,
                    name = file.first,
                    mimeType = "text/plain",
                    contentSeed = (1..100).random(),
                    infoTag = "Secure Cache",
                    encryptedContentBase64 = encryptedBase64,
                    fileSize = size,
                    remoteSynced = true,
                    createdAt = System.currentTimeMillis(),
                    isOtaReceived = false
                )
            )
        }
        addLog("Secure database seeding complete with custom encryption.")
    }
}
