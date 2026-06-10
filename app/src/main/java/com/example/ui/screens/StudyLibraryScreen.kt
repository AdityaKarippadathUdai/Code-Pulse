package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyItem
import com.example.data.repository.CodePulseRepository
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudyLibraryHub(
    repository: CodePulseRepository,
    onOpenItem: (StudyItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe DB study items
    var searchQuery by remember { mutableStateOf("") }
    val studyItems by repository.searchStudyItems(searchQuery).collectAsState(initial = emptyList())
    val totalSavedCount by repository.getTotalSavedCountFlow().collectAsState(initial = 0)
    val categoryCounts by repository.getCategoryCountsFlow().collectAsState(initial = emptyList())

    // Selected folder / category filter (null means all categories)
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // List of standard categories for display
    val categories = listOf(
        "DSA", "System Design", "Databases", "Operating Systems",
        "Networking", "Machine Learning", "Interview Prep", "Personal Notes", "Research"
    )

    // Filter items based on selected category in UI
    val displayedItems = remember(studyItems, selectedCategory) {
        if (selectedCategory == null) {
            studyItems
        } else {
            studyItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    // Dynamic stats calculations
    val streakDays = remember(studyItems) { calculateStreak(studyItems) }
    val topCategory = remember(studyItems, categoryCounts) {
        if (studyItems.isEmpty()) "None"
        else {
            val maxViewed = studyItems.maxByOrNull { it.viewCount }
            if (maxViewed != null && maxViewed.viewCount > 0) {
                "${maxViewed.category} (${maxViewed.viewCount}v)"
            } else {
                categoryCounts.firstOrNull()?.category ?: "General"
            }
        }
    }
    val recentlyStudied = remember(studyItems) {
        studyItems
            .filter { it.lastViewedDate > 0 }
            .sortedByDescending { it.lastViewedDate }
            .take(3)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Search Section
        item {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_search_input"),
                placeholder = { Text("Search files, notes, tags & content...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, "Clear", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedContainerColor = Color(0xFF161B22),
                    unfocusedContainerColor = Color(0xFF161B22),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        }

        // 2. Statistics Panel
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp))
                    .background(Color(0xFF161B22))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STUDY WORKSPACE ANALYTICS",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Filled.Insights,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Saved
                    StatCard(
                        title = "TOTAL SAVED",
                        value = "$totalSavedCount files",
                        icon = Icons.Filled.CollectionsBookmark,
                        iconColor = Color(0xFF58A6FF),
                        modifier = Modifier.weight(1f)
                    )
                    // Streak
                    StatCard(
                        title = "STREAK",
                        value = "$streakDays Days",
                        icon = Icons.Filled.LocalFireDepartment,
                        iconColor = Color(0xFFFF7B72),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Most Viewed Topics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F141C))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, "Active", tint = Color(0xFF58A6FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Most Viewed Topic: ",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = topCategory,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Category Filter Chips (Folders/Collections)
        item {
            Column {
                Text(
                    text = "Collections & Categories",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All files") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color(0xFF161B22),
                                labelColor = Color.LightGray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory == null,
                                borderColor = Color(0xFF30363D),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = if (isSelected) null else category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color(0xFF161B22),
                                labelColor = Color.LightGray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFF30363D),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        // 4. Recently Studied carousel
        if (recentlyStudied.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Recently Studied Documents",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentlyStudied) { item ->
                            RecentStudiedCard(item = item, onClick = { onOpenItem(item) })
                        }
                    }
                }
            }
        }

        // 5. Saved Files List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory == null) "Saved Library" else "Saved: $selectedCategory",
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${displayedItems.size} files",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // 6. Saved Files List Items
        if (displayedItems.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Inbox,
                        contentDescription = "Empty Library",
                        tint = Color.Gray.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No search results match." else "No study files in this category yet.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Browse Offline Repositories and tap Save to Study Library to bookmark files offline permanently.",
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp),
                            style = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        )
                    }
                }
            }
        } else {
            items(displayedItems) { item ->
                StudyItemCard(
                    item = item,
                    onClick = { onOpenItem(item) },
                    onDelete = {
                        coroutineScope.launch {
                            repository.deleteStudyItem(item)
                            Toast.makeText(context, "Removed from Study Library", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onToggleFavorite = {
                        coroutineScope.launch {
                            repository.updateStudyItem(item.copy(isFavorite = !item.isFavorite))
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F141C))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(value, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RecentStudiedCard(
    item: StudyItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(100.dp)
            .testTag("recent_studied_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = item.category,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.sourceRepository,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favorite",
                        tint = Color(0xFFE2B93B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StudyItemCard(
    item: StudyItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study_item_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF21262D))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category,
                            color = Color(0xFF58A6FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (item.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.tags.split(",").joinToString(" ") { "#$it" },
                            color = Color.Gray,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) Color(0xFFE2B93B) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF7B72),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.sourceRepository} » ${item.filePath}",
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F141C))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Notes, "Notes", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PERSONAL NOTES", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.notes,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved on ${formatStudyDate(item.savedDate)}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                if (item.viewCount > 0) {
                    Text(
                        text = "Studied ${item.viewCount} times",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Helper to calculate streaks
fun calculateStreak(items: List<StudyItem>): Int {
    val dates = items.flatMap { listOf(it.lastViewedDate, it.savedDate) }
        .filter { it > 0 }
        .map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }
        .toSet()

    if (dates.isEmpty()) return 0

    val todayCal = Calendar.getInstance()
    val todayStr = String.format("%04d-%02d-%02d", todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH) + 1, todayCal.get(Calendar.DAY_OF_MONTH))

    val yesterdayCal = Calendar.getInstance()
    yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = String.format("%04d-%02d-%02d", yesterdayCal.get(Calendar.YEAR), yesterdayCal.get(Calendar.MONTH) + 1, yesterdayCal.get(Calendar.DAY_OF_MONTH))

    var checkCal = Calendar.getInstance()
    if (dates.contains(todayStr)) {
        // Start counting today
    } else if (dates.contains(yesterdayStr)) {
        // Start yesterday
        checkCal.add(Calendar.DAY_OF_YEAR, -1)
    } else {
        return 0
    }

    var streak = 0
    while (true) {
        val dateStr = String.format("%04d-%02d-%02d", checkCal.get(Calendar.YEAR), checkCal.get(Calendar.MONTH) + 1, checkCal.get(Calendar.DAY_OF_MONTH))
        if (dates.contains(dateStr)) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    return streak
}

fun formatStudyDate(epoch: Long): String {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return df.format(Date(epoch))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyItemReader(
    item: StudyItem,
    repository: CodePulseRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Internal editable states updated in DB on actions
    var notesText by remember { mutableStateOf(item.notes) }
    var tagsText by remember { mutableStateOf(item.tags) }
    var isFavorite by remember { mutableStateOf(item.isFavorite) }
    var selectedCategory by remember { mutableStateOf(item.category) }
    var showCategorySelector by remember { mutableStateOf(false) }

    val categories = listOf(
        "DSA", "System Design", "Databases", "Operating Systems",
        "Networking", "Machine Learning", "Interview Prep", "Personal Notes", "Research"
    )

    // Parse highlight line set
    val highlightedLines = remember(item.highlightsJson) {
        item.highlightsJson.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    // Auto increment viewCount and track streak once on open
    LaunchedEffect(item.id) {
        val currentMillis = System.currentTimeMillis()
        val updated = item.copy(
            viewCount = item.viewCount + 1,
            lastViewedDate = currentMillis
        )
        repository.updateStudyItem(updated)
    }

    // Tab Selection: Document vs Notes
    var activeSubTab by remember { mutableStateOf(0) } // 0: Document, 1: Study Notes

    val fileContent = item.fileContent ?: "No content saved."
    val lines = remember(fileContent) { fileContent.lines() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.sourceRepository} » ${item.filePath}",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    isFavorite = !isFavorite
                    coroutineScope.launch {
                        repository.updateStudyItem(item.copy(isFavorite = isFavorite))
                    }
                }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFE2B93B) else Color.Gray
                )
            }
        }

        // Segment selector tabs
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = Color(0xFF161B22),
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Document View", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Filled.Description, null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Study Notes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Filled.EditNote, null, modifier = Modifier.size(16.dp)) }
            )
        }

        HorizontalDivider(color = Color(0xFF30363D))

        Box(modifier = Modifier.weight(1f)) {
            if (activeSubTab == 0) {
                // Document View with Line-by-Line Highlighter Clicking!
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1F242C))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 Tip: Tap on any text line to toggle study highlights.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (highlightedLines.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.updateStudyItem(item.copy(highlightsJson = ""))
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Clear All", fontSize = 11.sp, color = Color(0xFFFF7B72))
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F141C))
                            .padding(vertical = 8.dp)
                    ) {
                        items(lines.size) { index ->
                            val lineNum = index + 1
                            val lineText = lines[index]
                            val isHighlighted = highlightedLines.contains(lineNum)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isHighlighted) Color(0xFFFFF2B2).copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        val newHighlights = if (isHighlighted) {
                                            highlightedLines - lineNum
                                        } else {
                                            highlightedLines + lineNum
                                        }
                                        coroutineScope.launch {
                                            repository.updateStudyItem(
                                                item.copy(highlightsJson = newHighlights.joinToString(","))
                                            )
                                        }
                                    }
                                    .padding(vertical = 1.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%3d", lineNum),
                                    color = if (isHighlighted) Color(0xFFFFF2B2) else Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(32.dp),
                                    style = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = lineText,
                                    color = if (isHighlighted) Color(0xFFFFFAD2) else Color(0xFFE6EDF0),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Study Notes, Tags and Metadata Updates Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category selector block
                    Column {
                        Text("Category Classification", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
                                .background(Color(0xFF161B22))
                                .clickable { showCategorySelector = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.FolderOpen, null, tint = Color(0xFF58A6FF))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(selectedCategory, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(Icons.Filled.ArrowDropDown, null, tint = Color.Gray)
                            }
                        }

                        if (showCategorySelector) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                border = BorderStroke(1.dp, Color(0xFF30363D))
                            ) {
                                Column {
                                    categories.forEach { cat ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCategory = cat
                                                    showCategorySelector = false
                                                    coroutineScope.launch {
                                                        repository.updateStudyItem(item.copy(category = cat))
                                                    }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedCategory == cat,
                                                onClick = {
                                                    selectedCategory = cat
                                                    showCategorySelector = false
                                                    coroutineScope.launch {
                                                        repository.updateStudyItem(item.copy(category = cat))
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(cat, color = Color.White, fontSize = 14.sp)
                                        }
                                        HorizontalDivider(color = Color(0xFF30363D).copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    // Tags field
                    Column {
                        Text("Searchable Study Tags", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = tagsText,
                            onValueChange = {
                                tagsText = it
                                coroutineScope.launch {
                                    repository.updateStudyItem(item.copy(tags = it))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("study_tags_input"),
                            placeholder = { Text("e.g. dynamic_programming, binary_tree, interview", color = Color.Gray, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF30363D),
                                focusedContainerColor = Color(0xFF161B22),
                                unfocusedContainerColor = Color(0xFF161B22),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        Text("Comma-separated items will automatically map tag indices for global searches.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    // Notes text area
                    Column {
                        Text("Personal Research Notes & Highlights", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = {
                                notesText = it
                                coroutineScope.launch {
                                    repository.updateStudyItem(item.copy(notes = it))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("study_notes_input"),
                            placeholder = { Text("Type key formulas, system diagrams, summaries or algorithms here...", color = Color.Gray, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF30363D),
                                focusedContainerColor = Color(0xFF161B22),
                                unfocusedContainerColor = Color(0xFF161B22),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-saved instantaneously as you type.", color = Color(0xFF58A6FF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("${notesText.length} characters", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

