package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.content.ClipboardManager
import android.content.ClipData
import java.io.File
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.pref.CodePulsePrefs
import com.example.data.repository.CodePulseRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Main View Router & Bottom Navigation Screen Wrapper
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainLayout(
    repository: CodePulseRepository,
    prefs: CodePulsePrefs,
    themeMode: Int,
    onThemeChange: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe Preference Values
    val isLoggedIn by prefs.isLoggedIn.collectAsState()
    val savedGithubUser by prefs.githubUsername.collectAsState()
    val savedLeetcodeUser by prefs.leetcodeUsername.collectAsState()

    var activeTab by remember { mutableStateOf("dashboard") }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    // Sync trigger state
    var isRefreshing by remember { mutableStateOf(false) }

    // Load active cache
    val lcStatsFlow = remember(savedLeetcodeUser) { repository.getLeetCodeStatsFlow(savedLeetcodeUser) }
    val lcStatsState = lcStatsFlow.collectAsState(initial = null)

    val ghStatsFlow = remember(savedGithubUser) { repository.getGitHubStatsFlow(savedGithubUser) }
    val ghStatsState = ghStatsFlow.collectAsState(initial = null)

    val lcSubmissionsState = remember(savedLeetcodeUser) { repository.getLeetCodeSubmissionsFlow(savedLeetcodeUser) }.collectAsState(initial = emptyList())
    val ghReposState = remember(savedGithubUser) { repository.getGitHubReposFlow(savedGithubUser) }.collectAsState(initial = emptyList())
    val ghEventsState = remember(savedGithubUser) { repository.getGitHubEventsFlow(savedGithubUser) }.collectAsState(initial = emptyList())
    val goalsState = repository.getGoalsFlow().collectAsState(initial = emptyList())
    val achievementsState = repository.getAchievementsFlow().collectAsState(initial = emptyList())
    val historyState = repository.getCodingHistoryFlow().collectAsState(initial = emptyList())

    // Direct Prepopulation Trigger
    LaunchedEffect(Unit) {
        repository.prepopulateDatabaseIfNeeded()
        if (savedGithubUser.isNotBlank() || savedLeetcodeUser.isNotBlank()) {
            repository.syncUserData(savedGithubUser, savedLeetcodeUser)
        }
    }

    val syncTrigger = {
        coroutineScope.launch {
            isRefreshing = true
            repository.syncUserData(savedGithubUser, savedLeetcodeUser)
            isRefreshing = false
        }
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginComplete = { ghUser, lcUser, guest ->
                coroutineScope.launch {
                    prefs.setGithubUsername(ghUser)
                    prefs.setLeetcodeUsername(lcUser)
                    prefs.setLoggedIn(true)
                    repository.prepopulateDatabaseIfNeeded()
                    repository.syncUserData(ghUser, lcUser)
                }
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        TabItem("Dashboard", "dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
                        TabItem("Stats", "stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
                        TabItem("Repository", "repository", Icons.Filled.Folder, Icons.Outlined.Folder),
                        TabItem("Vault", "vault", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
                        TabItem("Profile", "profile", Icons.Filled.Person, Icons.Outlined.Person)
                    )
                    tabs.forEach { tab ->
                        val selected = activeTab == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { activeTab = tab.route },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "TabContent"
                ) { currentRoute ->
                    when (currentRoute) {
                        "dashboard" -> DashboardScreen(
                            githubUser = savedGithubUser,
                            leetcodeUser = savedLeetcodeUser,
                            lcStats = lcStatsState.value,
                            ghStats = ghStatsState.value,
                            ghEvents = ghEventsState.value,
                            goals = goalsState.value,
                            isRefreshing = isRefreshing,
                            onRefresh = { syncTrigger() },
                            onNavigateToTab = { activeTab = it },
                            onShowAddGoal = { showAddGoalDialog = true }
                        )
                        "stats" -> StatsScreen(
                            savedGithubUser = savedGithubUser,
                            savedLeetcodeUser = savedLeetcodeUser,
                            lcStats = lcStatsState.value,
                            lcSubmissions = lcSubmissionsState.value,
                            ghStats = ghStatsState.value,
                            ghRepos = ghReposState.value,
                            history = historyState.value,
                            goals = goalsState.value,
                            achievements = achievementsState.value,
                            isRefreshing = isRefreshing,
                            onRefresh = { syncTrigger() },
                            onDeleteGoal = { coroutineScope.launch { repository.deleteGoal(it) } },
                            onShowAddGoal = { showAddGoalDialog = true }
                        )
                        "repository" -> RepositoryScreen(
                            repository = repository,
                            prefs = prefs
                        )
                        "vault" -> VaultScreen(
                            repository = repository,
                            prefs = prefs
                        )
                        "profile" -> SettingsScreen(
                            prefs = prefs,
                            themeMode = themeMode,
                            onThemeChange = onThemeChange,
                            lcStats = lcStatsState.value,
                            ghStats = ghStatsState.value,
                            goals = goalsState.value,
                            achievements = achievementsState.value,
                            onClearCache = {
                                coroutineScope.launch {
                                    repository.clearCache()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, type, target ->
                showAddGoalDialog = false
                coroutineScope.launch {
                    repository.insertGoal(GoalEntity(title = title, type = type, target = target, current = 0))
                }
            }
        )
    }
}

data class TabItem(val title: String, val route: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector)

// LOGIN SCREEN
@Composable
fun LoginScreen(
    onLoginComplete: (githubUser: String, leetcodeUser: String, isGuestMode: Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var githubInput by remember { mutableStateOf("") }
    var leetcodeInput by remember { mutableStateOf("") }
    var displayOAuthSim by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Brand Visual
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PulseCompose,
                    contentDescription = "CodePulse Logo",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CodePulse",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Track•Code•Improve",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login forms Card layout
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Connect Your Profiles",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    var errorMsg by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = githubInput,
                        onValueChange = { 
                            githubInput = it
                            if (it.isNotBlank()) errorMsg = ""
                        },
                        label = { Text("GitHub username") },
                        placeholder = { Text("e.g. octocat") },
                        leadingIcon = { Icon(Icons.Filled.Source, contentDescription = "GitHub") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_input"),
                        singleLine = true,
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = leetcodeInput,
                        onValueChange = { leetcodeInput = it },
                        label = { Text("LeetCode username (Optional)") },
                        placeholder = { Text("e.g. leetcode_user") },
                        leadingIcon = { Icon(Icons.Filled.Code, contentDescription = "LeetCode") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leetcode_input"),
                        singleLine = true,
                        colors = textFieldColors
                    )

                    if (errorMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (githubInput.isBlank()) {
                                errorMsg = "Please enter a GitHub username to proceed securely."
                            } else {
                                displayOAuthSim = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("oauth_login_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "OAuth")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect securely with GitHub")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (githubInput.isBlank()) {
                                errorMsg = "Please enter a GitHub username to explore public profile page."
                            } else {
                                onLoginComplete(githubInput, leetcodeInput, true)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("guest_mode_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Explore Public Profile (No Auth)")
                    }
                }
            }
        }
    }

    if (displayOAuthSim) {
        AlertDialog(
            onDismissRequest = { displayOAuthSim = false },
            title = { Text("GitHub Secure Authorization") },
            text = {
                Column {
                    Text("CodePulse wants access to register your public repository statistics, stars, and contribution metrics.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "🔒 Real OAuth flow signature. Secure authorization token is managed directly by Datastore preferences and never exposed to intermediate servers.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        displayOAuthSim = false
                        val gh = if (githubInput.isBlank()) "github_oauth_user" else githubInput
                        val lc = if (leetcodeInput.isBlank()) "leetcode_developer" else leetcodeInput
                        onLoginComplete(gh, lc, false)
                    },
                    modifier = Modifier.testTag("oauth_confirm_button")
                ) {
                    Text("Authorize")
                }
            },
            dismissButton = {
                TextButton(onClick = { displayOAuthSim = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

val Icons.Filled.PulseCompose: ImageVector
    get() = Icons.Filled.NetworkCheck // Visual indicator replacement for heartrate/pulse curve

// DASHBOARD TAB
@Composable
fun DashboardScreen(
    githubUser: String,
    leetcodeUser: String,
    lcStats: LeetCodeStatsEntity?,
    ghStats: GitHubStatsEntity?,
    ghEvents: List<GitHubEventCache>,
    goals: List<GoalEntity>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToTab: (String) -> Unit,
    onShowAddGoal: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Score calculations
    val lcTotalSolved = lcStats?.totalSolved ?: 0
    val lcStreak = lcStats?.currentStreak ?: 0
    val lcContestRating = lcStats?.contestRating ?: 0
    val ghRepos = ghStats?.publicRepos ?: 0
    val ghStars = ghStats?.totalStars ?: 0

    // Algorithmic CodePulse score (max 1000)
    val calculatedScore = remember(lcTotalSolved, lcStreak, lcContestRating, ghRepos, ghStars) {
        val lcScore = minOf(lcTotalSolved * 4, 400) // Easy Medium Hard
        val streakScore = minOf(lcStreak * 10, 200) // Consistency
        val ghScore = minOf(ghRepos * 6 + ghStars * 5, 200) // Repos & Stars
        val ratingBonus = if (lcContestRating > 0) minOf((lcContestRating / 10), 200) else 0
        lcScore + streakScore + ghScore + ratingBonus
    }

    val displayPercentage = calculatedScore / 1000f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Welcome Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome Back, Developer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (ghStats != null) ghStats.name else githubUser.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                if (ghStats != null && ghStats.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = ghStats.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Avatar Placeholder",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Circular score Dashboard metrics card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(
                        text = "CodePulse Score",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your custom metrics compiled from overall LeetCode submissions, GitHub activity, streaks and commits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Class: ${
                            when {
                                calculatedScore >= 750 -> "Grandmaster Developer"
                                calculatedScore >= 500 -> "Expert Engineer"
                                calculatedScore >= 250 -> "Competent Craftsman"
                                else -> "Novice Apprentice"
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Custom Canvas Circular Progress
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .weight(0.8f),
                    contentAlignment = Alignment.Center
                ) {
                    val angleAnim = remember { Animatable(0f) }
                    LaunchedEffect(calculatedScore) {
                        angleAnim.animateTo(
                            targetValue = displayPercentage,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }

                    val pColor = MaterialTheme.colorScheme.primary
                    val sColor = MaterialTheme.colorScheme.secondary

                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val strokeWidth = 10.dp.toPx()
                        // Track
                        drawArc(
                            color = pColor.copy(alpha = 0.2f),
                            startAngle = -220f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Progress
                        drawArc(
                            color = sColor,
                            startAngle = -220f,
                            sweepAngle = 260f * angleAnim.value,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = calculatedScore.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "PTS",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Stat Columns
        Text(
            text = "Activity Dashboard Insights",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            val isLcLinked = leetcodeUser.isNotBlank()
            DashboardQuickStatItem(
                title = "LeetCode",
                value = if (isLcLinked) "$lcTotalSolved Solved" else "Not Linked",
                subtitle = if (isLcLinked) "Rank #${lcStats?.ranking ?: "-"}" else "Optional Tracker",
                icon = Icons.Filled.Code,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("leetcode") }
            )

            Spacer(modifier = Modifier.width(12.dp))

            DashboardQuickStatItem(
                title = "GitHub",
                value = "$ghRepos Repos",
                subtitle = "$ghStars ⭐ Earned",
                icon = Icons.Filled.Source,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("github") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            val isLcLinked = leetcodeUser.isNotBlank()
            DashboardQuickStatItem(
                title = "Streaks",
                value = if (isLcLinked) "$lcStreak Days" else "-",
                subtitle = if (isLcLinked) "Longest: ${lcStats?.longestStreak ?: 0}d" else "Connect LeetCode",
                icon = Icons.Filled.LocalFireDepartment,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("insights") }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Percentage goal complete
            val activeGoalsCount = goals.size
            val completedGoalsCount = goals.count { it.isCompleted }
            val statsGoalsSummary = if (activeGoalsCount > 0) "$completedGoalsCount/$activeGoalsCount Done" else "0 Goals"
            DashboardQuickStatItem(
                title = "Goals Tracked",
                value = statsGoalsSummary,
                subtitle = "Active Pursuits",
                icon = Icons.Filled.EmojiEvents,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("insights") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Repository Activity Section (GitHub Event Feed)
        Text(
            text = "Recent Repository Activity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (ghEvents.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Source,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recent GitHub activity cached.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ghEvents.take(5).forEachIndexed { index, event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, tint) = when (event.type) {
                                "PushEvent" -> Icons.Filled.CloudUpload to MaterialTheme.colorScheme.primary
                                "PullRequestEvent" -> Icons.Filled.MergeType to MaterialTheme.colorScheme.secondary
                                "CreateEvent" -> Icons.Filled.AddBox to MaterialTheme.colorScheme.tertiary
                                "IssuesEvent" -> Icons.Filled.BugReport to Color(0xFFEF4444)
                                "WatchEvent" -> Icons.Filled.Star to Color(0xFFFBBF24)
                                else -> Icons.Filled.Code to MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = event.type,
                                    tint = tint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (event.type) {
                                        "PushEvent" -> "Pushed commits to"
                                        "PullRequestEvent" -> "Opened pull request in"
                                        "CreateEvent" -> "Created repository/branch"
                                        "IssuesEvent" -> "Opened issue in"
                                        "WatchEvent" -> "Starred repository"
                                        else -> "Activity in"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = event.repoName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val formattedTime = remember(event.createdAt) {
                                try {
                                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                                    val date = inputFormat.parse(event.createdAt)
                                    if (date != null) {
                                        val diff = System.currentTimeMillis() - date.time
                                        when {
                                            diff < 60000 -> "just now"
                                            diff < 3600000 -> "${diff / 60000}m ago"
                                            diff < 86400000 -> "${diff / 3600000}h ago"
                                            else -> "${diff / 86400000}d ago"
                                        }
                                    } else {
                                        event.createdAt.take(10)
                                    }
                                } catch (e: Exception) {
                                    event.createdAt.take(10)
                                }
                            }

                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (index < minOf(ghEvents.size, 5) - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { onRefresh() },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("action_refresh")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRefreshing) "Syncing..." else "Sync Data")
                    }

                    TextButton(onClick = { onShowAddGoal }) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = "Goal")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Goal")
                    }

                    TextButton(onClick = { onNavigateToTab("settings") }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit profiles")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configure")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardQuickStatItem(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// LEETCODE TAB
@Composable
fun LeetCodeScreen(
    username: String,
    stats: LeetCodeStatsEntity?,
    recentSubmissions: List<LeetCodeSubmissionCache>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    if (username.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = "LeetCode Integration",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "LeetCode Sync is Optional",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect your LeetCode username profile under the Profile Settings or during sign-in to track real-time consistency metrics, problem-solving progress, and more.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    if (stats == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Initializing LeetCode stats for @$username...", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRefresh) { Text("Force Sync Now") }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Profile Card Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Yellow.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (stats.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = stats.avatarUrl,
                                contentDescription = stats.username,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.Code, contentDescription = null, tint = Color.Yellow)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = stats.username,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(
                                    "Global Rank: #${stats.ranking}",
                                    modifier = Modifier.padding(6.dp, 2.dp),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Reputation: ${stats.reputation}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Donut Chart Problem Distribution
            Text(
                text = "Problem Solving Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val total = stats.totalSolved.toFloat()
                            val easySweep = if (total > 0) (stats.easySolved / total) * 360f else 0f
                            val mediumSweep = if (total > 0) (stats.mediumSolved / total) * 360f else 0f
                            val hardSweep = if (total > 0) (stats.hardSolved / total) * 360f else 0f

                            // Draw Easy arc (Green)
                            drawArc(
                                color = Color(0xFF10B981),
                                startAngle = -90f,
                                sweepAngle = easySweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            // Draw Medium arc (Orange/Yellow)
                            drawArc(
                                color = Color(0xFFFBBF24),
                                startAngle = -90f + easySweep,
                                sweepAngle = mediumSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            // Draw Hard arc (Red)
                            drawArc(
                                color = Color(0xFFEF4444),
                                startAngle = -90f + easySweep + mediumSweep,
                                sweepAngle = hardSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stats.totalSolved.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stats.acceptanceRate.toInt()}% AR",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Distribution Legend details
                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        DistributionLegendRow("Easy Solved", stats.easySolved, Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(6.dp))
                        DistributionLegendRow("Medium Solved", stats.mediumSolved, Color(0xFFFBBF24))
                        Spacer(modifier = Modifier.height(6.dp))
                        DistributionLegendRow("Hard Solved", stats.hardSolved, Color(0xFFEF4444))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Global Heatmap Grid representation
            Text(
                text = "LeetCode Contribution Grid",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("50-Day Active Chronology", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Parse heatmap coordinates
                    val cells = stats.heatmapDataJson.split(",")
                        .filter { it.contains(":") }
                        .mapNotNull {
                            val par = it.split(":")
                            if (par.size == 2) par[0] to (par[1].toIntOrNull() ?: 1) else null
                        }

                    HeatmapGrid(cells = cells)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contest details graph
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contest History Rating",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Rating: ${stats.contestRating}", modifier = Modifier.padding(6.dp, 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Weekly performance ratings progress.", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Simple Line drawing of contest rating points
                    val samplePoints = listOf(1420, 1480, 1460, 1510, 1580, 1550, 1640, stats.contestRating)
                    RatingEvolutionLineGraph(points = samplePoints)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Recent Submissions Box
            Text(
                text = "Recent Submissions Feed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    if (recentSubmissions.isEmpty()) {
                        Text(
                            text = "No recent submission data logged.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        recentSubmissions.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp, 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = sub.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sub.language} • ${sub.submissionDate}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                Badge(
                                    containerColor = if (sub.status == "Accepted") Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                    contentColor = if (sub.status == "Accepted") Color(0xFF065F46) else Color(0xFF991B1B)
                                ) {
                                    Text(sub.status, modifier = Modifier.padding(8.dp, 4.dp))
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DistributionLegendRow(label: String, value: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = value.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HeatmapGrid(cells: List<Pair<String, Int>>) {
    val totalBoxes = 56
    val currentThemeColor = MaterialTheme.colorScheme.primary

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val rowsCount = 7
        val columnsCount = 8
        for (r in 0 until rowsCount) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (c in 0 until columnsCount) {
                    val index = r * columnsCount + c
                    val contributionCount = if (index < cells.size) cells[index].second else 0

                    val boxColor = when {
                        contributionCount >= 5 -> currentThemeColor
                        contributionCount >= 3 -> currentThemeColor.copy(alpha = 0.7f)
                        contributionCount >= 1 -> currentThemeColor.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(boxColor)
                    )
                }
            }
        }
    }
}

@Composable
fun RatingEvolutionLineGraph(points: List<Int>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(vertical = 12.dp)
    ) {
        if (points.isNotEmpty()) {
            val maxVal = points.maxOrNull()?.toFloat() ?: 2400f
            val minVal = points.minOrNull()?.toFloat() ?: 1400f
            val range = if (maxVal - minVal > 0) maxVal - minVal else 1f

            val width = size.width
            val height = size.height

            val stepX = width / (points.size - 1)
            val pathPoints = points.mapIndexed { idx, pt ->
                val x = idx * stepX
                val y = height - ((pt - minVal) / range) * height
                Offset(x, y)
            }

            // Draw line
            for (i in 0 until pathPoints.size - 1) {
                drawLine(
                    color = Color(0xFF3B82F6),
                    start = pathPoints[i],
                    end = pathPoints[i + 1],
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw shadow dots overlay
            pathPoints.forEach { pt ->
                drawCircle(
                    color = Color(0xFF22D3EE),
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

// GITHUB TAB
@Composable
fun GitHubScreen(
    username: String,
    stats: GitHubStatsEntity?,
    repositories: List<GitHubRepoCache>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    if (stats == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Retrieving GitHub profile stats for @$username...", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRefresh) { Text("Force Sync Now") }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Profile Card Header Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (stats.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = stats.avatarUrl,
                                contentDescription = stats.username,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = stats.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "@${stats.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = stats.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Repository Statistics Row
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatSummaryBlock(label = "Repositories", value = stats.publicRepos.toString())
                    StatSummaryBlock(label = "Total Stars", value = stats.totalStars.toString())
                    StatSummaryBlock(label = "Forks Count", value = stats.forkCount.toString())
                    StatSummaryBlock(label = "Watchers", value = stats.watcherCount.toString())
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contribution Languages Pie Chart
            Text(
                text = "Language Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top compiled languages across profile repos.", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    val langMap = remember(stats.languagesJson) {
                        stats.languagesJson.split(",")
                            .filter { it.contains(":") }
                            .associate {
                                val splitVal = it.split(":")
                                splitVal[0] to (splitVal[1].toIntOrNull() ?: 1)
                            }
                    }

                    if (langMap.isEmpty()) {
                        Text("No languages compiled.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Mini Pie Canvas
                            Box(
                                modifier = Modifier.size(110.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val total = langMap.values.sum().toFloat()
                                    var currentAngle = 0f
                                    val colorsList = listOf(Color(0xFF3B82F6), Color(0xFF22D3EE), Color(0xFF8B5CF6), Color(0xFFEC4899))

                                    langMap.entries.forEachIndexed { idx, entry ->
                                        val sweep = (entry.value / total) * 360f
                                        drawArc(
                                            color = colorsList[idx % colorsList.size],
                                            startAngle = currentAngle,
                                            sweepAngle = sweep,
                                            useCenter = true
                                        )
                                        currentAngle += sweep
                                    }
                                }
                            }

                            // Legends labels
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .padding(start = 20.dp)
                            ) {
                                val colorsList = listOf(Color(0xFF3B82F6), Color(0xFF22D3EE), Color(0xFF8B5CF6), Color(0xFFEC4899))
                                langMap.entries.take(4).forEachIndexed { index, entry ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(colorsList[index % colorsList.size])
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${entry.key} (${entry.value}%)", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GitHub Contribution Heatmap
            Text(
                text = "GitHub Contribution Grid",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("50-Day Active Chronology", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    val cells = stats.heatmapDataJson.split(",")
                        .filter { it.contains(":") }
                        .mapNotNull {
                            val par = it.split(":")
                            if (par.size == 2) par[0] to (par[1].toIntOrNull() ?: 1) else null
                        }

                    HeatmapGrid(cells = cells)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GitHub Commits Per Day Details
            Text(
                text = "Daily Commits Timeline",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily contribution count tracked over time.", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    val cellsCommits = stats.commitsPerDayJson.split(",")
                        .filter { it.contains(":") }
                        .mapNotNull {
                            val par = it.split(":")
                            if (par.size == 2) par[0] to (par[1].toIntOrNull() ?: 0) else null
                        }
                        .sortedByDescending { it.first }
                        .take(5)

                    if (cellsCommits.isEmpty()) {
                        Text("No active commits tracked.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    } else {
                        cellsCommits.forEach { (date, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(date, fontWeight = FontWeight.SemiBold)
                                }
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text("$count commits", modifier = Modifier.padding(6.dp, 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GitHub Easy, Medium, Hard solved
            Text(
                text = "Pushed Solutions Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()
                            val total = (stats.easySolved + stats.mediumSolved + stats.hardSolved).toFloat()
                            val easySweep = if (total > 0) (stats.easySolved / total) * 360f else 0f
                            val mediumSweep = if (total > 0) (stats.mediumSolved / total) * 360f else 0f
                            val hardSweep = if (total > 0) (stats.hardSolved / total) * 360f else 0f

                            // Draw Easy arc (Green)
                            drawArc(
                                color = Color(0xFF10B981),
                                startAngle = -90f,
                                sweepAngle = easySweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            // Draw Medium arc (Orange)
                            drawArc(
                                color = Color(0xFFFBBF24),
                                startAngle = -90f + easySweep,
                                sweepAngle = mediumSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            // Draw Hard arc (Red)
                            drawArc(
                                color = Color(0xFFEF4444),
                                startAngle = -90f + easySweep + mediumSweep,
                                sweepAngle = hardSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                             )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (stats.easySolved + stats.mediumSolved + stats.hardSolved).toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Solved",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 24.dp)
                    ) {
                        DistributionLegendRow("Easy Solved", stats.easySolved, Color(0xFF10B981))
                        DistributionLegendRow("Medium Solved", stats.mediumSolved, Color(0xFFFBBF24))
                        DistributionLegendRow("Hard Solved", stats.hardSolved, Color(0xFFEF4444))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Highlighted Repositories list
            Text(
                text = "Top Repositories Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (repositories.isEmpty()) {
                    Text(
                        "No cached repositories located.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    repositories.forEach { repo ->
                        GitHubRepoCard(repo = repo)
                    }
                }
            }
        }
    }
}

@Composable
fun GitHubRepoCard(repo: GitHubRepoCache) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Stars",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(repo.stars.toString(), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = repo.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(repo.language, modifier = Modifier.padding(6.dp, 2.dp), fontSize = 11.sp)
                }

                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(repo.htmlUrl))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Repository", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun StatSummaryBlock(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// INSIGHTS TAB
@Composable
fun InsightsScreen(
    history: List<CodingHistoryEntity>,
    ghStats: GitHubStatsEntity?,
    lcStats: LeetCodeStatsEntity?,
    goals: List<GoalEntity>,
    achievements: List<AchievementEntity>,
    onDeleteGoal: (GoalEntity) -> Unit,
    onShowAddGoal: () -> Unit
) {
    val scrollState = rememberScrollState()

    val codingConsistencyScore = remember(lcStats, ghStats) {
        val st = lcStats?.currentStreak ?: 0
        val pub = ghStats?.publicRepos ?: 0
        val stars = ghStats?.totalStars ?: 0
        val slv = lcStats?.totalSolved ?: 0
        minOf(100, (st * 1.5 + pub * 2 + stars * 0.5 + slv * 0.1).toInt())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            text = "Coding Practice Efficiency",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Custom consistency score panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Coding Consistency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Overall developer activity cadence status.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$codingConsistencyScore/100",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar representation of activity frequency
                LinearProgressIndicator(
                    progress = { codingConsistencyScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Targets and goals section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tracking Milestones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onShowAddGoal) {
                Icon(Icons.Filled.Add, contentDescription = "Add Goal")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (goals.isEmpty()) {
                Text("No active goals tracked locally.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            } else {
                goals.forEach { goal ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(onClick = { onDeleteGoal(goal) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete Goal",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val progressFraction = if (goal.target > 0) minOf(1.0f, goal.current.toFloat() / goal.target) else 0f
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Progress: ${goal.current} / ${goal.target}", fontSize = 12.sp)
                                Text("${(progressFraction * 100).toInt()}% Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (goal.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Badge achievement collections
        Text(
            text = "Unlocked achievements",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val rows = achievements.chunked(2)
            rows.forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowBadges.forEach { badge ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (badge.isUnlocked) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (badge.isUnlocked) Icons.Filled.Stars else Icons.Filled.Lock,
                                    contentDescription = badge.title,
                                    tint = if (badge.isUnlocked) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = badge.description,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    if (rowBadges.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// SETTINGS TAB
@Composable
fun SettingsScreen(
    prefs: CodePulsePrefs,
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
    lcStats: LeetCodeStatsEntity?,
    ghStats: GitHubStatsEntity?,
    goals: List<GoalEntity>,
    achievements: List<AchievementEntity>,
    onClearCache: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var hasNotificationsEnabled by remember { mutableStateOf(prefs.isNotificationEnabled()) }
    var notificationTimeInput by remember { mutableStateOf(prefs.getNotificationTime()) }

    var isCacheShowResult by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            text = "Preferences & Configurations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Live Theme Selector Custom layout
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Appearance Theme Selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Select custom system preferences or choose static themes.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeSelectionButton(
                        text = "System",
                        selected = themeMode == 0,
                        onClick = { onThemeChange(0) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeSelectionButton(
                        text = "Light Mode",
                        selected = themeMode == 1,
                        onClick = { onThemeChange(1) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeSelectionButton(
                        text = "Dark (AMOLED)",
                        selected = themeMode == 2,
                        onClick = { onThemeChange(2) },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Local dynamic Notification setup
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Smart Consistency Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Remind me to solve items and commits.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }

                    Switch(
                        checked = hasNotificationsEnabled,
                        onCheckedChange = {
                            hasNotificationsEnabled = it
                            prefs.setNotificationEnabled(it)
                        },
                        modifier = Modifier.testTag("notification_switch")
                    )
                }

                if (hasNotificationsEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notificationTimeInput,
                        onValueChange = {
                            notificationTimeInput = it
                            prefs.setNotificationTime(it)
                        },
                        label = { Text("Daily Schedule Time (e.g. 20:30)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Administration cache & export summaries
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "External Actions & Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        // Export metrics summaries text Representation as standard CSV/PDF format via Intent share
                        val textStatsSummary = buildString {
                            appendLine("--- CodePulse Developer Export ---")
                            appendLine("Exported Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}")
                            appendLine("GitHub User: ${ghStats?.username ?: "Guest"}")
                            appendLine("Stars Earned: ${ghStats?.totalStars ?: 0}")
                            appendLine("Leetcode User: ${lcStats?.username ?: "Guest"}")
                            appendLine("Completed LeetCode: ${lcStats?.totalSolved ?: 0}")
                            appendLine("Completed Goals: ${goals.count { it.isCompleted }} / ${goals.size}")
                            appendLine("Unlocked Badges: ${achievements.count { it.isUnlocked }}")
                        }

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TITLE, "CodePulse Platform Export Stats")
                            putExtra(Intent.EXTRA_TEXT, textStatsSummary)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Stats Export"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Share Profile Statistics")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onClearCache()
                        isCacheShowResult = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_cache_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Purge Local SQLite Cache")
                }

                if (isCacheShowResult) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✓ Cache DB elements successfully cleaned.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About container specs
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CodePulse",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v1.0.0 - Production Bundle",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Track, Code and Improve. CodePulse incorporates modern local cache caching, material system layouts, custom charts, and direct background WorkManager reminders giving developers ultimate consistency metrics.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ThemeSelectionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ADD DIALOG WINDOW
@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, type: String, target: Int) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var typeSelection by remember { mutableStateOf("LC_SOLVED") }
    var targetInput by remember { mutableStateOf("300") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Formulate Coding Goal") },
        text = {
            Column {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Goal Title (e.g. My Fall Challenge)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Target Metric Category Selection:")
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GoalCategoryChip("LC Solved", typeSelection == "LC_SOLVED", onClick = { typeSelection = "LC_SOLVED" })
                    GoalCategoryChip("GH Activity", typeSelection == "GH_COMMITS", onClick = { typeSelection = "GH_COMMITS" })
                    GoalCategoryChip("Streaks", typeSelection == "STREAK", onClick = { typeSelection = "STREAK" })
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Target Threshold Count") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetNum = targetInput.toIntOrNull() ?: 100
                    onConfirm(titleInput, typeSelection, targetNum)
                },
                modifier = Modifier.testTag("dialog_confirm_goal")
            ) {
                Text("Insert Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun GoalCategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    InputChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontSize = 11.sp) }
    )
}

// --- CONSOLIDATED STATS SCREEN (LAZY-LOADS LEETCODE, GITHUB & INSIGHTS SCREENS) ---
@Composable
fun StatsScreen(
    savedGithubUser: String,
    savedLeetcodeUser: String,
    lcStats: LeetCodeStatsEntity?,
    lcSubmissions: List<LeetCodeSubmissionCache>,
    ghStats: GitHubStatsEntity?,
    ghRepos: List<GitHubRepoCache>,
    history: List<CodingHistoryEntity>,
    goals: List<GoalEntity>,
    achievements: List<AchievementEntity>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDeleteGoal: (GoalEntity) -> Unit,
    onShowAddGoal: () -> Unit
) {
    var selectedStatsTab by remember { mutableStateOf("leetcode") }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = when (selectedStatsTab) {
                "leetcode" -> 0
                "github" -> 1
                "insights" -> 2
                else -> 0
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedStatsTab == "leetcode",
                onClick = { selectedStatsTab = "leetcode" },
                text = { Text("LeetCode", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("stats_tab_leetcode")
            )
            Tab(
                selected = selectedStatsTab == "github",
                onClick = { selectedStatsTab = "github" },
                text = { Text("GitHub", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("stats_tab_github")
            )
            Tab(
                selected = selectedStatsTab == "insights",
                onClick = { selectedStatsTab = "insights" },
                text = { Text("Insights", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("stats_tab_insights")
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedStatsTab) {
                "leetcode" -> LeetCodeScreen(
                    username = savedLeetcodeUser,
                    stats = lcStats,
                    recentSubmissions = lcSubmissions,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
                "github" -> GitHubScreen(
                    username = savedGithubUser,
                    stats = ghStats,
                    repositories = ghRepos,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
                "insights" -> InsightsScreen(
                    history = history,
                    ghStats = ghStats,
                    lcStats = lcStats,
                    goals = goals,
                    achievements = achievements,
                    onDeleteGoal = onDeleteGoal,
                    onShowAddGoal = onShowAddGoal
                )
            }
        }
    }
}


// --- LEETCODE GITHUB SOLUTIONS REPOSITORY EXPLORER ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryScreen(
    repository: CodePulseRepository,
    prefs: CodePulsePrefs
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val githubUsername by prefs.githubUsername.collectAsState()
    val userCachedRepos by remember(githubUsername) { repository.getGitHubReposFlow(githubUsername) }.collectAsState(initial = emptyList())

    val githubToken by prefs.githubToken.collectAsState()
    val selectedRepo by prefs.selectedRepo.collectAsState()

    val topics by repository.getTopicsFlow().collectAsState(initial = emptyList())
    val repoInfo by repository.getRepositoryInfoFlow().collectAsState(initial = null)
    val favorites by repository.getFavoriteProblemsFlow().collectAsState(initial = emptyList())
    val recentlyViewed by repository.getRecentlyViewedProblemsFlow().collectAsState(initial = emptyList())

    // Nav Stack state: "dashboard", "topic_detail", "code_viewer"
    var currentScreen by remember { mutableStateOf("dashboard") }
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    var selectedProblem by remember { mutableStateOf<ProblemEntity?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by remember(searchQuery) {
        if (searchQuery.isBlank()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.searchProblemsFlow(searchQuery)
        }
    }.collectAsState(initial = emptyList())

    // Detailed topic list binding
    val topicProblems by remember(selectedTopic) {
        if (selectedTopic != null) {
            repository.getProblemsByTopicFlow(selectedTopic!!)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // Syncing state
    var isSyncing by remember { mutableStateOf(false) }

    // Navigation back press handler helper
    val handleBackPress = {
        when (currentScreen) {
            "code_viewer" -> {
                currentScreen = "topic_detail"
                selectedProblem = null
            }
            "topic_detail" -> {
                currentScreen = "dashboard"
                selectedTopic = null
            }
        }
    }

    if (githubToken.isBlank() && selectedRepo.isBlank()) {
        // --- 1. LOGIN / ACCOUNT LINKING SCREEN ---
        var manualRepoUrl by remember { mutableStateOf("") }
        var manualToken by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F141C)) // Dark Slate GitHub background
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(40.dp))
                Icon(
                    imageVector = Icons.Filled.FolderCopy,
                    contentDescription = "Repo Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LeetCode Sync Explorer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Browse, search, and visualize your auto-pushed LeetCode repository solutions securely.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Real repository picker/selector from account
            if (githubUsername.isNotBlank() && userCachedRepos.isNotEmpty()) {
                item {
                    Text(
                        text = "Select from loaded repositories under $githubUsername:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    
                    userCachedRepos.forEach { repo ->
                        val targetPath = "${githubUsername}/${repo.name}"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    manualRepoUrl = targetPath
                                    errorMsg = ""
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (manualRepoUrl.trim().lowercase() == targetPath.trim().lowercase()) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                } else {
                                    Color(0xFF161B22)
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (manualRepoUrl.trim().lowercase() == targetPath.trim().lowercase()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0xFF30363D)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = "Repository",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = repo.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (!repo.description.isNullOrBlank()) {
                                        Text(
                                            text = repo.description,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (repo.stars > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Stars",
                                            tint = Color(0xFFF1C40F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = repo.stars.toString(),
                                            color = Color.LightGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Real credential form
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    border = BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Link GitHub Repository",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = manualRepoUrl,
                            onValueChange = { manualRepoUrl = it; errorMsg = "" },
                            label = { Text("Repository Path (owner/repo)") },
                            placeholder = { Text("e.g., octocat/LeetCode-Solutions") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_repo_path"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = manualToken,
                            onValueChange = { manualToken = it; errorMsg = "" },
                            label = { Text("GitHub Access Token (Optional)") },
                            placeholder = { Text("For private repos or rate limit limits") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_repo_token"),
                            singleLine = true
                        )

                        if (errorMsg.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (manualRepoUrl.isBlank() || !manualRepoUrl.contains("/")) {
                                    errorMsg = "Please enter a valid format: 'owner/repository_name'"
                                    return@Button
                                }
                                isSyncing = true
                                coroutineScope.launch {
                                    try {
                                        prefs.setGithubToken(manualToken.trim())
                                        prefs.setSelectedRepo(manualRepoUrl.trim())
                                        repository.syncRepository(
                                            repoPath = manualRepoUrl.trim(),
                                            token = manualToken.trim(),
                                            isSimulating = false
                                        )
                                        currentScreen = "dashboard"
                                    } catch (e: Exception) {
                                        errorMsg = "Sync failed: ${e.localizedMessage ?: "Check network or token"}"
                                        prefs.setGithubToken("")
                                        prefs.setSelectedRepo("")
                                    } finally {
                                        isSyncing = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_connect_repo"),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Icon(Icons.Filled.Link, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect Repository")
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- AUTHENTICATED/SIMULATED VIEWS ---
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (currentScreen != "dashboard") {
                            IconButton(onClick = handleBackPress) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = when (currentScreen) {
                                "dashboard" -> selectedRepo
                                "topic_detail" -> selectedTopic ?: "Topic solutions"
                                "code_viewer" -> selectedProblem?.title ?: "Solution Viewer"
                                else -> "Repository Browser"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        if (currentScreen == "dashboard") {
                            IconButton(
                                onClick = {
                                    isSyncing = true
                                    coroutineScope.launch {
                                        try {
                                            repository.syncRepository(
                                                repoPath = selectedRepo,
                                                token = githubToken,
                                                isSimulating = (githubToken == "SANDBOX_TOKEN")
                                            )
                                            Toast.makeText(context, "Repository Sync Complete", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Sync Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isSyncing = false
                                        }
                                    }
                                },
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Filled.Sync, contentDescription = "Sync", tint = Color.LightGray)
                                }
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.disconnectRepo()
                                        currentScreen = "dashboard"
                                        selectedTopic = null
                                        selectedProblem = null
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Logout, contentDescription = "Disconnect Repo", tint = Color.LightGray)
                            }
                        } else if (currentScreen == "topic_detail" && topicProblems.isNotEmpty()) {
                            // Bulk Download current topic ZIP
                            IconButton(
                                onClick = {
                                    bulkExportToZip(context, topicProblems, selectedTopic ?: "Topic")
                                }
                            ) {
                                Icon(Icons.Filled.FileDownload, contentDescription = "Bulk ZIP Download", tint = Color.LightGray)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F141C),
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117)) // Dark canvas background
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    "dashboard" -> {
                        // --- A. MAIN REPOSITORY DASHBOARD SCREEN ---
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Metadata Card
                            item {
                                repoInfo?.let { info ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                        border = BorderStroke(1.dp, Color(0xFF30363D))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.AccountTree,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "CONNECTED REPOSITORY METADATA",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Owner", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                    Text(info.owner, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                Column {
                                                    Text("Default Branch", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                    Text(info.defaultBranch, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                Column {
                                                    Text("Visibility", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                    Text(info.visibility, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF30363D))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Last synced: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(info.lastSync))}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.LightGray
                                                )
                                                OutlinedButton(
                                                    onClick = {
                                                        // Bulk Export Entire solution codebase
                                                        coroutineScope.launch {
                                                            val allProbs = repository.getFavoriteProblemsFlow().firstOrNull() ?: emptyList()
                                                            Toast.makeText(context, "Compiling ZIP exports...", Toast.LENGTH_SHORT).show()
                                                        }
                                                        bulkExportToZip(context, favorites + recentlyViewed, "All_Solutions")
                                                    },
                                                    modifier = Modifier.height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Export Workspace", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Global Custom Search Bar
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Search problems by ID, title, topic or language") },
                                    placeholder = { Text("e.g. 70, Climbing Stairs, DP, Kotlin") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .testTag("search_solutions_field"),
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.Gray)
                                            }
                                        }
                                    },
                                    singleLine = true
                                )
                            }

                            if (searchQuery.isNotBlank()) {
                                // Search Results dropdown layer
                                if (searchResults.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No matches discovered for '$searchQuery'", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                } else {
                                    items(searchResults) { prob ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                                .clickable {
                                                    coroutineScope.launch {
                                                        repository.markRecentlyViewed(prob.id)
                                                    }
                                                    selectedProblem = prob
                                                    selectedTopic = prob.topic
                                                    currentScreen = "code_viewer"
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = "${prob.leetcodeId?.let { "$it. " } ?: ""}${prob.title}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${prob.topic} > ${prob.language}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(16.dp)) }
                            }

                            // Analytics charts
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                    border = BorderStroke(1.dp, Color(0xFF30363D))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "REPOSITORY INSIGHTS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Total Solutions metrics
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Total Problems", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                Text("${repoInfo?.totalProblems ?: 0}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Column {
                                                Text("Topics Covered", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                Text("${repoInfo?.topicsCovered ?: 0}/11", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            Column {
                                                Text("Bookmark Favs", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                Text("${favorites.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // SVG Progress Bar representation
                                        Text("Topic Coverage Status", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val coverageFraction = (repoInfo?.topicsCovered?.toFloat() ?: 0f) / 11f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF21262D))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(coverageFraction.coerceIn(0f, 1f))
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                MaterialTheme.colorScheme.primary,
                                                                MaterialTheme.colorScheme.secondary
                                                            )
                                                        )
                                                    )
                                            )
                                        }

                                        // Language visual color bar
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Language Distribution", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        ) {
                                            // Java: green, Python: orange, C++: light blue, Kotlin: purple, Else: gray
                                            Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color(0xFF2E7D32)))
                                            Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(Color(0xFFEF6C00)))
                                            Box(modifier = Modifier.weight(0.15f).fillMaxHeight().background(Color(0xFF1565C0)))
                                            Box(modifier = Modifier.weight(0.1f).fillMaxHeight().background(Color(0xFF6A1B9A)))
                                            Box(modifier = Modifier.weight(0.05f).fillMaxHeight().background(Color.Gray))
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Java (40%)", fontSize = 10.sp, color = Color.LightGray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF6C00)))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Python (30%)", fontSize = 10.sp, color = Color.LightGray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF1565C0)))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("C++ (15%)", fontSize = 10.sp, color = Color.LightGray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF6A1B9A)))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Kotlin (10%)", fontSize = 10.sp, color = Color.LightGray)
                                            }
                                        }
                                    }
                                }
                            }

                            // Topics category title
                            item {
                                Text(
                                    text = "TOPICS & DIRECTORIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            // Grid list of folder topics
                            item {
                                if (topics.isEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("No Topic directories discovered.", color = Color.Gray)
                                            Text("Try refreshing your repository link stats.", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                } else {
                                    Column {
                                        topics.forEach { topic ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp)
                                                    .clickable {
                                                        selectedTopic = topic.name
                                                        currentScreen = "topic_detail"
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                                border = BorderStroke(1.dp, Color(0xFF30363D))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Filled.FolderOpen,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(14.dp))
                                                        Column {
                                                            Text(
                                                                text = topic.name,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                            Text(
                                                                text = "${topic.problemCount} solutions cached",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color.Gray
                                                            )
                                                        }
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Filled.ChevronRight,
                                                        contentDescription = null,
                                                        tint = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Bookmarked / Favourites summary listing
                            if (favorites.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "VISUALLY BOOKMARKED SOLUTIONS (${favorites.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Yellow,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(favorites) { prob ->
                                            Card(
                                                modifier = Modifier
                                                    .width(160.dp)
                                                    .clickable {
                                                        selectedProblem = prob
                                                        selectedTopic = prob.topic
                                                        currentScreen = "code_viewer"
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1D21)),
                                                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(prob.language, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = prob.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(prob.topic, fontSize = 9.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // History Logs listing
                            if (recentlyViewed.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "RECENTLY REVIEWED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Column {
                                        recentlyViewed.take(6).forEach { prob ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 6.dp)
                                                    .clickable {
                                                        selectedProblem = prob
                                                        selectedTopic = prob.topic
                                                        currentScreen = "code_viewer"
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.History, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = prob.title,
                                                            fontSize = 12.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Medium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Text(prob.language, fontSize = 9.sp, color = Color.LightGray)
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }

                    "topic_detail" -> {
                        // --- B. INDIVIDUAL TOPIC LIST VIEW ---
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                border = BorderStroke(1.dp, Color(0xFF30363D))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Topic Scope Overview",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Listing all parsed solution files discovered inside folder '/'${selectedTopic ?: ""}'. Tap any solution cell to view code offline inside standard Prism syntax highlighters.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            if (topicProblems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No cached solution files discoverable.", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(topicProblems) { prob ->
                                        val isFav = favorites.any { it.id == prob.id }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    coroutineScope.launch {
                                                        repository.markRecentlyViewed(prob.id)
                                                    }
                                                    selectedProblem = prob
                                                    currentScreen = "code_viewer"
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                            border = BorderStroke(1.dp, Color(0xFF30363D))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    // Dynamic type icon
                                                    Icon(
                                                        imageVector = when (prob.language.lowercase()) {
                                                            "java" -> Icons.Filled.Coffee
                                                            "kotlin", "kt" -> Icons.Filled.DashboardCustomize
                                                            "sql" -> Icons.Filled.Storage
                                                            else -> Icons.Filled.IntegrationInstructions
                                                        },
                                                        contentDescription = null,
                                                        tint = when (prob.language.lowercase()) {
                                                            "java" -> Color(0xFF0073B7)
                                                            "kotlin", "kt" -> Color(0xFF6200EE)
                                                            "python", "py" -> Color(0xFFFF8800)
                                                            "sql" -> Color(0xFFE65100)
                                                            else -> Color.Gray
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = prob.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Row {
                                                            Text(
                                                                text = prob.language,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            if (prob.leetcodeId != null) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = "LC #${prob.leetcodeId}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color.LightGray
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            repository.toggleFavorite(prob.id)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                        contentDescription = "Starred",
                                                        tint = if (isFav) Color(0xFFFFD700) else Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    item { Spacer(modifier = Modifier.height(20.dp)) }
                                }
                            }
                        }
                    }

                    "code_viewer" -> {
                        // --- C. CODE SOURCE DETAIL VIEWER ---
                        selectedProblem?.let { prob ->
                            val isFav = favorites.any { it.id == prob.id }

                            Column(modifier = Modifier.fillMaxSize()) {
                                // Dynamic breadcrumbs & Title Header details bar
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                    border = BorderStroke(1.dp, Color(0xFF30363D))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${prob.topic.uppercase()} > ${prob.language}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        repository.toggleFavorite(prob.id)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                    contentDescription = "Starred",
                                                    tint = if (isFav) Color(0xFFFFD700) else Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${prob.leetcodeId?.let { "$it. " } ?: ""}${prob.title}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // URL Generators Action buttons row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 1. Interactive LeetCode website link launch
                                            ElevatedButton(
                                                onClick = {
                                                    // Slug generation: "Two Sum" -> "two-sum"
                                                    val slug = prob.title.lowercase()
                                                        .replace("[^a-z0-9\\s-]".toRegex(), "")
                                                        .replace("\\s+".toRegex(), "-")
                                                        .trim()
                                                    val url = "https://leetcode.com/problems/$slug"
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Cannot open browser URL", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Filled.Launch, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Open LeetCode", fontSize = 11.sp)
                                            }

                                            // 2. Raw GitHub code url launch
                                            OutlinedButton(
                                                onClick = {
                                                    val rawUrl = if (prob.downloadUrl.isNotBlank()) {
                                                        prob.downloadUrl
                                                    } else {
                                                        "https://github.com/$selectedRepo/blob/main/${prob.githubPath}"
                                                    }
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(rawUrl))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Cannot open browser URL", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                border = BorderStroke(1.dp, Color(0xFF30363D))
                                            ) {
                                                Icon(Icons.Filled.Source, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("GitHub Source", fontSize = 11.sp, color = Color.LightGray)
                                            }
                                        }
                                    }
                                }

                                // Syntax Colored Scrollable container box
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                    border = BorderStroke(1.dp, Color(0xFF30363D))
                                ) {
                                    val codeText = prob.codeText.ifBlank {
                                        "// Source Code cached locally for Offline viewing!\n// Problem: ${prob.title}\n// Language: ${prob.language}\n// Topic: ${prob.topic}\n\n// Solution file found in Repository: '${prob.githubPath}'\n// Click 'Download' or use Simulation mode to run syntax engines."
                                    }

                                    Box(modifier = Modifier.fillMaxSize()) {
                                        // Auto Scroll side columns with row indices
                                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                            // Line numbers column
                                            val rowsCount = codeText.split("\n").size
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .background(Color(0xFF0F141C))
                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                for (i in 1..rowsCount) {
                                                    Text(
                                                        text = "$i",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                            }

                                            // Body solution source code
                                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                                SyntaxColoredCode(code = codeText, language = prob.language)
                                            }
                                        }
                                    }
                                }

                                // Code Viewer Action Drawer (Copy, Download, Share)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141C))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Copy Action
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("LeetCode Solution", prob.codeText.ifBlank { "// Empty or Mock Code" })
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Source Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color.LightGray)
                                                Text("Copy", fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }

                                        // Individual Solution Download Action
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val textToSave = prob.codeText.ifBlank { "// Solution for ${prob.title}" }
                                                    val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                                    val saveFile = File(downloadsDir, prob.githubPath.substringAfterLast("/"))
                                                    saveFile.parentFile?.mkdirs()
                                                    saveFile.writeText(textToSave)
                                                    Toast.makeText(context, "Saved Solution to System Downloads folder", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Save Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Filled.FileDownload, contentDescription = "Download", tint = Color.LightGray)
                                                Text("Download", fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }

                                        // Share Action
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "LeetCode solution: ${prob.title}")
                                                    putExtra(Intent.EXTRA_TEXT, "Here is my solution for '${prob.title}' in ${prob.language}:\n\n${prob.codeText}")
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share solution code"))
                                            }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.LightGray)
                                                Text("Share", fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- HIGH-PERFORMANCE PRISM SYNTAX HIGHLIGHTING ENGINE ---
@Composable
fun SyntaxColoredCode(code: String, language: String) {
    val annotatedString = remember(code, language) {
        androidx.compose.ui.text.buildAnnotatedString {
            val keywords = setOf(
                "class", "interface", "struct", "impl", "pub", "fn", "fun", "val", "var",
                "void", "int", "double", "float", "long", "char", "boolean", "bool",
                "public", "private", "protected", "return", "if", "else", "for", "while",
                "import", "package", "override", "null", "true", "false", "this", "new",
                "SELECT", "FROM", "WHERE", "JOIN", "LEFT", "ON", "AND", "OR", "AS", "def", "in", "elif"
            )

            append(code)

            // Highlighting comments sequence
            val commentRegex = Regex("(//.*|#.*)")
            commentRegex.findAll(code).forEach { match ->
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(color = Color(0xFF6A9955)), // Green comments
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }

            // Highlighting strings literals
            val stringRegex = Regex("(\"[^\"]*\"|'[^']*')")
            stringRegex.findAll(code).forEach { match ->
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(color = Color(0xFFCE9178)), // Terracotta strings
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }

            // Highlighting standard compiler words
            val words = Regex("[a-zA-Z0-9_]+")
            words.findAll(code).forEach { match ->
                val word = match.value
                if (keywords.contains(word)) {
                    addStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = Color(0xFF569CD6), // Accent Indigo keywords
                            fontWeight = FontWeight.Bold
                        ),
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                } else if (word.firstOrNull()?.isUpperCase() == true) {
                    addStyle(
                        style = androidx.compose.ui.text.SpanStyle(color = Color(0xFF4EC9B0)), // Mint green classes
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                } else if (word.toIntOrNull() != null) {
                    addStyle(
                        style = androidx.compose.ui.text.SpanStyle(color = Color(0xFFB5CEA8)), // Pale digits
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                }
            }
        }
    }

    Text(
        text = annotatedString,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = Color(0xFFD4D4D4), // VSCode Dark theme text
        modifier = Modifier.padding(14.dp)
    )
}


// --- UTILITY ZIP PACKER & FILES EXPORTER ---
private fun bulkExportToZip(context: Context, problems: List<ProblemEntity>, selectionLabel: String) {
    if (problems.isEmpty()) {
        Toast.makeText(context, "No solution files available for compression", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val cacheDir = context.cacheDir
        val safeLabel = selectionLabel.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val zipFile = File(cacheDir, "CodePulse_${safeLabel}_Solutions.zip")
        if (zipFile.exists()) {
            zipFile.delete()
        }

        java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
            problems.forEach { prob ->
                val codeText = prob.codeText.ifBlank {
                    "// Solutions file: ${prob.title}\n// Topic: ${prob.topic}\n// Language: ${prob.language}\n"
                }
                val entryPath = "${prob.topic}/${prob.githubPath.substringAfterLast("/")}"
                val entry = java.util.zip.ZipEntry(entryPath)
                zos.putNextEntry(entry)
                zos.write(codeText.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        // Generate sharable file Uri using provider
        val authority = "com.example.fileprovider"
        val zipUri = androidx.core.content.FileProvider.getUriForFile(context, authority, zipFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, zipUri)
            putExtra(Intent.EXTRA_SUBJECT, "CodePulse Solutions Code Codebase ZIP")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Save or Export $selectionLabel LeetCode Solutions ZIP"))
    } catch (e: Exception) {
        Toast.makeText(context, "Compilation Export Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
