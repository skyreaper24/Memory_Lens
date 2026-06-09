package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.res.painterResource
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
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
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
            "Connect \"${memories[0].title.take(22)}\" with \"${memories[1].title.take(22)}\" to unlock nested synaptic pathways."
        } else {
            "Ingest more files or quotes to spin the neural web of your second brain."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(DeepIndigo.copy(alpha = 0.15f), ElevatedSlate.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, NebulaViolet.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pulse anim or indicator
            val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseAlpha"
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(NebulaViolet.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .graphicsLayer(alpha = pulseAlpha)
                        .background(TealGlow, CircleShape)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NEURAL EXPLORATION PROMPT",
                    color = NebulaViolet,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
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
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = SlateGray,
                modifier = Modifier.size(16.dp)
            )
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

    if (allMemories.isEmpty()) {
        EmptyWorkspaceView(onSeed = { viewModel.seedSampleData() })
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("memories_list")
        ) {
            // Item 1: Brand Dynamic Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    brush = Brush.radialGradient(colors = listOf(NebulaViolet.copy(alpha = 0.25f), Color.Transparent)),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = com.example.R.drawable.ic_logo),
                                contentDescription = "MemoryLens Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MemoryLens",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OffWhite,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "YOUR SECOND COGNITIVE BRAIN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    // Sync indicators with glow color mix
                    Box(
                        modifier = Modifier
                            .background(DeepSlate, RoundedCornerShape(12.dp))
                            .border(1.dp, GhostWhite, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(TealGlow, CircleShape))
                            Text(text = "LIVE SYNCED", color = OffWhite, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Item 2: Spectacular Aura Dashboard Card
            item {
                MindPalaceAuraWidget(allMemories.size, conceptNodes.size)
            }

            // Item 3: Daily High-End Multi-colored Insight Card
            item {
                ApertureRefractiveInsightPanel(allMemories)
            }

            // Item 4: Glowing Rainbow Action Search Dock
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 1.dp)
                            .background(
                                Brush.horizontalGradient(listOf(NebulaViolet, CoralGlow)),
                                RoundedCornerShape(18.dp)
                            )
                            .graphicsLayer(alpha = 0.15f)
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.userQuery.value = it },
                        placeholder = { Text("Query your mind palace...", color = SlateGray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Query Lens", tint = NebulaViolet, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.userQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_bar_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = DeepSlate.copy(alpha = 0.95f),
                            unfocusedContainerColor = DeepSlate.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true
                    )
                }
            }

            // Item 5: Pulse-glowing Recommended Connection bubble
            item {
                RecommendedDiscoveryBubble(memories = memories)
            }

            // Item 6: Source choice capsules list colored beautifully
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sources = listOf("all", "note", "book", "url", "audio")
                    sources.forEach { src ->
                        val isSelected = activeFilter == src
                        val bgBrush = if (isSelected) {
                            val gradientColors = when (src) {
                                "book" -> listOf(DeepIndigo, NebulaViolet)
                                "url" -> listOf(TealGlow, DeepIndigo)
                                "audio" -> listOf(CoralGlow, DeepIndigo)
                                "note" -> listOf(NebulaViolet, DeepIndigo)
                                else -> listOf(NebulaViolet, DeepIndigo)
                            }
                            Brush.horizontalGradient(gradientColors)
                        } else {
                            Brush.linearGradient(listOf(DeepSlate, DeepSlate))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(bgBrush, RoundedCornerShape(20.dp))
                                .border(1.dp, if (isSelected) Color.White.copy(alpha = 0.6f) else GhostWhite, RoundedCornerShape(20.dp))
                                .clickable { viewModel.setSourceFilter(src) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (src) {
                                "book" -> Icons.Default.MenuBook
                                "url" -> Icons.Default.Link
                                "audio" -> Icons.Default.Mic
                                "note" -> Icons.Default.Description
                                else -> Icons.Default.GridView
                            }
                            val accentColor = when (src) {
                                "book" -> DeepIndigo
                                "url" -> TealGlow
                                "audio" -> CoralGlow
                                "note" -> NebulaViolet
                                else -> NebulaViolet
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    icon, 
                                    contentDescription = src, 
                                    tint = if (isSelected) Color.White else accentColor, 
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = src.uppercase(),
                                    color = if (isSelected) Color.White else OffWhite,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Item 7: Memory Streams feed list header
            item {
                Text(
                    text = "MEMORIES STREAM FEED // SHRED RECALL",
                    color = SlateGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Items (Memories stream search listings)
            if (memories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = CoralGlow, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching concept highlights found.\nTry broadening your search query.",
                                color = SlateGray,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                items(memories) { memo ->
                    MemoryItemRowCard(item = memo, onClick = { viewModel.selectItem(memo) })
                }
            }

            // Safe spacing for bottom bar overlay
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun MindPalaceAuraWidget(memoriesCount: Int, nodesCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(DeepSlate, ElevatedSlate)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(CyberCyan, NebulaViolet, CyberYellow)),
                RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "MIND INTEGRITY & SYNA-AURA",
                    color = CyberCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Cosmic Mindspace",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OffWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(CyberYellow.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, CyberYellow.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "7 Days Streak", color = CyberYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(LaserRed.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, LaserRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Compounding Focus", color = LaserRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Radial progress sphere represents learning density
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(58.dp)) {
                    val centerPt = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = GhostWhite,
                        radius = size.width / 2,
                        center = centerPt,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(CyberCyan, NebulaViolet, LaserRed, CyberCyan)
                        ),
                        startAngle = -90f,
                        sweepAngle = 290f, // representing 80% depth score
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "94%", color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = "DEP", color = CyberCyan, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Divider(color = GhostWhite, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "SAVED MEMORIES", fontSize = 8.sp, color = SlateGray, fontWeight = FontWeight.Bold)
                Text(text = "$memoriesCount Nodes", fontSize = 14.sp, color = CyberCyan, fontWeight = FontWeight.ExtraBold)
            }
            Column {
                Text(text = "CONCEPT MATRIX", fontSize = 8.sp, color = SlateGray, fontWeight = FontWeight.Bold)
                Text(text = "$nodesCount Links", fontSize = 14.sp, color = CyberYellow, fontWeight = FontWeight.ExtraBold)
            }
            Column {
                Text(text = "COGNITIVE SPEED", fontSize = 8.sp, color = SlateGray, fontWeight = FontWeight.Bold)
                Text(text = "37x Speed", fontSize = 14.sp, color = LaserRed, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun ApertureRefractiveInsightPanel(memories: List<MemoryItem>) {
    // Elegant refract highlight
    val defaultHighlight = "Compounding happens automatically. Daily improvements of 1% yield stellar, lifelong cognitive assets."
    val displayInsight = remember(memories) {
        if (memories.isNotEmpty()) {
            "Synthesized from \"${memories[0].title}\": Keep active focus limits inside 90-minute blocks to avoid neural fatigue."
        } else {
            defaultHighlight
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(DeepSlate, ElevatedSlate)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                1.dp, 
                Brush.horizontalGradient(listOf(LaserRed, CyberYellow, CyberCyan)), 
                RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberYellow, modifier = Modifier.size(14.dp))
                Text(
                    text = "DAILY RECONSTRUCTIVE INSIGHT",
                    fontSize = 8.sp,
                    color = CyberYellow,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = displayInsight,
                color = OffWhite,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MemoryItemRowCard(item: MemoryItem, onClick: () -> Unit) {
    val sourceColor = remember(item.sourceType) {
        when (item.sourceType.lowercase()) {
            "book" -> CyberYellow
            "url" -> CyberCyan
            "audio" -> NeonOrange
            "note" -> LaserRed
            else -> NebulaViolet
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("memory_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = DeepSlate),
        shape = RoundedCornerShape(20.dp),
        border = borderStrokeLight(sourceColor.copy(alpha = 0.25f))
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
                            .background(sourceColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, sourceColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        val icon = when (item.sourceType.lowercase()) {
                            "book" -> Icons.Default.MenuBook
                            "url" -> Icons.Default.Link
                            "audio" -> Icons.Default.Mic
                            else -> Icons.Default.NoteAlt
                        }
                        Icon(icon, contentDescription = null, tint = sourceColor, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.sourceType.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = sourceColor,
                        letterSpacing = 0.8.sp
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
                    color = SlateGray,
                    fontWeight = FontWeight.Medium
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
                fontSize = 12.sp,
                color = SlateGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Display cognitive tags directly
            val tags = remember(item.cognitiveTagsJson) {
                parseJsonStringList(item.cognitiveTagsJson)
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
                            Text(text = "#$tag", color = SlateGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(
                        brush = Brush.radialGradient(colors = listOf(NebulaViolet.copy(alpha = 0.15f), Color.Transparent)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = com.example.R.drawable.ic_logo),
                    contentDescription = "MemoryLens Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Your Mind Palace Awaits",
                fontSize = 22.sp,
                color = OffWhite,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Begin capturing video highlights, text logs, podcast insights, or article notes to build your cognitive neural network dynamically.",
                fontSize = 13.sp,
                color = SlateGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            
            Button(
                onClick = onSeed,
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedSlate),
                border = borderStrokeLight(NebulaViolet.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NebulaViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Initialize Second Brain (Seed Data)", color = OffWhite, fontWeight = FontWeight.Bold)
                }
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
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GhostWhite, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Enrich Brain Cache",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Supply web links, books passages, lectures blocks, or speech transcripts. Gemini AI will analyze key nodes.",
                    fontSize = 11.sp,
                    color = SlateGray,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Source Type Capsule Choice
                Text(text = "SOURCE FORMAT TYPE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateGray, letterSpacing = 0.5.sp)
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
                                    RoundedCornerShape(10.dp)
                                )
                                .border(1.dp, if (active) NebulaViolet else Color.Transparent, RoundedCornerShape(10.dp))
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

                // Raw input text field
                OutlinedTextField(
                    value = text,
                    onValueChange = { viewModel.addMemoryText.value = it },
                    placeholder = { Text("What did you learn? Paste raw texts, lectures transcripts, quotes or logs...", color = SlateGray, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("ingest_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite,
                        focusedBorderColor = NebulaViolet,
                        unfocusedBorderColor = GhostWhite,
                        focusedContainerColor = CosmicVoid,
                        unfocusedContainerColor = CosmicVoid
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isAnalyzing) {
                        Text(text = "CANCEL", color = SlateGray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { viewModel.ingestMemory() },
                        colors = ButtonDefaults.buttonColors(containerColor = NebulaViolet),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAnalyzing && text.trim().isNotEmpty(),
                        modifier = Modifier.testTag("submit_ingest_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text(text = "ENRICH & MAP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GhostWhite, RoundedCornerShape(24.dp))
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
                            Text(text = "Synthesized Concept Map", fontWeight = FontWeight.Bold, color = OffWhite, fontSize = 16.sp)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Outline", tint = SlateGray)
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
                    Text(text = "SEMANTIC EXTRACT", fontSize = 10.sp, color = SlateGray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.summary,
                        color = OffWhite,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Raw Content
                    Text(text = "RAW MIND DATA SOURCE", fontSize = 10.sp, color = SlateGray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicVoid, RoundedCornerShape(12.dp))
                            .border(1.dp, GhostWhite, RoundedCornerShape(12.dp))
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
                        Text(text = "PROACTIVE COGNITIVE TODO", fontSize = 10.sp, color = SlateGray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
                        Text(text = "ACTIVE STUDY MEMORY DECK", fontSize = 10.sp, color = SlateGray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(flashcards) { card ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(ElevatedSlate, RoundedCornerShape(12.dp))
                                .border(1.dp, GhostWhite, RoundedCornerShape(12.dp))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1FFF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = CoralGlow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "EXCISE FROM ACTIVE KNOWLEDGE", color = CoralGlow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
        Text(text = "Temporal Continuum", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OffWhite)
        Text(text = "YOUR CHRONOLOGICAL INTELLECTUAL EXPLORATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateGray, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (memories.isEmpty()) {
            EmptyWorkspaceView(onSeed = { viewModel.seedSampleData() })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(memories) { memo ->
                    val memoColor = when (memo.sourceType.lowercase()) {
                        "book" -> CyberYellow
                        "url" -> CyberCyan
                        "audio" -> NeonOrange
                        "note" -> LaserRed
                        else -> NebulaViolet
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        // Futuristic timeline tracker path
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(32.dp)
                        ) {
                            // Pulsing core dot
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        brush = Brush.radialGradient(colors = listOf(memoColor, Color.Transparent)),
                                        shape = CircleShape
                                    )
                                    .border(2.dp, memoColor, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(110.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(memoColor.copy(alpha = 0.5f), Color.Transparent)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Timeline box cards
                        Column {
                            val timeStr = remember(memo.timestamp) {
                                val sdf = SimpleDateFormat("MMMM d, yyyy @ h:mm a", Locale.getDefault())
                                sdf.format(Date(memo.timestamp))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = timeStr, fontSize = 11.sp, color = memoColor, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .background(memoColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .border(1.dp, memoColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = memo.sourceType.uppercase(), color = memoColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectItem(memo) },
                                colors = CardDefaults.cardColors(containerColor = DeepSlate),
                                shape = RoundedCornerShape(16.dp),
                                border = borderStrokeLight(memoColor.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = memo.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OffWhite)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = memo.summary,
                                        fontSize = 12.sp,
                                        color = SlateGray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 17.sp
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

// Draggable orbital node container class
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

    // Synchronize concept mapping node objects
    LaunchedEffect(concepts) {
        if (concepts.isNotEmpty()) {
            val existingIds = graphNodes.map { it.concept.id }.toSet()
            concepts.forEach { node ->
                if (node.id !in existingIds) {
                    val gNode = GraphNode(node).apply {
                        x.value = 0f
                        y.value = 0f
                    }
                    graphNodes.add(gNode)
                }
            }
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
            // Spectacular Interactive Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(colors = listOf(DeepSlate, ElevatedSlate)),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(LaserRed, CyberYellow, CyberCyan)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(CyberCyan, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "NEURAL LINK INTEGRATION", color = CyberCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Text(text = "All ${concepts.size} Synapses Active", color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    
                    Button(
                        onClick = {
                            // Instant high fidelity random spring redistribution
                            graphNodes.forEachIndexed { index, gNode ->
                                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                                val radius = 120f + Random.nextFloat() * 120f
                                gNode.x.value = 400f + radius * cos(angle)
                                gNode.y.value = 400f + radius * sin(angle)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicVoid),
                        border = BorderStroke(1.dp, GhostWhite),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberYellow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Optimize Layout", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OffWhite)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Zoom transform factors
            var scale by remember { mutableStateOf(1f) }
            val transformState = rememberTransformableState { zoomChange, _, _ ->
                scale = (scale * zoomChange).coerceIn(0.6f, 2.0f)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CosmicVoid, RoundedCornerShape(24.dp))
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(colors = listOf(NebulaViolet.copy(alpha = 0.5f), CyberCyan.copy(alpha = 0.2f)))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .clipToBounds()
            ) {
                val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

                val centerXPx = if (widthPx > 0) widthPx / 2f else 500f
                val centerYPx = if (heightPx > 0) heightPx / 2f else 450f

                // Re-center and circular disperse nodes when scale/bounds load or concepts change
                LaunchedEffect(widthPx, heightPx, graphNodes.size) {
                    if (widthPx > 0 && heightPx > 0) {
                        graphNodes.forEachIndexed { index, gNode ->
                            if (gNode.x.value == 0f || gNode.x.value == 540f) {
                                val angle = (index.toFloat() / maxOf(1, graphNodes.size)) * 2.0 * Math.PI
                                val radius = (minOf(widthPx, heightPx) * 0.28f).coerceAtLeast(110f) + (index % 3) * 15f
                                gNode.x.value = centerXPx + (radius * cos(angle)).toFloat()
                                gNode.y.value = centerYPx + (radius * sin(angle)).toFloat()
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .transformable(state = transformState)
                ) {
                    // Immersive backdrop pattern
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer(alpha = 0.15f)
                        ) {
                            Text(
                                text = "COGNITIVE GRAPH INTERFACE",
                                color = SlateGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "N-Dimensional Mind",
                                color = OffWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Background grid and link paths
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val boundsCenter = Offset(size.width / 2f, size.height / 2f)

                        // Outer sonar rings
                        drawCircle(
                            color = CyberCyan.copy(alpha = 0.05f),
                            radius = minOf(size.width, size.height) * 0.18f,
                            center = boundsCenter,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = LaserRed.copy(alpha = 0.04f),
                            radius = minOf(size.width, size.height) * 0.32f,
                            center = boundsCenter,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )
                        drawCircle(
                            color = CyberYellow.copy(alpha = 0.02f),
                            radius = minOf(size.width, size.height) * 0.45f,
                            center = boundsCenter,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )

                        // Draw Connections with glowing multitone arcs/lines
                        connections.forEach { conn ->
                            val srcNode = graphNodes.firstOrNull { it.concept.id == conn.sourceId }
                            val trgNode = graphNodes.firstOrNull { it.concept.id == conn.targetId }
                            if (srcNode != null && trgNode != null && srcNode.x.value > 0 && trgNode.x.value > 0) {
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(CyberCyan.copy(alpha = 0.6f), LaserRed.copy(alpha = 0.6f))
                                    ),
                                    start = Offset(srcNode.x.value, srcNode.y.value),
                                    end = Offset(trgNode.x.value, trgNode.y.value),
                                    strokeWidth = 4f
                                )
                            }
                        }
                    }

                    // Cybernetic Multi-Colored Conceptual Nodes
                    graphNodes.forEach { gNode ->
                        val nodeColor = remember(gNode.concept.category) {
                            when (gNode.concept.category.lowercase()) {
                                "book" -> CyberYellow
                                "person", "author" -> LaserRed
                                "tool", "site" -> CyberCyan
                                "url" -> TealGlow
                                "audio", "voice" -> NeonOrange
                                "note" -> NebulaViolet
                                else -> NebulaViolet
                            }
                        }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (gNode.x.value - 60f).toInt(),
                                        (gNode.y.value - 26f).toInt()
                                    )
                                }
                                .pointerInput(gNode) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        // Position boundary clamping to NEVER bleed off-screen or overflow container
                                        val nextX = (gNode.x.value + dragAmount.x).coerceIn(80f, widthPx - 80f)
                                        val nextY = (gNode.y.value + dragAmount.y).coerceIn(40f, heightPx - 40f)
                                        gNode.x.value = nextX
                                        gNode.y.value = nextY
                                    }
                                }
                                .background(
                                    color = DeepSlate.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = nodeColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = gNode.concept.name,
                                    color = OffWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(nodeColor, CircleShape)
                                    )
                                    Text(
                                        text = gNode.concept.category.uppercase(),
                                        color = nodeColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
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

        // Cognitive dialog heading status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(text = "Brain Companion Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OffWhite)
                Text(text = "CONVERSE DIRECTLY WITH CONSTRUCTED KNOWLEDGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateGray, letterSpacing = 1.sp)
            }
            Box(
                modifier = Modifier
                    .background(ElevatedSlate, CircleShape)
                    .border(1.dp, GhostWhite, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = NebulaViolet, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History logs
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages.reversed()) { msg ->
                val isUser = msg.second
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (isUser) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .background(
                                    brush = Brush.horizontalGradient(listOf(NebulaViolet.copy(alpha = 0.85f), DeepIndigo.copy(alpha = 0.85f))),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .border(
                                    1.dp,
                                    NebulaViolet,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "YOU // SENSOR INTEGRATION",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OffWhite,
                                        letterSpacing = 1.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = OffWhite,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = msg.first,
                                    color = OffWhite,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .background(
                                    color = DeepSlate,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                                .border(
                                    1.dp,
                                    CyberCyan.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(36.dp)
                                        .background(CyberCyan, RoundedCornerShape(1.5.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "COGNITIVE COMPANION // GENERAL INTELLIGENCE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan,
                                            letterSpacing = 1.sp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = CyberYellow,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
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
            }
        }

        if (isThinking) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(ElevatedSlate, RoundedCornerShape(12.dp))
                        .border(1.dp, GhostWhite, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = NebulaViolet, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Rerouting synapses & scanning memories...", color = SlateGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input sent box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { viewModel.chatInput.value = it },
                placeholder = { Text("Ask summaries, flashcard review or studies...", color = SlateGray, fontSize = 13.sp) },
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
                shape = RoundedCornerShape(16.dp),
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
                    contentDescription = "Send Synaptic Query",
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
                .background(DeepSlate.copy(alpha = 0.94f), RoundedCornerShape(32.dp))
                .border(1.dp, GhostWhite, RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tab 1: Map
            val tab1Selected = activeTab == "dashboard"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable { onTabSelected("dashboard") }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (tab1Selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = "Dashboard Screen",
                    tint = if (tab1Selected) NebulaViolet else SlateGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Palace Map",
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
                    .clip(CircleShape)
                    .clickable { onTabSelected("timeline") }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (tab2Selected) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = "Timeline Screen",
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

            // Central Brain Ingestion Floating Trigger
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.verticalGradient(listOf(NebulaViolet, DeepIndigo)),
                        CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { onAddClicked() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ingest Knowledge Insight",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab 3: Mind Graph
            val tab3Selected = activeTab == "graph"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable { onTabSelected("graph") }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (tab3Selected) Icons.Filled.BubbleChart else Icons.Outlined.BubbleChart,
                    contentDescription = "Mind Graph",
                    tint = if (tab3Selected) NebulaViolet else SlateGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Neural Web",
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
                    .clip(CircleShape)
                    .clickable { onTabSelected("chat") }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (tab4Selected) Icons.Filled.Psychology else Icons.Outlined.Psychology,
                    contentDescription = "AI Assistant Chat",
                    tint = if (tab4Selected) NebulaViolet else SlateGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Brain Chat",
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

// Moshi parser client helper functions for DB data parsing
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