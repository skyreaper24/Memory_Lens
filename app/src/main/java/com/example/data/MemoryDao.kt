package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    fun getAllMemoryItems(): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    suspend fun getAllMemoryItemsList(): List<MemoryItem>

    @Query("SELECT * FROM memory_items WHERE id = :id LIMIT 1")
    suspend fun getMemoryItemById(id: Long): MemoryItem?

    @Query("SELECT * FROM memory_items WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%'")
    suspend fun searchMemoryItems(query: String): List<MemoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryItem(item: MemoryItem): Long

    @Delete
    suspend fun deleteMemoryItem(item: MemoryItem)

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteMemoryItemById(id: Long)

    // --- Concept Node Queries ---
    @Query("SELECT * FROM concept_nodes")
    fun getAllConceptNodes(): Flow<List<ConceptNode>>

    @Query("SELECT * FROM concept_nodes WHERE name = :name LIMIT 1")
    suspend fun getConceptNodeByName(name: String): ConceptNode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConceptNode(node: ConceptNode): Long

    // --- Relationship Links ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryConceptLink(link: MemoryConceptCrossRef)

    @Query("""
        SELECT * FROM concept_nodes 
        INNER JOIN memory_concept_links ON concept_nodes.id = memory_concept_links.conceptId 
        WHERE memory_concept_links.memoryId = :memoryId
    """)
    suspend fun getConceptsForMemory(memoryId: Long): List<ConceptNode>

    @Query("SELECT * FROM edge_connections")
    fun getAllConnections(): Flow<List<EdgeConnection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdgeConnection(connection: EdgeConnection): Long
}
