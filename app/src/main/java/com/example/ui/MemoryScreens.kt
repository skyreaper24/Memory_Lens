package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MemoryAppContent(viewModel: MemoryViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            MemoryBottomBar(
                activeTab = activeTab,
                onTabSelected = { viewModel.activeTab.value = it },
                onAddClicked = { viewModel.showAddDialog.value = true }
            )
        },
        containerColor = CosmicVoid
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeTab,
                label = "ScreenSwitch"
            ) { targetTab ->
                when (targetTab) {
                    "dashboard" -> DashboardScreen(viewModel)
                    "timeline" -> TimelineScreen(viewModel)
                    "graph" -> GraphScreen(viewModel)
                    "chat" -> CognitiveChatScreen(viewModel)
                }
            }

            // Ingestion dialog
            if (showAddDialog) {
                IngestMemoryDialog(
                    viewModel = viewModel,
                    isAnalyzing = isAnalyzing,
                    onDismiss = { viewModel.showAddDialog.value = false }
                )
            }

            // Detail Panel Overlay
            selectedItem?.let { item ->
                MemoryDetailPanel(
                    item = item,
                    onDismiss = { viewModel.selectItem(null) },
                    onDelete = {
                        viewModel.deleteMemory(item.id)
                    }
                )
            }
        }
    }
}

@Composable
fun RecommendedDiscoveryBubble(memories: List<MemoryItem>) {
    val suggestion = remember(memories) {
        if (memories.size >= 2) {
            "Synthesize context: Connect \"${memories[0].title.take(18)}\" with your recent study on \"${memories[1].title.take(18)}\"?"
        } else {
            "Ingest and register more concept notes to build an interactive associative knowledge network."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(DeepIndigo.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .border(1.dp, NebulaViolet.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(NebulaViolet.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(TealGlow, CircleShape)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Recommended Discovery",
                    color = NebulaViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = suggestion,
                    color = OffWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: MemoryViewModel) {
    val memories by viewModel.filteredMemories.collectAsStateWithLifecycle()
    val allMemories by viewModel.memoryItems.collectAsStateWithLifecycle()
    val conceptNodes by viewModel.conceptNodes.collectAsStateWithLifecycle()
    val query by viewModel.userQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.filterSourceType.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Immersive Brand Header with Glowing Scanner Orb
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = NebulaViolet,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MemoryLens",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "PERSONAL KNOWLEDGE GRAPH",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        letterSpacing = 1.5.sp
                    )
                }
            }
            // Glowing Network Orb
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(NebulaViolet.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, NebulaViolet.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(NebulaViolet, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Overlay bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSlate, RoundedCornerShape(24.dp))
                .border(1.dp, GhostWhite, RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "SAVED METADATA", fontSize = 9.sp, color = SlateGray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(text = "${allMemories.size}", fontSize = 20.sp, color = NebulaViolet, fontWeight = FontWeight.ExtraBold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "CONCEPT NODES", fontSize = 9.sp, color = SlateGray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(text = "${conceptNodes.size}", fontSize = 20.sp, color = TealGlow, fontWeight = FontWeight.ExtraBold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "COGNITIVE INDEX", fontSize = 9.sp, color = SlateGray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                val pct = if (allMemories.isEmpty()) 0 else (allMemories.size * 18 + 12) % 100
                Text(text = "$pct%", fontSize = 20.sp, color = CoralGlow, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Block with Glowing ambient background gradient
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 1.dp)
                    .background(
                        Brush.horizontalGradient(listOf(NebulaViolet, DeepIndigo)),
                        RoundedCornerShape(18.dp)
                    )
                    .graphicsLayer(alpha = 0.15f)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.userQuery.value = it },
                placeholder = { Text("Search your mind space...", color = SlateGray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, tint = NebulaViolet) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bar_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = DeepSlate.copy(alpha = 0.9f),
                    unfocusedContainerColor = DeepSlate.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Recommended Discovery Insight Bubble from Immersive UI Spec
        RecommendedDiscoveryBubble(memories = memories)

        Spacer(modifier = Modifier.height(14.dp))

        // Capsules slider for sources
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sources = listOf("all", "note", "book", "url", "image", "audio")
            sources.forEach { src ->
                val isSelected = activeFilter == src
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) NebulaViolet else DeepSlate,
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, if (isSelected) NebulaViolet else GhostWhite, RoundedCornerShape(20.dp))
                        .clickable { viewModel.setSourceFilter(src) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = src.uppercase(),
                        color = if (isSelected) Color.Black else OffWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feed block
        if (allMemories.isEmpty()) {
            EmptyWorkspaceView(onSeed = { viewModel.seedSampleData() })
        } else if (memories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching concept highlights found.\nTry broadening your search query.",
                    color = SlateGray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("memories_list")
            ) {
                items(memories) { memo ->
                    MemoryItemRowCard(item = memo, onClick = { viewModel.selectItem(memo) })
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun MemoryItemRowCard(item: MemoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("memory_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = DeepSlate),
        shape = RoundedCornerShape(14.dp),
        border = borderStrokeLight()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(ElevatedSlate, CircleShape)
                            .padding(8.dp)
                    ) {
                        val icon = when (item.sourceType.lowercase()) {
                            "book" -> Icons.Default.MenuBook
                            "url" -> Icons.Default.Link
                            "image" -> Icons.Default.Image
                            "audio" -> Icons.Default.Mic
                            else -> Icons.Default.NoteAlt
                        }
                        Icon(icon, contentDescription = null, tint = NebulaViolet, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.sourceType.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = TealGlow,
                        letterSpacing = 1.sp
                    )
                }
                
                // Formatted simple Date
                val dateStr = remember(item.timestamp) {
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    sdf.format(Date(item.timestamp))
                }
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = SlateGray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.summary,
                fontSize = 13.sp,
                color = SlateGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Display some tags directly on the card
            val tags = remember(item.cognitiveTagsJson) {
                try {
                    val cleaned = item.cognitiveTagsJson.trim()
                    if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                        cleaned.substring(1, cleaned.length - 1)
                            .split(",")
                            .map { it.trim().removeSurrounding("\"") }
                            .filter { it.isNotBlank() }
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(ElevatedSlate, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "#$tag", color = SlateGray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyWorkspaceView(onSeed: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = SlateGray,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Second Brain Is Pure",
                fontSize = 20.sp,
                color = OffWhite,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Begin capturing notes, web connections, or book concepts to populate your visual interactive Memory Map instantly.",
                fontSize = 13.sp,
                color = SlateGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            
            Button(
                onClick = onSeed,
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedSlate),
                border = borderStrokeLight(Color(0xFF2E2E3E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NebulaViolet)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Seed Intelligent Sample Memories", color = OffWhite)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestMemoryDialog(
    viewModel: MemoryViewModel,
    isAnalyzing: Boolean,
    onDismiss: () -> Unit
) {
    val text by viewModel.addMemoryText.collectAsStateWithLifecycle()
    val sourceType by viewModel.addMemorySourceType.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = { if (!isAnalyzing) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepSlate),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GhostWhite, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Augment Mind Cache",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Supply links, logs, screenshots, transcripts, or notes. Gemini will extract relationships dynamically.",
                    fontSize = 11.sp,
                    color = SlateGray,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Source Type Capsule Choice
                Text(text = "SOURCE FORMAT TYPE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateGray)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val options = listOf("note", "book", "url", "audio")
                    options.forEach { opt ->
                        val active = sourceType == opt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (active) DeepIndigo else ElevatedSlate,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, if (active) NebulaViolet else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { viewModel.addMemorySourceType.value = opt }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt.uppercase(),
                                fontSize = 10.sp,
                                color = OffWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Raw input
                OutlinedTextField(
                    value = text,
                    onValueChange = { viewModel.addMemoryText.value = it },
                    placeholder = { Text("What did you learn? Paste raw texts, lectures transcripts, quotes or logs...", color = SlateGray, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("ingest_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite,
                        focusedBorderColor = NebulaViolet,
                        unfocusedBorderColor = GhostWhite,
                        focusedContainerColor = CosmicVoid,
                        unfocusedContainerColor = CosmicVoid
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss, enabled = !isAnalyzing) {
                        Text(text = "CANCEL", color = SlateGray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { viewModel.ingestMemory() },
                        colors = ButtonDefaults.buttonColors(containerColor = NebulaViolet),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isAnalyzing && text.trim().isNotEmpty(),
                        modifier = Modifier.testTag("submit_ingest_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text(text = "ENRICH & SAVE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryDetailPanel(
    item: MemoryItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepSlate),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GhostWhite, RoundedCornerShape(18.dp))
                .padding(4.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxHeight(0.85f)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = NebulaViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Cognitive Outline", fontWeight = FontWeight.Bold, color = OffWhite, fontSize = 18.sp)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = SlateGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(text = item.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = OffWhite)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .background(ElevatedSlate, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = item.sourceType.uppercase(), color = TealGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Raw input summary
                    Text(text = "SEMANTIC SUMMARY", fontSize = 10.sp, color = SlateGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.summary,
                        color = OffWhite,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Raw Content
                    Text(text = "RAW MEMORY INPUT", fontSize = 10.sp, color = SlateGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicVoid, RoundedCornerShape(10.dp))
                            .border(1.dp, GhostWhite, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = item.content,
                            color = SlateGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Action items list
                val actions = parseJsonStringList(item.actionItemsJson)
                if (actions.isNotEmpty()) {
                    item {
                        Text(text = "PROACTIVE ACTION ITEMS", fontSize = 10.sp, color = SlateGray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(actions) { act ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TealGlow, modifier = Modifier.size(16.dp).offset(y = 2.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = act, color = OffWhite, fontSize = 13.sp, lineHeight = 16.sp)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                // Flashcards list
                val flashcards = parseJsonFlashcardList(item.flashcardsJson)
                if (flashcards.isNotEmpty()) {
                    item {
                        Text(text = "STUDY MEMORY LINKS", fontSize = 10.sp, color = SlateGray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(flashcards) { card ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(ElevatedSlate, RoundedCornerShape(10.dp))
                                .border(1.dp, GhostWhite, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CoralGlow, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Q: " + card.question, color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "A: " + card.answer, color = SlateGray, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x27FF6B6B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = CoralGlow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "DELETE FROM SECOND BRAIN", color = CoralGlow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineScreen(viewModel: MemoryViewModel) {
    val memories by viewModel.memoryItems.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Memory Timeline", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OffWhite)
        Text(text = "YOUR SYSTEMATIC KNOWLEDGE JOURNEY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateGray, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (memories.isEmpty()) {
            EmptyWorkspaceView(onSeed = { viewModel.seedSampleData() })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(memories) { memo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        // Left line timeline connector
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(NebulaViolet, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(100.dp)
                                    .background(GhostWhite)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Timeline Box Card
                        Column {
                            val timeStr = remember(memo.timestamp) {
                                val sdf = SimpleDateFormat("MMM d, yyyy @ h:mm a", Locale.getDefault())
                                sdf.format(Date(memo.timestamp))
                            }
                            Text(text = timeStr, fontSize = 11.sp, color = TealGlow, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectItem(memo) },
                                colors = CardDefaults.cardColors(containerColor = DeepSlate),
                                shape = RoundedCornerShape(12.dp),
                                border = borderStrokeLight()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = memo.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OffWhite)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = memo.summary,
                                        fontSize = 12.sp,
                                        color = SlateGray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Visual Node State holding drag variables dynamically inside the drawing canvas
class GraphNode(val concept: ConceptNode) {
    var x = mutableStateOf(0f)
    var y = mutableStateOf(0f)
}

@Composable
fun GraphScreen(viewModel: MemoryViewModel) {
    val concepts by viewModel.conceptNodes.collectAsStateWithLifecycle()
    val connections by viewModel.connections.collectAsStateWithLifecycle()

    var initialized by remember { mutableStateOf(false) }
    val graphNodes = remember { mutableStateListOf<GraphNode>() }

    // Sync state
    LaunchedEffect(concepts) {
        if (concepts.isNotEmpty()) {
            // Find existings, drop deleted, append new
            val existingIds = graphNodes.map { it.concept.id }.toSet()
            concepts.forEach { node ->
                if (node.id !in existingIds) {
                    val gNode = GraphNode(node).apply {
                        // Position circular or random spread around center
                        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                        val radius = 100f + Random.nextFloat() * 150f
                        x.value = 500f + radius * cos(angle)
                        y.value = 400f + radius * sin(angle)
                    }
                    graphNodes.add(gNode)
                }
            }
            // Filter out elements that might have been deleted
            val currentNodeIds = concepts.map { it.id }.toSet()
            graphNodes.removeAll { it.concept.id !in currentNodeIds }
            initialized = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Visual Mind Graph", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OffWhite)
        Text(text = "INTERACTIVE CONCEPT NETWORK MAP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateGray, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (concepts.isEmpty()) {
            EmptyWorkspaceView(onSeed = { viewModel.seedSampleData() })
        } else {
            Text(
                text = "Drag physical nodes to organize relationships. Floating connections depict key pathways across your memories.",
                fontSize = 12.sp,
                color = SlateGray,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Zoom transform factors
            var scale by remember { mutableStateOf(1f) }
            val transformState = rememberTransformableState { zoomChange, _, _ ->
                scale = (scale * zoomChange).coerceIn(0.5f, 2.5f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CosmicVoid, RoundedCornerShape(24.dp))
                    .border(1.dp, GhostWhite, RoundedCornerShape(24.dp))
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .transformable(state = transformState)
                    .clipToBounds()
            ) {
                // Immersive backdrop watermark center focus
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer(alpha = 0.25f)
                    ) {
                        Text(
                            text = "ACTIVE GRAPH",
                            color = SlateGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Neural Hub",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Drag links + concentric sonar scanning circles drawn behind
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerPt = Offset(size.width / 2, size.height / 2)
                    
                    // Sonar Rings
                    drawCircle(
                        color = NebulaViolet.copy(alpha = 0.08f),
                        radius = 160f,
                        center = centerPt,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                    drawCircle(
                        color = NebulaViolet.copy(alpha = 0.04f),
                        radius = 280f,
                        center = centerPt,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = NebulaViolet.copy(alpha = 0.02f),
                        radius = 420f,
                        center = centerPt,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                    )

                    connections.forEach { conn ->
                        val srcNode = graphNodes.firstOrNull { it.concept.id == conn.sourceId }
                        val trgNode = graphNodes.firstOrNull { it.concept.id == conn.targetId }
                        if (srcNode != null && trgNode != null) {
                            drawLine(
                                color = NebulaViolet.copy(alpha = 0.4f),
                                start = Offset(srcNode.x.value, srcNode.y.value),
                                end = Offset(trgNode.x.value, trgNode.y.value),
                                strokeWidth = 3f
                            )
                        }
                    }
                }

                // Interactive physical nodes rendered as draggable Composable bubbles
                graphNodes.forEach { gNode ->
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(gNode.x.value.toInt() - 65, gNode.y.value.toInt() - 30) }
                            .pointerInput(gNode) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    gNode.x.value += dragAmount.x
                                    gNode.y.value += dragAmount.y
                                }
                            }
                            .background(
                                color = DeepSlate,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = when (gNode.concept.category) {
                                    "book" -> TealGlow
                                    "person" -> CoralGlow
                                    "tool" -> DeepIndigo
                                    else -> NebulaViolet
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = gNode.concept.name,
                                color = OffWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = gNode.concept.category.uppercase(),
                                color = SlateGray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CognitiveChatScreen(viewModel: MemoryViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isChatThinking.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Cognitive Memory Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OffWhite)
        Text(text = "CONVERSE DIRECTLY WITH YOUR KNOWLEDGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateGray, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // History message log
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Reverse order logic to simplify lazy column layouts
            items(messages.reversed()) { msg ->
                val isUser = msg.second
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(
                                color = if (isUser) DeepIndigo else DeepSlate,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                )
                            )
                            .border(
                                1.dp,
                                if (isUser) NebulaViolet.copy(alpha = 0.5f) else GhostWhite,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isUser) "YOU" else "COGNITIVE ASSISTANT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) TealGlow else NebulaViolet,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = msg.first,
                                color = OffWhite,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        if (isThinking) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NebulaViolet, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Brain searching memories & reasoning...", color = SlateGray, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input send block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { viewModel.chatInput.value = it },
                placeholder = { Text("Ask summaries, quizzes or studies...", color = SlateGray, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedBorderColor = NebulaViolet,
                    unfocusedBorderColor = GhostWhite,
                    focusedContainerColor = DeepSlate,
                    unfocusedContainerColor = DeepSlate
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.submitChatMessage()
                    focusManager.clearFocus()
                },
                enabled = chatInput.isNotBlank() && !isThinking,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = NebulaViolet,
                    disabledContainerColor = ElevatedSlate
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Query",
                    tint = if (chatInput.isNotBlank() && !isThinking) Color.Black else SlateGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MemoryBottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    onAddClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSlate.copy(alpha = 0.92f), RoundedCornerShape(32.dp))
                .border(1.dp, GhostWhite, RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tab 1: Brain Stream / Map
            val tab1Selected = activeTab == "dashboard"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected("dashboard") }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (tab1Selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = null,
                    tint = if (tab1Selected) NebulaViolet else SlateGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Map",
                    color = if (tab1Selected) NebulaViolet else SlateGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tab 2: Timeline
            val tab2Selected = activeTab == "timeline"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected("timeline") }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (tab2Selected) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = null,
                    tint = if (tab2Selected) NebulaViolet else SlateGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Timeline",
                    color = if (tab2Selected) NebulaViolet else SlateGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Central Ingestion Trigger
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.verticalGradient(listOf(NebulaViolet, DeepIndigo)),
                        CircleShape
                    )
                    .clickable { onAddClicked() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ingest Entry",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Tab 3: Mind Graph
            val tab3Selected = activeTab == "graph"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected("graph") }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (tab3Selected) Icons.Filled.BubbleChart else Icons.Outlined.BubbleChart,
                    contentDescription = null,
                    tint = if (tab3Selected) NebulaViolet else SlateGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Vault",
                    color = if (tab3Selected) NebulaViolet else SlateGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tab 4: AI Chat
            val tab4Selected = activeTab == "chat"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected("chat") }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (tab4Selected) Icons.Filled.Psychology else Icons.Outlined.Psychology,
                    contentDescription = null,
                    tint = if (tab4Selected) NebulaViolet else SlateGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "AI Chat",
                    color = if (tab4Selected) NebulaViolet else SlateGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper boundary stroke decoration
fun borderStrokeLight(color: Color = GhostWhite) = androidx.compose.foundation.BorderStroke(1.dp, color)

// Moshi parser client helper functions
fun parseJsonStringList(json: String): List<String> {
    return try {
        val cleaned = json.trim()
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned.substring(1, cleaned.length - 1)
                .split(",")
                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                .filter { it.isNotBlank() }
        } else emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun parseJsonFlashcardList(json: String): List<GeminiFlashcard> {
    val list = mutableListOf<GeminiFlashcard>()
    try {
        val cleaned = json.trim()
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            // Find object blocks manually to guarantee safety across minor model glitches
            val blocks = cleaned.substring(1, cleaned.length - 1).split("},")
            blocks.forEach { block ->
                val qIdx = block.indexOf("\"question\"")
                val aIdx = block.indexOf("\"answer\"")
                if (qIdx != -1 && aIdx != -1) {
                    val qSub = block.substring(qIdx)
                    val qVal = qSub.substring(qSub.indexOf(":") + 1, qSub.indexOf(",")).trim().removeSurrounding("\"").removeSurrounding("'")
                    val aSub = block.substring(aIdx)
                    val aVal = aSub.substring(aSub.indexOf(":") + 1).trim().removeSurrounding("\"").removeSurrounding("'").removeSurrounding("}")
                    list.add(GeminiFlashcard(qVal, aVal))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

// Custom simple modifier to avoid clipping
fun Modifier.clipToBounds() = this.graphicsLayer(clip = true)
