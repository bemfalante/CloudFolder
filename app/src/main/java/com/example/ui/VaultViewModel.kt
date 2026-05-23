package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptography.EncryptionHelper
import com.example.data.VaultRepository
import com.example.data.database.AppDatabase
import com.example.data.database.FolderEntity
import com.example.data.database.MediaObjectEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.crypto.spec.SecretKeySpec

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = VaultRepository(database.folderDao(), database.mediaObjectDao())

    // Core inputs
    val passphrase = MutableStateFlow("admin123")
    val searchQuery = MutableStateFlow("")
    val activeFolderId = MutableStateFlow<String?>(null) // null = root

    // Selected items for display / inspectors
    val selectedMediaItem = MutableStateFlow<MediaObjectEntity?>(null)
    val showCreateFolderDialog = MutableStateFlow(false)
    val showCreateFileDialog = MutableStateFlow(false)
    val showOtaSimulationDialog = MutableStateFlow(false)

    // Derived flows
    val isCloudConnected = repository.isCloudConnected
    val otaLogs = repository.otaLogs
    val syncProgress = repository.syncProgress
    val latencyMs = repository.latencyMs

    // Live list flows
    val allFolders = repository.foldersFlow
    val allMediaObjects = repository.mediaObjectsFlow

    // Current levels filtered by hierarchical folder tree
    val currentFolders: StateFlow<List<FolderEntity>> = combine(
        allFolders,
        activeFolderId
    ) { folders, activeId ->
        folders.filter { it.parentId == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMediaObjects: StateFlow<List<MediaObjectEntity>> = combine(
        allMediaObjects,
        activeFolderId,
        searchQuery
    ) { mediaList, activeId, query ->
        mediaList.filter {
            // If searching globally, ignore hierarchy, matching query. Else match hierarchy + query.
            val matchesFolder = if (query.isEmpty()) it.folderId == activeId else true
            val matchesQuery = it.name.contains(query, ignoreCase = true) || 
                               it.infoTag.contains(query, ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of full folder path for breadcrumbs
    val folderNavigationPath: StateFlow<List<FolderEntity>> = combine(
        allFolders,
        activeFolderId
    ) { folders, activeId ->
        if (activeId == null) {
            emptyList()
        } else {
            val path = mutableListOf<FolderEntity>()
            var currentId: String? = activeId
            val foldersMap = folders.associateBy { it.id }
            while (currentId != null) {
                val folder = foldersMap[currentId]
                if (folder != null) {
                    path.add(0, folder)
                    currentId = folder.parentId
                } else {
                    currentId = null
                }
            }
            path
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived AES-256 SecretKeySpec
    val activeSecretKey: StateFlow<SecretKeySpec> = passphrase
        .map { EncryptionHelper.deriveKey(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            EncryptionHelper.deriveKey("admin123")
        )

    init {
        // Run database seeding on first launch if everything is empty
        viewModelScope.launch {
            repository.addLog("Initializing Secure E2EE System...")
            repository.foldersFlow.first().let { folders ->
                if (folders.isEmpty()) {
                    repository.addLog("Secure Vault SQLite is empty. Injecting seed data...")
                    repository.generateSeedData(passphrase.value)
                } else {
                    repository.addLog("Secure Vault structure loaded. Waiting for password verification.")
                }
            }
        }
    }

    // Actions
    fun updatePassphrase(newPassphrase: String) {
        passphrase.value = newPassphrase
        repository.addLog("E2E derived secret master key re-generated.")
    }

    fun selectFolder(folderId: String?) {
        activeFolderId.value = folderId
        searchQuery.value = "" // clear search on directory change
    }

    fun selectMedia(item: MediaObjectEntity?) {
        selectedMediaItem.value = item
    }

    fun setQuery(query: String) {
        searchQuery.value = query
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name, activeFolderId.value)
        }
    }

    fun createMediaObject(name: String, content: String, mimeType: String, tag: String) {
        viewModelScope.launch {
            repository.createMediaObject(
                name = name,
                folderId = activeFolderId.value,
                plainContent = content,
                mimeType = mimeType,
                passphraseOnlyIfE2EE = passphrase.value,
                infoTag = tag
            )
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (activeFolderId.value == folderId) {
                activeFolderId.value = null
            }
        }
    }

    fun deleteMediaObject(mediaId: String) {
        viewModelScope.launch {
            repository.deleteMedia(mediaId)
            if (selectedMediaItem.value?.id == mediaId) {
                selectedMediaItem.value = null
            }
        }
    }

    fun triggerOtaSimulation(channel: String, customPassphraseForOta: String) {
        viewModelScope.launch {
            repository.runOtaPushSimulation(channel, customPassphraseForOta)
        }
    }

    fun toggleCloudConnection() {
        viewModelScope.launch {
            repository.setCloudConnection(!isCloudConnected.value)
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearAllData()
            activeFolderId.value = null
            selectedMediaItem.value = null
        }
    }

    fun rebuildSeedData() {
        viewModelScope.launch {
            repository.generateSeedData(passphrase.value)
            activeFolderId.value = null
            selectedMediaItem.value = null
        }
    }

    /**
     * Helper to decrypt a media object dynamically using the current derived secret key.
     */
    fun decryptMediaItem(item: MediaObjectEntity): String {
        val key = activeSecretKey.value
        return EncryptionHelper.decrypt(item.encryptedContentBase64, key)
    }
}
