# MemoryLens — System Architecture & Product Blueprint
**Tagline:** Never Lose Knowledge Again.
**Document Version:** 1.0.0
**Author:** AI Product & Systems Architecture Team

---

## 1. Product Strategy
MemoryLens is positioned not as another manual note-taking app or file organization utility, but as an **on-device cognitive prosthesis**. Modern digital lives create a firehose of fragmented knowledge (YouTube transcripts, PDFs, quick web-clippings, audio files, photos, thoughts). Traditional productivity suites fail because they demand *active maintenance* (manual categorization, filing into folders, tagging). MemoryLens breaks this paradigm by using **proactive AI ambient synthesis** to automatically build a semantic connection layer without human effort.

### Core Value Propositions:
- **Zero-Effort Curation:** The system infers tags, concepts, related books, people, and actions purely from standard input streams (text, images, PDFs, URLs).
- **Infinite Retrieval:** Natural language queries retrieve precise highlights, even if the user only remembers contextual fragments (e.g., *"What was that thing about dopamine habits I read last month?"*).
- **Dynamic Context (The Second Brain):** The knowledge graph represents the digital layout of the user’s mind, linking a book outline to a voice note to a code fragment.

---

## 2. System Design
MemoryLens utilizes an **offline-first, client-heavy hub architecture** on Android. Local cache stability and user privacy are prioritized, with the Gemini API orchestrating structured conceptual mapping.

```
       [ USER INPUTS: PDF, Web link, Screenshot, Text, Voice ]
                               |
                               v
                       [ Processing Hub ]
                     /         |        \
    [ Local Document Cache ]   |   [ Gemini API (Prototyping REST) ]
                               |         | (Analysis & Enrichment)
                               v         v
                     [ Room Database Engine ]
                     - Ingestion Entities
                     - Concept Nodes
                     - Semantic Links/Edges
                     /         |        \
     [ Semantic Search ]  [ Timeline ]  [ Mind Map Node Canvas ]
```

### Components:
1. **Ingestion Engine:** Standardized ingestion adapter converting text, rich URLs, and images/drawables into uniform memory units.
2. **AI Enrichment Pipeline:** Retrofit client calling `gemini-3.5-flash` to process unstructured input into standard JSON mapping out:
   - Topic tags
   - Summary and bullet facts
   - Identified entities (People, Books, Tools, Companies)
   - Actions and Flashcards
   - Recommended connections
3. **Knowledge Graph Engine (Room):** Local persistence of Memory Items, Tags, Concepts, and Connection Edges.
4. **Interactive Mind Canvas:** Custom 2D canvas enabling pan-and-scan graphic explorations of the memory network.
5. **Cognitive Chat Hub:** Continuous dialogue screen allowing users to query, summarize, and cross-examine their saved data.

---

## 3. Architecture
The codebase strictly adheres to the modern **MVVM (Model-View-ViewModel)** architectural pattern, following standard Android Clean Architecture principles.

- **Data Layer:**
  - `MemoryDatabase`: Room Database holding entities.
  - `Moshi`: Handles serialization for metadata.
  - `MemoryDao` & `GraphDao`: Manage transactions and select queries.
  - `MemoryRepository`: Unifies network (Gemini API) and local database transactions into unified Flow resources.
- **Domain Layer:**
  - Standard Kotlin data models (`MemoryItem`, `ConceptNode`, `EdgeConnection`).
  - Input parsing schemas for standard web-scraping or document generation.
- **UI Layer (Jetpack Compose):**
  - Navigation Graph (`MemoryNavGraph`) separating concerns across screens:
    - **Dashboard Stream** (Adding, searching, and overview statistics)
    - **Memory Timeline** (Temporal map of inputs)
    - **Mind Graph Canvas** (Visual relationship node graph explorer)
    - **Cognitive Chat** (Conversational search & roadmap creator)
  - `MemoryViewModel`: Handles state orchestration via `MutableStateFlow` to guarantee reactive UI rendering.

---

## 4. Knowledge Graph Design
A highly optimized SQLite relationship model represents the connection between diverse knowledge notes.

### Schema Blueprint (SQLite Entities & Relations):

#### MemoryItem (Table `memory_items`)
- `id` (Long, PK)
- `title` (String)
- `content` (String)
- `summary` (String)
- `sourceType` (String: "pdf" | "image" | "url" | "note" | "audio")
- `sourceUri` (String?)
- `timestamp` (Long)
- `cognitiveTagList` (String -> JSON-serialized tag list)
- `actionItems` (String -> JSON-serialized action tasks)
- `flashcards` (String -> JSON-serialized quiz cards)

#### ConceptNode (Table `concept_nodes`)
- `id` (Long, PK)
- `name` (String, Unique)
- `category` (String: "topic" | "person" | "book" | "tool" | "company")

#### MemoryConceptCrossRef (Table `memory_concept_links`)
- `memoryId` (Long)
- `conceptId` (Long)
*Composite PK: (memoryId, conceptId)*

#### EdgeConnection (Table `edge_connections`)
- `id` (Long, PK)
- `sourceId` (Long) -> ConceptNode or MemoryItem ID
- `targetId` (Long) -> ConceptNode or MemoryItem ID
- `relationshipType` (String: "mentions" | "relatedTo" | "authorOf" | "partOf")

---

## 5. AI Enrichment Pipeline
The prompt framework utilizes the `gemini-3.5-flash` model via its direct JSON mode to convert unstructured content into a standard schema.

### Ingestion Flow:
1. User creates a memory item (e.g., enters a note or pasts a clipboard link).
2. The pipeline fires an asynchronous worker task carrying the custom prompt instructions.
3. System Instructions dictate:
   - Strict JSON generation format.
   - Intelligent connection logic (e.g., if content discusses "dopamine" and "sleep", output those as concepts).
4. The output matches a Kotlin `@Serializable` response frame, enabling direct mapping back into local database items.

---

## 6. Search Architecture
Retrieval is structured in a two-tier hybrid architecture to ensure instant speed:

- **Tier 1 (Lexical Filter):** Direct database query executing standard `LIKE` expressions and keyword-indexed filters against memory text, titles, summaries, and associated concept tags.
- **Tier 2 (AI Semantic Query / Chat context):** When a user triggers the Cognitive Chat, the app loads relevant memory notes matching the conversation history into the model's context window. This empowers the user to converse directly with their database.

---

## 7. UI System
Underpinned by a premium **"Cosmic Slate" (Luxury Dark)** design language to cultivate a sense of mystery and organic intelligence.

- **Palette:** Deep void backdrops, glowing nebula violet accents, muted slate grey cards, and high-contrast typography.
- **Micro-interactions:** Responsive custom ripples, smooth transition sliders, and dynamic item additions with spring animations.
- **Mind Canvas:** Zoomable 2D floating physics-simulated node map canvas rendering concepts and entries linked together with clean SVG-like bezier paths.

---

## 8. Security & Privacy Model
Privacy is not an added checkbox; it is the fundamental core of the MemoryLens architectural foundation.

- **On-Device Sandbox:** All files, text caches, web content extracts, and PDFs remain isolated within private storage sandboxes on Android. No background trackers share telemetry.
- **Secure Key Injection:** API keys reside inside system environmental secrets, injected into `BuildConfig` at compile-time. No plain-text secrets enter version repositories.
- **Absolute Ownership:** The SQLite architecture guarantees easy exports, letting the user back up their own SQLite container whenever desired.

---

## 9. Development Roadmap

### Phase 1: Core Foundation & Room Architecture (Current Turn)
- Apply dependencies for Navigation, Extended Icons, and Web Image Loading.
- Write Room Schemas, Entity models, and Daos.
- Establish the Ingestion Adapter and `RetrofitClient` with timeout parameters.

### Phase 2: Dynamic Mind View & Graph Canvas
- Build custom physics layout nodes rendering on-canvas elements with gestures.
- Implement the Timeline progression charts.

### Phase 3: Conversational Mind Chat & Insights
- Establish the live conversation stream matching database entries with context blocks.
- Create automated weekly Learning Reports mapping knowledge growth.

---

## 10. Technical Specifications & Payload Contract
To ensure the Gemini model is returned in precise schemas, we mandate sending specific custom system instructions:

```json
{
  "title": "Topic Title",
  "summary": "High level brief summary...",
  "concepts": [
    { "name": "Atomic Habits", "category": "book" },
    { "name": "James Clear", "category": "person" },
    { "name": "Habit Formation", "category": "topic" }
  ],
  "actionItems": [
    "Create a daily 1% habit framework"
  ],
  "flashcards": [
    { "question": "What is James Clear's baseline rule?", "answer": "1% improvement daily accumulates exponentially." }
  ]
}
```
This payload is parsed using the robust Moshi parser client and mapped straight into the respective database repositories.
