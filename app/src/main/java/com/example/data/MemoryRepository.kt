package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MemoryRepository(private val dao: MemoryDao) {

    val allMemoryItems: Flow<List<MemoryItem>> = dao.getAllMemoryItems()
    val allConceptNodes: Flow<List<ConceptNode>> = dao.getAllConceptNodes()
    val allConnections: Flow<List<EdgeConnection>> = dao.getAllConnections()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    private val flashcardListType = Types.newParameterizedType(List::class.java, GeminiFlashcard::class.java)
    private val flashcardListAdapter = moshi.adapter<List<GeminiFlashcard>>(flashcardListType)

    suspend fun insertMemory(item: MemoryItem): Long = withContext(Dispatchers.IO) {
        dao.insertMemoryItem(item)
    }

    suspend fun deleteMemoryById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteMemoryItemById(id)
    }

    suspend fun searchLocal(query: String): List<MemoryItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        dao.searchMemoryItems(query)
    }

    /**
     * Call Gemini to enrich, and assemble unified relations inside Room
     */
    suspend fun addMemoryWithEnrichment(
        rawText: String,
        sourceType: String,
        sourceUri: String? = null
    ): Long = withContext(Dispatchers.IO) {
        // 1. Call API Gate
        val enrichment = GeminiNetworkGateway.enrichContent(rawText, sourceType)

        val tagsJson = stringListAdapter.toJson(enrichment.concepts.map { it.name })
        val actionsJson = stringListAdapter.toJson(enrichment.actionItems)
        val flashcardsJson = flashcardListAdapter.toJson(enrichment.flashcards)

        // 2. Write main item
        val memoryItem = MemoryItem(
            title = enrichment.title,
            content = rawText,
            summary = enrichment.summary,
            sourceType = sourceType,
            sourceUri = sourceUri,
            cognitiveTagsJson = tagsJson,
            actionItemsJson = actionsJson,
            flashcardsJson = flashcardsJson,
            timestamp = System.currentTimeMillis()
        )
        val memoryId = dao.insertMemoryItem(memoryItem)

        // 3. Connect nodes in graph
        val createdConceptIds = mutableListOf<Long>()
        enrichment.concepts.forEach { item ->
            val existing = dao.getConceptNodeByName(item.name)
            val conceptId = if (existing != null) {
                existing.id
            } else {
                dao.insertConceptNode(ConceptNode(name = item.name, category = item.category))
            }
            createdConceptIds.add(conceptId)

            // Cross reference memory <-> concept
            dao.insertMemoryConceptLink(MemoryConceptCrossRef(memoryId, conceptId))
        }

        // 4. Create semantic connections in graph among siblings to build visual nodes
        for (i in 0 until createdConceptIds.size) {
            for (j in i + 1 until createdConceptIds.size) {
                val source = createdConceptIds[i]
                val target = createdConceptIds[j]
                dao.insertEdgeConnection(
                    EdgeConnection(
                        sourceId = source,
                        targetId = target,
                        relationshipType = "relatedTo"
                    )
                )
            }
        }

        memoryId
    }

    /**
     * Gather database contents to build prompt contexts
     */
    suspend fun getFullMemoryContextForChat(): String = withContext(Dispatchers.IO) {
        val builder = java.lang.StringBuilder()
        try {
            val allItems = dao.getAllMemoryItemsList()
            if (allItems.isEmpty()) {
                builder.append("No saved memories present in the Second Brain yet. The user should start by saving their first memory item.")
            } else {
                allItems.forEachIndexed { index, item ->
                    builder.append("Memory #${index + 1}:\n")
                    builder.append("- Title: ${item.title}\n")
                    builder.append("- Source Type: ${item.sourceType}\n")
                    builder.append("- Key Summary: ${item.summary}\n")
                    builder.append("- Raw Content: ${item.content}\n")
                    builder.append("- Cognitive Tags: ${item.cognitiveTagsJson}\n")
                    builder.append("----------------------------\n")
                }
            }
        } catch (e: Exception) {
            builder.append("Error retrieving context history: ${e.message}")
        }
        return@withContext builder.toString()
    }
}
