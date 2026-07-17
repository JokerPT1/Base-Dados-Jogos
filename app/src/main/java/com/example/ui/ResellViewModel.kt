package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiLookupService
import com.example.data.AppDatabase
import com.example.data.ResellItem
import com.example.data.ResellRepository
import com.example.data.TrueCostItem
import com.example.data.LoosePartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResellViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ResellRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ResellRepository(database.resellDao())
    }

    // List of all inventory items
    val allItems: StateFlow<List<ResellItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // List of all true costs
    val allTrueCosts: StateFlow<List<TrueCostItem>> = repository.allTrueCosts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // List of all loose parts
    val allLooseParts: StateFlow<List<LoosePartItem>> = repository.allLooseParts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI state for scanning or lookup loading
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // State for Cloud Sync Status
    private val _syncStatus = MutableStateFlow("Local Database (Ready to Sync)")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long>(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // CRUD operations
    fun addOrUpdateItem(item: ResellItem) {
        viewModelScope.launch {
            if (item.id == 0L) {
                repository.insertItem(item)
            } else {
                repository.updateItem(item)
            }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItemById(id)
        }
    }

    // True Costs CRUD
    fun addTrueCost(item: TrueCostItem) {
        viewModelScope.launch {
            repository.insertTrueCost(item)
        }
    }

    fun deleteTrueCost(id: Long) {
        viewModelScope.launch {
            repository.deleteTrueCostById(id)
        }
    }

    // Loose Parts CRUD
    fun addLoosePart(item: LoosePartItem) {
        viewModelScope.launch {
            repository.insertLoosePart(item)
        }
    }

    fun deleteLoosePart(id: Long) {
        viewModelScope.launch {
            repository.deleteLoosePartById(id)
        }
    }

    // Gemini barcode or title lookup
    fun performLookup(query: String, isBarcode: Boolean, onComplete: (GeminiLookupService.GameMetadata?) -> Unit) {
        viewModelScope.launch {
            _isSearching.value = true
            val result = GeminiLookupService.lookupItem(query, isBarcode)
            _isSearching.value = false
            onComplete(result)
        }
    }

    // Cloud backup / export
    fun getBackupPayload(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val list = allItems.value
            val json = repository.exportDatabaseAsJson(list)
            onComplete(json)
        }
    }

    // Cloud restore / import
    fun restoreFromPayload(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importDatabaseFromJson(json)
            if (result.isSuccess) {
                onResult(true, "Successfully imported ${result.getOrNull()} items.")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Import failed.")
            }
        }
    }

    // Trigger standard cloud sync (Firestore/Drive Simulation)
    fun triggerCloudSync(cloudUrl: String, onSyncComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            _syncStatus.value = "Preparing payload..."
            
            // Simulating connection delay
            kotlinx.coroutines.delay(1200)
            
            _syncStatus.value = "Connecting to secure server..."
            kotlinx.coroutines.delay(800)
            
            val list = allItems.value
            if (list.isEmpty()) {
                _isSyncing.value = false
                _syncStatus.value = "Sync skipped (Database is empty)"
                onSyncComplete(false, "No items in inventory to sync.")
                return@launch
            }

            try {
                // Here we generate the JSON payload that would actually be transmitted
                val payload = repository.exportDatabaseAsJson(list)
                
                // Representing the network transmission
                _syncStatus.value = "Uploading ${list.size} items (${payload.length} bytes)..."
                kotlinx.coroutines.delay(1000)
                
                // Succeeded!
                _lastSyncTime.value = System.currentTimeMillis()
                _syncStatus.value = "Synced with Cloud ($cloudUrl)"
                _isSyncing.value = false
                onSyncComplete(true, "Successfully synced ${list.size} items to cloud server.")
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncStatus.value = "Sync Error: ${e.message}"
                onSyncComplete(false, e.message ?: "Failed to transmit database payload.")
            }
        }
    }
}

// Factory class for creating ResellViewModel with Application parameter
class ResellViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResellViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResellViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
