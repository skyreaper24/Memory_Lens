package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiNetworkGateway {
    private const val TAG = "GeminiGateway"
    
    // Check if key is configured
    val isKeyConfigured: Boolean
        get() {
            val key = BuildConfig.GEMINI_API_KEY
            return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
        }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val responseAdapter = moshi.adapter(GeminiEnrichmentResponse::class.java)

    /**
     * Enriches unstructured input into a structured payload containing:
     * title, summary, concepts, action items, and flashcards.
     */
    suspend fun enrichContent(content: String, sourceType: String): GeminiEnrichmentResponse {
        if (!isKeyConfigured) {
            Log.w(TAG, "API key is missing or is placeholder. Falling back to local/mock semantic parsing.")
            return generateMockEnrichment(content, sourceType)
        }

        val prompt = """
            Analyze the following input. It comes from a source of type "$sourceType".
            Generate:
            1. An elegant, concise title that summarizes this input.
            2. A clear, intellectual 2-3 sentence summary of the key takeaway.
            3. A list of concept nodes related to this content. Each concept should have:
               - "name": The short noun (e.g., "James Clear", "Habit Formation", "dopamine", "Android", "Spanner").
               - "category": Categorize precisely as "topic", "person", "book", "tool", or "company" (lowercase only).
            4. Outstanding action items or key takeaways (as simple list of strings, max 3 items).
            5. Outstanding study flashcards to help remember this (max 2 items), each having a "question" and "answer".

            Return your response ONLY as a clean JSON object following this JSON schema (do not wrap in markdown code blocks, do not output any other text):
            {
              "title": "...",
              "summary": "...",
              "concepts": [{"name": "...", "category": "topic/person/book/tool/company"}],
              "actionItems": ["...", "..."],
              "flashcards": [{"question": "...", "answer": "..."}]
            }

            CONTENT TO ANALYZE:
            $content
        """.trimIndent()

        // Build Gemini Request Payload manual serialization to guarantee robustness
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val mediaType = "application/json".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)
        val apiKey = BuildConfig.GEMINI_API_KEY
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected HTTP code: ${response.code} - ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw IOException("Empty response body")
                Log.d(TAG, "Raw response: $bodyString")
                
                // Parse the native Gemini Response JSON, extract the payload text inside content.parts[0].text
                val rootJson = JSONObject(bodyString)
                val candidates = rootJson.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                responseAdapter.fromJson(textResponse) ?: throw IOException("Failed to parse inner response JSON")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed, falling back to local simulation", e)
            generateMockEnrichment(content, sourceType)
        }
    }

    /**
     * Conducts a conversational Q&A against saved notes and details.
     */
    suspend fun chatWithMemory(history: List<Pair<String, Boolean>>, currentMessage: String, contextText: String): String {
        if (!isKeyConfigured) {
            return "Local AI Brain: I see you are asking about your memories. Once you introduce a valid Gemini API Key, I can integrate your entire database. Currently, I see your query is: \"$currentMessage\" and I have access to ${contextText.length} characters of memory context!"
        }

        // Build continuous contents list
        val contentsArray = JSONArray()

        // System Instruction Content
        val systemInstruction = """
            You are the MemoryLens Cognitive Interface, an elite AI memory companion of the user's Second Brain.
            You have access to the user's digital life database below. Use it to answer their queries precisely, warmly, and thoroughly.
            If the user asks about people, concepts, books, or notes from their database, use the memory context to enrich your answer.
            
            YOUR ACCESSIBLE MEMORY CONTEXT DATABASE:
            $contextText
        """.trimIndent()

        val fullPrompt = StringBuilder()
        fullPrompt.append("System Context:\n").append(systemInstruction).append("\n\n")
        fullPrompt.append("Conversation History:\n")
        history.forEach { (msg, isUser) ->
            val role = if (isUser) "User" else "AI Companion"
            fullPrompt.append("$role: $msg\n")
        }
        fullPrompt.append("User Query: $currentMessage\n\nProvide an elegant, helpful, and insightful response.")

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", fullPrompt.toString()) })
                    })
                })
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val apiKey = BuildConfig.GEMINI_API_KEY
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "Error from Gemini API: ${response.code} (Check internet and quota limits)"
                }
                val bodyString = response.body?.string() ?: return "Error: empty response"
                val rootJson = JSONObject(bodyString)
                rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            "Error: unable to reach Gemini API. Ensure you have network access. Exception: ${e.message}"
        }
    }

    private fun generateMockEnrichment(content: String, sourceType: String): GeminiEnrichmentResponse {
        var cleanContent = content.trim()
        if (cleanContent.isEmpty()) {
            cleanContent = "Saved memory insight"
        }

        // Identify concepts
        val lowercaseContent = cleanContent.lowercase()
        val detectedConceptsList = mutableListOf<GeminiConcept>()
        
        if (lowercaseContent.contains("habit") || lowercaseContent.contains("growth") || lowercaseContent.contains("atomic")) {
            detectedConceptsList.add(GeminiConcept("Habit Formation", "topic"))
            detectedConceptsList.add(GeminiConcept("James Clear", "person"))
            detectedConceptsList.add(GeminiConcept("Atomic Habits", "book"))
        }
        if (lowercaseContent.contains("focus") || lowercaseContent.contains("dopamine") || lowercaseContent.contains("brain")) {
            detectedConceptsList.add(GeminiConcept("Dopamine regulation", "topic"))
            detectedConceptsList.add(GeminiConcept("Deep Work", "topic"))
            detectedConceptsList.add(GeminiConcept("Cal Newport", "person"))
        }
        if (lowercaseContent.contains("cybersecurity") || lowercaseContent.contains("security") || lowercaseContent.contains("cloud")) {
            detectedConceptsList.add(GeminiConcept("Cybersecurity", "topic"))
            detectedConceptsList.add(GeminiConcept("Cloud Infrastructure", "topic"))
            detectedConceptsList.add(GeminiConcept("Zero Trust Architecture", "topic"))
        }
        if (lowercaseContent.contains("investing") || lowercaseContent.contains("money") || lowercaseContent.contains("finance")) {
            detectedConceptsList.add(GeminiConcept("Financial Literacy", "topic"))
            detectedConceptsList.add(GeminiConcept("Benjamin Graham", "person"))
            detectedConceptsList.add(GeminiConcept("The Intelligent Investor", "book"))
        }
        if (lowercaseContent.contains("android") || lowercaseContent.contains("kotlin") || lowercaseContent.contains("compose")) {
            detectedConceptsList.add(GeminiConcept("Android Development", "topic"))
            detectedConceptsList.add(GeminiConcept("Kotlin", "tool"))
            detectedConceptsList.add(GeminiConcept("Jetpack Compose", "tool"))
            detectedConceptsList.add(GeminiConcept("Google AI Studio", "company"))
        }

        // Fallback random helper
        if (detectedConceptsList.isEmpty()) {
            val words = cleanContent.split(Regex("\\s+"))
                .map { it.lowercase().replace(Regex("[^a-zA-Z0-9]"), "") }
                .filter { it.length > 5 && it !in setOf("should", "would", "could", "about", "their", "there", "where", "these", "those") }
                .distinct()
                .take(3)
            words.forEachIndexed { index, word ->
                val capWord = word.replaceFirstChar { it.uppercase() }
                val cat = when (index % 3) {
                    0 -> "topic"
                    1 -> "person"
                    else -> "tool"
                }
                detectedConceptsList.add(GeminiConcept(capWord, cat))
            }
        }

        if (detectedConceptsList.isEmpty()) {
            detectedConceptsList.add(GeminiConcept("Personal Knowledge Graph", "topic"))
            detectedConceptsList.add(GeminiConcept("MemoryLens", "tool"))
        }

        val cleanTitle = if (cleanContent.length > 40) {
            cleanContent.take(35) + "..."
        } else {
            cleanContent
        }

        return GeminiEnrichmentResponse(
            title = "[Simulated] $cleanTitle",
            summary = "Direct semantic synthesis of your $sourceType entry. Identifies core conceptual nodes to establish persistent memory connections within your Mind Map.",
            concepts = detectedConceptsList,
            actionItems = listOf(
                "Incorporate learnings about this $sourceType entry into daily review routine.",
                "Review visual graph connections for nodes: " + detectedConceptsList.joinToString { it.name }
            ),
            flashcards = listOf(
                GeminiFlashcard(
                    question = "For what source type was this item generated?",
                    answer = "This memory item was indexed as a $sourceType entry."
                ),
                GeminiFlashcard(
                    question = "What primary topic does this entry touch upon?",
                    answer = "Main theme: " + (detectedConceptsList.firstOrNull()?.name ?: "Knowledge Graph")
                )
            )
        )
    }
}
