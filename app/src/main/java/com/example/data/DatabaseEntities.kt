package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_items")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val summary: String,
    val sourceType: String, // "pdf" | "image" | "url" | "note" | "audio"
    val sourceUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val cognitiveTagsJson: String = "[]", // JSON string of tags: ["productivity", "mindset"]
    val actionItemsJson: String = "[]",  // JSON string of task strings: ["Read 10 mins Daily"]
    val flashcardsJson: String = "[]"    // JSON string of objects: [{"q":"...","a":"..."}]
)

@Entity(tableName = "concept_nodes")
data class ConceptNode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "James Clear" or "Habit Formation"
    val category: String, // "topic" | "person" | "book" | "tool" | "company"
    val importance: Float = 1.0f // Visual weight multiplier
)

@Entity(tableName = "memory_concept_links", primaryKeys = ["memoryId", "conceptId"])
data class MemoryConceptCrossRef(
    val memoryId: Long,
    val conceptId: Long
)

@Entity(tableName = "edge_connections")
data class EdgeConnection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val targetId: Long,
    val relationshipType: String // "mentions" | "relatedTo" | "authorOf" | "partOf"
)
