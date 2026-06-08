package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiEnrichmentResponse(
    val title: String,
    val summary: String,
    val concepts: List<GeminiConcept> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val flashcards: List<GeminiFlashcard> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeminiConcept(
    val name: String,
    val category: String // "topic" | "person" | "book" | "tool" | "company"
)

@JsonClass(generateAdapter = true)
data class GeminiFlashcard(
    val question: String,
    val answer: String
)
