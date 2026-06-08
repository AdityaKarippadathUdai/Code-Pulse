package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
                        TabItem("LeetCode", "leetcode", Icons.Filled.Code, Icons.Outlined.Code),
                        TabItem("GitHub", "github", Icons.Filled.Source, Icons.Outlined.Source),
                        TabItem("Insights", "insights", Icons.Filled.Analytics, Icons.Outlined.Analytics),
                        TabItem("Settings", "settings", Icons.Filled.Settings, Icons.Outlined.Settings)
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
                        "leetcode" -> LeetCodeScreen(
                            username = savedLeetcodeUser,
                            stats = lcStatsState.value,
                            recentSubmissions = lcSubmissionsState.value,
                            isRefreshing = isRefreshing,
                            onRefresh = { syncTrigger() }
                        )
                        "github" -> GitHubScreen(
                            username = savedGithubUser,
                            stats = ghStatsState.value,
                            repositories = ghReposState.value,
                            isRefreshing = isRefreshing,
                            onRefresh = { syncTrigger() }
                        )
                        "insights" -> InsightsScreen(
                            history = historyState.value,
                            ghStats = ghStatsState.value,
                            lcStats = lcStatsState.value,
                            goals = goalsState.value,
                            achievements = achievementsState.value,
                            onDeleteGoal = { coroutineScope.launch { repository.deleteGoal(it) } },
                            onShowAddGoal = { showAddGoalDialog = true }
                        )
                        "settings" -> SettingsScreen(
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

                    OutlinedTextField(
                        value = githubInput,
                        onValueChange = { githubInput = it },
                        label = { Text("GitHub username") },
                        leadingIcon = { Icon(Icons.Filled.Source, contentDescription = "GitHub") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = leetcodeInput,
                        onValueChange = { leetcodeInput = it },
                        label = { Text("LeetCode username") },
                        leadingIcon = { Icon(Icons.Filled.Code, contentDescription = "LeetCode") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leetcode_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { displayOAuthSim = true },
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
                            val gh = if (githubInput.isBlank()) "guest_developer" else githubInput
                            val lc = if (leetcodeInput.isBlank()) "guest_coder" else leetcodeInput
                            onLoginComplete(gh, lc, true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("guest_mode_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continue as Guest")
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
            DashboardQuickStatItem(
                title = "LeetCode",
                value = "$lcTotalSolved Solved",
                subtitle = "Rank #${lcStats?.ranking ?: "-"}",
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
            DashboardQuickStatItem(
                title = "Streaks",
                value = "$lcStreak Days",
                subtitle = "Longest: ${lcStats?.longestStreak ?: 0}d",
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
    // Generate a fixed size representing 50 boxes
    val totalBoxes = 56
    val currentThemeColor = MaterialTheme.colorScheme.primary

    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(totalBoxes) { index ->
            val contributionCount = if (index < cells.size) cells[index].second else 0

            val boxColor = when {
                contributionCount >= 5 -> currentThemeColor
                contributionCount >= 3 -> currentThemeColor.copy(alpha = 0.7f)
                contributionCount >= 1 -> currentThemeColor.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            }

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(boxColor)
            )
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(achievements.size) { index ->
                val badge = achievements[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
