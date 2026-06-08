package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MemoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MemoryDatabase.getDatabase(application)
    private val repository = MemoryRepository(database.memoryDao())

    val memoryItems: StateFlow<List<MemoryItem>> = repository.allMemoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conceptNodes: StateFlow<List<ConceptNode>> = repository.allConceptNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connections: StateFlow<List<EdgeConnection>> = repository.allConnections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI parameters
    val activeTab = MutableStateFlow("dashboard")
    val userQuery = MutableStateFlow("")
    val filterSourceType = MutableStateFlow("all")
    val selectedItem = MutableStateFlow<MemoryItem?>(null)

    // Ingestion state
    val showAddDialog = MutableStateFlow(false)
    val addMemoryText = MutableStateFlow("")
    val addMemorySourceType = MutableStateFlow("note")
    val isAnalyzing = MutableStateFlow(false)

    // Chat states
    val chatInput = MutableStateFlow("")
    val chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            "Hello! I am your MemoryLens Cognitive Companion. Let's discuss, study, or explore your Second Brain. Ask me anything!" to false
        )
    )
    val isChatThinking = MutableStateFlow(false)

    // Filtered memories list based on search or categories
    val filteredMemories: StateFlow<List<MemoryItem>> = combine(
        memoryItems, userQuery, filterSourceType
    ) { items, query, source ->
        items.filter { item ->
            val matchesSource = source == "all" || item.sourceType.lowercase() == source.lowercase()
            val matchesQuery = query.isBlank() || 
                    item.title.contains(query, ignoreCase = true) || 
                    item.content.contains(query, ignoreCase = true) || 
                    item.summary.contains(query, ignoreCase = true)
            matchesSource && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectItem(item: MemoryItem?) {
        selectedItem.value = item
    }

    fun setSourceFilter(source: String) {
        filterSourceType.value = source
    }

    fun ingestMemory() {
        val text = addMemoryText.value.trim()
        val type = addMemorySourceType.value
        if (text.isEmpty()) return

        viewModelScope.launch {
            isAnalyzing.value = true
            try {
                repository.addMemoryWithEnrichment(text, type)
                addMemoryText.value = ""
                showAddDialog.value = false
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isAnalyzing.value = false
            }
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemoryById(id)
            if (selectedItem.value?.id == id) {
                selectedItem.value = null
            }
        }
    }

    fun submitChatMessage() {
        val message = chatInput.value.trim()
        if (message.isEmpty() || isChatThinking.value) return

        // Save entry
        val currentHistory = chatMessages.value.toMutableList()
        currentHistory.add(message to true)
        chatMessages.value = currentHistory
        chatInput.value = ""
        isChatThinking.value = true

        viewModelScope.launch {
            try {
                // Get memory context from the database
                val contextText = repository.getFullMemoryContextForChat()
                val response = GeminiNetworkGateway.chatWithMemory(
                    history = currentHistory.dropLast(1),
                    currentMessage = message,
                    contextText = contextText
                )
                val updatedHistory = chatMessages.value.toMutableList()
                updatedHistory.add(response to false)
                chatMessages.value = updatedHistory
            } catch (e: Exception) {
                val updatedHistory = chatMessages.value.toMutableList()
                updatedHistory.add("Cognitive Error: unable to fetch response. ${e.message}" to false)
                chatMessages.value = updatedHistory
            } finally {
                isChatThinking.value = false
            }
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            isAnalyzing.value = true
            try {
                // Seed 1: Atomic Habits
                repository.addMemoryWithEnrichment(
                    rawText = "I finished reading Atomic Habits by James Clear. Key concept is that getting 1% better daily leads to 37x growth over a year. Focus on systems rather than goals. True change comes from identity-shifting habits rather than outcome triggers.",
                    sourceType = "book"
                )
                
                // Seed 2: Dopamine Focus video
                repository.addMemoryWithEnrichment(
                    rawText = "Watched Huberman podcast on Dopamine control. To maintain focus, do not anchor dopamine spikes exclusively to output rewards. Build love for friction processes. Deep work session limits should hover around 90-minute blocks.",
                    sourceType = "url"
                )

                // Seed 3: Cybersecurity Note
                repository.addMemoryWithEnrichment(
                    rawText = "Note to self: Implement Zero Trust Architecture in the backend API. Treat every service call as hostile. Enforce JWT tokens, secure HTTPS scopes, and hide raw database details behind Firebase functions.",
                    sourceType = "note"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isAnalyzing.value = false
            }
        }
    }
}
