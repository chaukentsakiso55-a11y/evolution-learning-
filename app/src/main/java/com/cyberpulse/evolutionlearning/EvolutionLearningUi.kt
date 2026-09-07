package com.cyberpulse.evolutionlearning

import android.speech.tts.TextToSpeech
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cyberpulse.evolutionlearning.ai.AiFeature
import com.cyberpulse.evolutionlearning.ai.AiRepository
import com.cyberpulse.evolutionlearning.data.FirebaseRepository
import com.cyberpulse.evolutionlearning.model.HomeworkItem
import com.cyberpulse.evolutionlearning.model.Progress
import com.cyberpulse.evolutionlearning.model.StudyGoal
import com.cyberpulse.evolutionlearning.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

private val Accent = Color(0xFF38BDF8)
private val Blue = Color(0xFF2563EB)
private val Purple = Color(0xFFC084FC)
private val Gold = Color(0xFFFBBF24)
private val Orange = Color(0xFFFB923C)
private val Good = Color(0xFF4ADE80)
private val Red = Color(0xFFF87171)
private val Bg = Color(0xFF050F1A)
private val CardBg = Color(0x0AFFFFFF)
private val HeaderBg = Color(0xE6050F1A)
private val Muted = Color(0x80FFFFFF)
private val Faint = Color(0x61FFFFFF)

private enum class RootTab(val label: String, val icon: String) {
    HOME("Home", "🏠"),
    STUDY("Study", "📖"),
    AI("AI", "🤖"),
    QUIZ("Quiz", "✏️"),
    STATS("Stats", "📊"),
    ME("Me", "👤"),
    ALERTS("Alerts", "🔔")
}

@Composable
fun EvolutionLearningRoot() {
    val repository = remember { FirebaseRepository() }
    var user by remember { mutableStateOf<FirebaseUser?>(repository.currentUser()) }
    var authReady by remember { mutableStateOf(false) }

    DisposableEffect(repository) {
        val listener = repository.observeAuthState {
            user = it
            authReady = true
        }
        onDispose { repository.removeAuthStateListener(listener) }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = Purple,
            background = Bg,
            surface = CardBg,
            onPrimary = Bg,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0C1B33), Bg, Color(0xFF130820)),
                    radius = 1500f
                )
            )
        ) {
            when {
                !authReady -> LoadingScreen()
                user == null -> AuthScreen(repository)
                else -> SignedInApp(repository, user!!)
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📚", fontSize = 44.sp)
            Text("EVOLUTION", color = Accent, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("LEARNING", color = Muted, fontSize = 9.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(color = Accent)
        }
    }
}

@Composable
private fun AuthScreen(repository: FirebaseRepository) {
    var createMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("Grade 10") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("📚", fontSize = 52.sp)
            Text("EVOLUTION", color = Accent, fontSize = 31.sp, fontWeight = FontWeight.Black)
            Text("LEARNING v5.2", color = Muted, fontSize = 10.sp, letterSpacing = 4.sp)
            Text("Learn • Practice • Improve • Evolve", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
        }
        item {
            HtmlCard {
                Text(if (createMode) "Create Account" else "Welcome Back", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                if (createMode) {
                    HtmlField(name, { name = it }, "Full name")
                    Spacer(Modifier.height(8.dp))
                    Text("Your Grade", color = Faint, fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Grade 9", "Grade 10", "Grade 11", "Grade 12").forEach { option ->
                            TinyChip(option.removePrefix("Grade "), grade == option) { grade = option }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                HtmlField(email, { email = it }, "Email")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (createMode) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = if (error) Red else Good, fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        message = null
                        error = false
                        val cleanEmail = email.trim()
                        when {
                            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> { message = "Enter a valid email."; error = true }
                            password.length < 6 -> { message = "Password must be at least 6 characters."; error = true }
                            createMode && name.trim().length < 2 -> { message = "Enter your name."; error = true }
                            createMode && password != confirm -> { message = "Passwords do not match."; error = true }
                            else -> {
                                busy = true
                                if (createMode) {
                                    repository.signUp(name, cleanEmail, password, grade) { result ->
                                        busy = false
                                        result.exceptionOrNull()?.let { throwable ->
                                            message = throwable.localizedMessage ?: "Account creation failed."
                                            error = true
                                        }
                                    }
                                } else {
                                    repository.signIn(cleanEmail, password) { result ->
                                        busy = false
                                        result.exceptionOrNull()?.let { throwable ->
                                            message = throwable.localizedMessage ?: "Sign in failed."
                                            error = true
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (createMode) "Create Account" else "Sign In", fontWeight = FontWeight.Bold)
                }
                if (!createMode) {
                    TextButton(onClick = {
                        if (Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            repository.sendPasswordReset(email) { result ->
                                message = if (result.isSuccess) "Password reset email sent." else result.exceptionOrNull()?.localizedMessage
                                error = result.isFailure
                            }
                        } else {
                            message = "Enter your email first."
                            error = true
                        }
                    }) { Text("Forgot password?") }
                }
                TextButton(onClick = { createMode = !createMode; message = null; error = false }) {
                    Text(if (createMode) "Already have an account? Sign in" else "New here? Create an account")
                }
            }
        }
    }
}

@Composable
private fun SignedInApp(repository: FirebaseRepository, user: FirebaseUser) {
    var profile by remember { mutableStateOf(UserProfile(uid = user.uid, email = user.email.orEmpty())) }
    var progress by remember { mutableStateOf(Progress()) }
    var goals by remember { mutableStateOf<List<StudyGoal>>(emptyList()) }
    var homework by remember { mutableStateOf<List<HomeworkItem>>(emptyList()) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf(RootTab.HOME) }

    DisposableEffect(user.uid) {
        val profileListener = repository.listenToProfile(user.uid, { profile = it }, { syncError = it.localizedMessage })
        val progressListener = repository.listenToProgress(user.uid, { progress = it }, { syncError = it.localizedMessage })
        val goalsListener = repository.listenToGoals(user.uid, { goals = it }, { syncError = it.localizedMessage })
        val homeworkListener = repository.listenToHomework(user.uid, { homework = it }, { syncError = it.localizedMessage })
        onDispose {
            profileListener.remove()
            progressListener.remove()
            goalsListener.remove()
            homeworkListener.remove()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { HtmlHeader(profile, progress) },
        bottomBar = { HtmlBottomBar(selected, syncError != null) { selected = it } }
    ) { padding ->
        when (selected) {
            RootTab.HOME -> HomeTab(repository, progress, goals, homework, padding) { selected = RootTab.STUDY }
            RootTab.STUDY -> StudyTab(repository, progress, goals, homework, padding) { selected = RootTab.AI }
            RootTab.AI -> AiTab(profile, progress, padding)
            RootTab.QUIZ -> QuizTab(repository, padding)
            RootTab.STATS -> StatsTab(progress, padding)
            RootTab.ME -> MeTab(repository, profile, progress, padding) { selected = RootTab.AI }
            RootTab.ALERTS -> AlertsTab(progress, syncError, padding)
        }
    }
}

@Composable
private fun HtmlHeader(profile: UserProfile, progress: Progress) {
    Row(
        modifier = Modifier.fillMaxWidth().background(HeaderBg).statusBarsPadding().border(0.5.dp, Accent.copy(alpha = 0.10f)).padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📚", fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("EVOLUTION", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("LEARNING", color = Muted, fontSize = 8.sp, letterSpacing = 2.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MiniStat("🔥 ${progress.streak}", Good)
            MiniStat("${progress.xp} XP", Gold)
            Column(horizontalAlignment = Alignment.End) {
                Text(profile.name.ifBlank { "Student" }, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                Text(profile.grade, fontSize = 8.sp, color = Muted)
            }
        }
    }
}

@Composable
private fun HtmlBottomBar(selected: RootTab, hasAlert: Boolean, onSelect: (RootTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xF7050F1A)).navigationBarsPadding().border(0.5.dp, Accent.copy(alpha = 0.10f)).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RootTab.entries.forEach { tab ->
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selected == tab) Accent.copy(alpha = 0.07f) else Color.Transparent).clickable { onSelect(tab) }.padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Text(tab.icon, fontSize = 16.sp)
                    if (tab == RootTab.ALERTS && hasAlert) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Orange).align(Alignment.TopEnd))
                    }
                }
                Text(tab.label, fontSize = 8.sp, color = if (selected == tab) Accent else Color.White.copy(alpha = 0.28f), fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Normal)
                Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected == tab) Accent else Color.Transparent))
            }
        }
    }
}

@Composable
private fun HomeTab(
    repository: FirebaseRepository,
    progress: Progress,
    goals: List<StudyGoal>,
    homework: List<HomeworkItem>,
    padding: PaddingValues,
    openStudy: () -> Unit
) {
    var mood by remember { mutableStateOf<String?>(null) }
    var checkInMessage by remember { mutableStateOf<String?>(null) }
    var newGoal by remember { mutableStateOf("") }
    var hwSubject by remember { mutableStateOf("") }
    var hwTask by remember { mutableStateOf("") }
    val checkedInToday = progress.lastCheckInDate == LocalDate.now().toString()
    val denominator = goals.size.coerceAtLeast(1).toFloat()
    val goalProgress = goals.count { it.completed }.toFloat() / denominator

    HtmlList(padding) {
        item { SectionHeading("🏠 Home", "Learn, Track, Grow") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Accent.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("DAILY CHECK-IN", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (checkedInToday) "Today's +100 XP has been claimed" else "Claim your +100 XP bonus", color = Muted, fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            repository.recordDailyCheckIn { result ->
                                checkInMessage = when {
                                    result.isFailure -> result.exceptionOrNull()?.localizedMessage
                                    result.getOrNull() == true -> "+100 XP claimed"
                                    else -> "Already claimed today"
                                }
                            }
                        },
                        enabled = !checkedInToday,
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(if (checkedInToday) "Claimed ✓" else "Claim", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        checkInMessage?.let { item { SmallNotice(it, Good) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFeature("💧", "Wellness Tip", "Take a short water and stretch break.", Purple, Modifier.weight(1f))
                SmallFeature("⚡", "Daily Challenge", "Complete one real study goal today.", Gold, Modifier.weight(1f))
            }
        }
        item {
            HtmlCard {
                Text("💭 How are you feeling today?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("😴" to "Tired", "😟" to "Stressed", "😐" to "Okay", "😊" to "Good", "🚀" to "Focused").forEach { pair ->
                        MoodButton(pair.first, pair.second, mood == pair.second, Modifier.weight(1f)) {
                            mood = pair.second
                            repository.setMood(pair.second)
                        }
                    }
                }
            }
        }
        item {
            HtmlCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📈 Daily Progress", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(goalProgress * 100).toInt()}%", color = Accent, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { goalProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                    color = Accent,
                    trackColor = Color.White.copy(alpha = 0.07f)
                )
                Text("Complete real goals to fill the bar", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item {
            HtmlCard {
                Text("🔒 Focus Lock Session", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Start a focused study timer and keep your study flow on task.", color = Faint, fontSize = 11.sp, modifier = Modifier.padding(vertical = 7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("TikTok", "Instagram", "YouTube", "Games").forEach { RedBadge(it) }
                }
                Spacer(Modifier.height(9.dp))
                PrimaryAction("Start Focus Lock", onClick = openStudy)
            }
        }
        item {
            HtmlCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎯 Study Goals", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${goals.count { it.completed }}/${goals.size} done", color = Accent, fontSize = 10.sp)
                }
                Spacer(Modifier.height(8.dp))
                if (goals.isEmpty()) Text("No goals yet.", color = Muted, fontSize = 11.sp)
                goals.take(5).forEach { goal ->
                    ActionRow(goal.title, if (goal.completed) "Completed" else "Active", goal.completed) { repository.completeGoal(goal.id) }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newGoal, onValueChange = { newGoal = it }, modifier = Modifier.weight(1f), label = { Text("Add a new goal") }, singleLine = true)
                    Spacer(Modifier.width(7.dp))
                    Button(onClick = { repository.addGoal(newGoal) { if (it.isSuccess) newGoal = "" } }, enabled = newGoal.isNotBlank()) { Text("Add") }
                }
            }
        }
        item {
            HtmlCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📝 Homework Tracker", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${homework.count { !it.completed }} pending", color = Orange, fontSize = 10.sp)
                }
                Spacer(Modifier.height(8.dp))
                if (homework.isEmpty()) Text("No homework added yet.", color = Muted, fontSize = 11.sp)
                homework.take(5).forEach { item ->
                    ActionRow("${item.subject}: ${item.title}", if (item.completed) "Completed" else "Pending", item.completed) { repository.completeHomework(item.id) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = hwSubject, onValueChange = { hwSubject = it }, modifier = Modifier.weight(0.42f), label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(value = hwTask, onValueChange = { hwTask = it }, modifier = Modifier.weight(1f), label = { Text("Assignment task") }, singleLine = true)
                }
                Spacer(Modifier.height(7.dp))
                PrimaryAction("Add Homework") {
                    repository.addHomework(hwSubject, hwTask) { result ->
                        if (result.isSuccess) { hwSubject = ""; hwTask = "" }
                    }
                }
            }
        }
        item {
            HtmlCard {
                Text("📅 Upcoming Exams", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("No exam dates have been added yet. Sample dates from the old HTML are not used as real data.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.05f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Gold.copy(alpha = 0.13f), RoundedCornerShape(14.dp))) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("“Education is the most powerful weapon which you can use to change the world.”", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, textAlign = TextAlign.Center)
                    Text("— Nelson Mandela", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun StudyTab(
    repository: FirebaseRepository,
    progress: Progress,
    goals: List<StudyGoal>,
    homework: List<HomeworkItem>,
    padding: PaddingValues,
    openAi: () -> Unit
) {
    var focusMinutes by remember { mutableIntStateOf(25) }
    var secondsLeft by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(running, secondsLeft) {
        if (running && secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        } else if (running && secondsLeft == 0) {
            running = false
            repository.recordCompletedFocusSession(focusMinutes * 60L) { result ->
                message = if (result.isSuccess) "Focus session saved to your real progress." else result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    HtmlList(padding) {
        item { SectionHeading("📖 Study Hub", "AI-enhanced learning tools") }
        item {
            HtmlCard {
                Text("🎓 Exam Prep Centre", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Get ready for finals with resources and strategy tools.", color = Faint, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryAction("📄 Past Papers", Modifier.weight(1f)) { message = "Past-paper content packs are not installed yet." }
                    SecondaryAction("💡 Strategy Tips", Modifier.weight(1f)) { message = "Open the AI Tutor for subject-specific exam strategy." }
                }
            }
        }
        message?.let { item { SmallNotice(it, Accent) } }
        item { FlashcardCard(repository) }
        item { FeatureActionCard("🧠", "AI Mind Map", "Turn a topic or your notes into a structured mind map.", "Open Mind Map", openAi) }
        item {
            HtmlCard {
                Text("⏱️ Pomodoro / Focus Lock", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("A completed timer updates your real study time.", color = Faint, fontSize = 11.sp, modifier = Modifier.padding(vertical = 7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(25, 45, 60).forEach { value ->
                        TinyChip("$value min", focusMinutes == value) {
                            if (!running) { focusMinutes = value; secondsLeft = value * 60 }
                        }
                    }
                }
                Text(formatClock(secondsLeft), color = Accent, fontSize = 38.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { running = !running }, modifier = Modifier.weight(1f)) { Text(if (running) "Pause" else "Start") }
                    OutlinedButton(onClick = { running = false; secondsLeft = focusMinutes * 60 }, modifier = Modifier.weight(1f)) { Text("Reset") }
                }
            }
        }
        item {
            HtmlCard {
                Text("🔎 Subject Explorer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Choose a subject for summaries, key facts, and guided help.", color = Faint, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecondaryAction("Mathematics", Modifier.weight(1f), openAi)
                    SecondaryAction("Science", Modifier.weight(1f), openAi)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecondaryAction("History", Modifier.weight(1f), openAi)
                    SecondaryAction("English", Modifier.weight(1f), openAi)
                }
            }
        }
        item { FeatureActionCard("📅", "Personalised Study Planner", "AI builds a plan from subjects, exam dates, weak areas, and available time.", "Create My Plan", openAi) }
        item { FeatureActionCard("📝", "AI Quiz Generator from Notes", "Paste notes and generate a custom practice quiz.", "Generate Quiz from Notes", openAi) }
        item { FeatureActionCard("👩🏽‍🏫", "Live AI Teacher", "Ask for step-by-step explanations, examples, and study help.", "Open Live AI Tutor", openAi) }
        item { FeatureActionCard("📷", "Camera Homework Solver", "Use Smart Scanner to extract a homework question and ask AI.", "Open Smart Scanner", openAi) }
        item { FeatureActionCard("🧠", "AI Weakness Detector", "Analyze real learning totals and find where more practice is needed.", "Analyze Weaknesses", openAi) }
        item {
            HtmlCard {
                Text("🧩 Memory Training Game", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Smart Flashcards are the active memory-training tool in this build.", color = Faint, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                Text("Reviewed: ${progress.flashcardsReviewed}", color = Accent, fontWeight = FontWeight.Bold)
            }
        }
        item {
            HtmlCard {
                Text("🏆 Class Leaderboard", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("No fake classmates are shown. A real leaderboard needs an approved shared Firestore structure and privacy rules.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
        item {
            HtmlCard {
                Text("🧠 AI Mistake Bank", color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${(progress.questionsAnswered - progress.correctAnswers).coerceAtLeast(0)} wrong answers recorded in aggregate. Detailed question history is never invented.", color = Faint, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
        item {
            HtmlCard {
                Text("📝 My Notes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("The note summarizer works now. Persistent note storage can be added without changing this design.", color = Faint, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
        item { SmallNotice("${goals.count { !it.completed }} active goals · ${homework.count { !it.completed }} pending homework", Accent) }
    }
}

@Composable
private fun AiTab(profile: UserProfile, progress: Progress, padding: PaddingValues) {
    var selectedTool by remember { mutableStateOf<AiFeature?>(null) }
    var showVoice by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    HtmlList(padding) {
        item { SectionHeading("🤖 AI Power Centre", "8 AI tools powered securely by Gemini") }
        item { AiCatalogCard("📖", "Smart Dictionary AI", "Reference", "Search a word for meaning, synonyms, and examples.") { selectedTool = AiFeature.DICTIONARY } }
        item { AiCatalogCard("🔊", "AI Voice Reader", "Accessibility", "Listen to notes, summaries, or any text using Android text-to-speech.") { showVoice = true } }
        item { AiCatalogCard("📸", "AI Smart Scanner", "PRO", "Take a photo, extract text with OCR, then send it to AI.") { showScanner = true } }
        item { AiCatalogCard("🤖", "AI Tutor", "Tutor", "Ask school questions and get step-by-step explanations.") { selectedTool = AiFeature.TUTOR } }
        item { AiCatalogCard("🧾", "AI Note Summarizer", "Study", "Turn long notes into a focused study summary.") { selectedTool = AiFeature.SUMMARIZE } }
        item { AiCatalogCard("✏️", "AI Quiz Generator", "Practice", "Generate a custom 5-question quiz from a topic or notes.") { selectedTool = AiFeature.QUIZ } }
        item { AiCatalogCard("📅", "AI Study Planner", "Plan", "Build a realistic schedule from subjects, dates, and available time.") { selectedTool = AiFeature.STUDY_PLAN } }
        item { AiCatalogCard("🧠", "AI Weakness Detector", "Analytics", "Analyze real progress without inventing missing data.") { selectedTool = AiFeature.WEAKNESS } }
        item { SmallNotice("AI requests use Firebase AI Logic. The provider secret is not embedded in this APK.", Good) }
    }

    selectedTool?.let { feature ->
        val prefill = if (feature == AiFeature.WEAKNESS) {
            "Study time: ${formatStudyTime(progress.studySeconds)}; quizzes: ${progress.quizzesCompleted}; answers: ${progress.questionsAnswered}; correct: ${progress.correctAnswers}; flashcards: ${progress.flashcardsReviewed}; focus sessions: ${progress.focusSessionsCompleted}; goals completed: ${progress.goalsCompleted}; homework completed: ${progress.homeworkCompleted}."
        } else ""
        AiToolDialog(feature, profile.grade, prefill) { selectedTool = null }
    }
    if (showVoice) VoiceReaderDialog { showVoice = false }
    if (showScanner) ScannerDialog(profile.grade) { showScanner = false }
}

@Composable
private fun AiToolDialog(feature: AiFeature, grade: String, prefill: String, onClose: () -> Unit) {
    val ai = remember { AiRepository() }
    val scope = rememberCoroutineScope()
    var input by remember(feature) { mutableStateOf(prefill) }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(620.dp).border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF08131F)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(feature.label, color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(feature.hint, color = Muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = onClose) { Text("✕") }
                }
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth().height(150.dp), label = { Text(feature.hint) })
                Spacer(Modifier.height(8.dp))
                PrimaryAction(if (busy) "Thinking…" else "Run ${feature.label}") {
                    if (busy || input.isBlank()) return@PrimaryAction
                    busy = true
                    error = null
                    output = null
                    scope.launch {
                        val result = ai.runFeature(feature, grade, input)
                        busy = false
                        result.onSuccess { output = it }.onFailure { error = friendlyAiError(it) }
                    }
                }
                if (busy) CircularProgressIndicator(color = Accent, modifier = Modifier.padding(top = 10.dp).size(20.dp))
                error?.let { Text(it, color = Orange, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp)) }
                output?.let { response ->
                    Spacer(Modifier.height(10.dp))
                    Text("AI RESPONSE", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 7.dp)) {
                        item { Text(response, color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 18.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceReaderDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf(1.0f) }
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val created = TextToSpeech(context) { }
        engine = created
        onDispose {
            created.stop()
            created.shutdown()
            engine = null
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF08131F)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("AI Voice Reader", color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Convert text to natural device speech.", color = Muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = onClose) { Text("✕") }
                }
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().height(200.dp), label = { Text("Paste text here to read aloud") })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryAction("🔊 Read Aloud", Modifier.weight(2f)) {
                        engine?.language = Locale.getDefault()
                        engine?.setSpeechRate(speed)
                        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "evolution-reader")
                    }
                    SecondaryAction("⏹ Stop", Modifier.weight(1f)) { engine?.stop() }
                }
                Spacer(Modifier.height(10.dp))
                Text("VOICE SPEED: ${"%.1f".format(speed)}x", color = Muted, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.7f, 1.0f, 1.3f, 1.6f).forEach { value -> TinyChip("${value}x", speed == value) { speed = value } }
                }
            }
        }
    }
}

@Composable
private fun ScannerDialog(grade: String, onClose: () -> Unit) {
    val ai = remember { AiRepository() }
    val scope = rememberCoroutineScope()
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var extracted by remember { mutableStateOf("") }
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            busy = true
            error = null
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result -> extracted = result.text; busy = false }
                .addOnFailureListener { throwable -> error = throwable.localizedMessage ?: "OCR failed"; busy = false }
        }
    }

    DisposableEffect(recognizer) {
        onDispose { recognizer.close() }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(650.dp).border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF08131F)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(18.dp).fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Smart Scanner", color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("OCR-VISION · Extract text from a camera photo", color = Muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = onClose) { Text("✕") }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black).border(1.dp, Accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (extracted.isBlank()) "📸\nCamera OCR" else "TEXT EXTRACTED ✓", color = if (extracted.isBlank()) Muted else Good, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(8.dp))
                PrimaryAction("📸 Open Camera") { launcher.launch(null) }
                if (busy) CircularProgressIndicator(color = Accent, modifier = Modifier.padding(top = 8.dp).size(20.dp))
                error?.let { Text(it, color = Orange, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
                if (extracted.isNotBlank()) {
                    Text("EXTRACTED TEXT", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    LazyColumn(Modifier.fillMaxWidth().height(120.dp).padding(top = 5.dp)) { item { Text(extracted, fontSize = 11.sp) } }
                    Spacer(Modifier.height(7.dp))
                    PrimaryAction("Ask AI") {
                        if (busy) return@PrimaryAction
                        busy = true
                        scope.launch {
                            val result = ai.runFeature(AiFeature.TUTOR, grade, extracted)
                            busy = false
                            result.onSuccess { aiAnswer = it }.onFailure { error = friendlyAiError(it) }
                        }
                    }
                }
                aiAnswer?.let { response ->
                    Text("AI RESPONSE", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 5.dp)) { item { Text(response, fontSize = 11.sp, lineHeight = 17.sp) } }
                }
            }
        }
    }
}

@Composable
private fun QuizTab(repository: FirebaseRepository, padding: PaddingValues) {
    HtmlList(padding) {
        item { SectionHeading("✏️ Quiz", "Practice, check, improve") }
        item { QuizCard(repository) }
        item { SmallNotice("Your score is saved only after the quiz is completed. No pre-filled quiz progress is shown.", Good) }
    }
}

private data class QuizQuestion(val question: String, val options: List<String>, val correct: Int)

@Composable
private fun QuizCard(repository: FirebaseRepository) {
    val questions = remember {
        listOf(
            QuizQuestion("Solve: 3x - 9 = 0", listOf("x = 1", "x = 2", "x = 3", "x = 6"), 2),
            QuizQuestion("Which process lets green plants make food?", listOf("Respiration", "Photosynthesis", "Diffusion", "Transpiration"), 1),
            QuizQuestion("What is 25% of 80?", listOf("10", "20", "25", "40"), 1)
        )
    }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var choice by remember { mutableStateOf<Int?>(null) }
    var finished by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    HtmlCard {
        Text("Quick Quiz", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (!finished) {
            val question = questions[index]
            Text("Question ${index + 1}/${questions.size}", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
            Text(question.question, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            question.options.forEachIndexed { optionIndex, label -> ChoiceRow(label, choice == optionIndex) { choice = optionIndex } }
            Spacer(Modifier.height(8.dp))
            PrimaryAction(if (index == questions.lastIndex) "Finish Quiz" else "Next") {
                val chosen = choice ?: return@PrimaryAction
                val nextScore = score + if (chosen == question.correct) 1 else 0
                score = nextScore
                if (index == questions.lastIndex) {
                    finished = true
                    repository.recordQuizResult(nextScore, questions.size) { saved = it.isSuccess }
                } else {
                    index++
                    choice = null
                }
            }
        } else {
            Text("$score / ${questions.size}", color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp))
            Text(if (saved) "Saved to Firebase progress." else "Saving result…", color = if (saved) Good else Muted, fontSize = 10.sp)
            TextButton(onClick = { index = 0; score = 0; choice = null; finished = false; saved = false }) { Text("Try Again") }
        }
    }
}

@Composable
private fun StatsTab(progress: Progress, padding: PaddingValues) {
    HtmlList(padding) {
        item { SectionHeading("📊 Progress Dashboard", "Your full learning analytics") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StatTile("⏱️", formatStudyTime(progress.studySeconds), "Study Hrs", Accent, Modifier.weight(1f))
                StatTile("✏️", progress.quizzesCompleted.toString(), "Quizzes", Accent, Modifier.weight(1f))
                StatTile("⚡", progress.flashcardsReviewed.toString(), "Flashcards", Purple, Modifier.weight(1f))
            }
        }
        item {
            HtmlCard {
                Text("Learning Analytics", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                AnalyticsRow("Quiz accuracy", progress.accuracyPercent?.let { "$it%" } ?: "—", Accent)
                AnalyticsRow("Focus sessions", progress.focusSessionsCompleted.toString(), Good)
                AnalyticsRow("Goals completed", progress.goalsCompleted.toString(), Blue)
                AnalyticsRow("Homework completed", progress.homeworkCompleted.toString(), Purple)
                AnalyticsRow("XP", progress.xp.toString(), Gold)
            }
        }
        item {
            HtmlCard {
                Text("⏱️ App Usage Tracker", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Usage totals are not fabricated. Android usage access can be added later with explicit permission.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
    }
}

@Composable
private fun MeTab(repository: FirebaseRepository, profile: UserProfile, progress: Progress, padding: PaddingValues, openAi: () -> Unit) {
    var resetMessage by remember { mutableStateOf<String?>(null) }
    val levelProgress = progress.xpIntoLevel.toFloat() / 1000f
    val earnedBadges = listOf(
        progress.focusSessionsCompleted >= 1,
        progress.quizzesCompleted >= 1,
        progress.flashcardsReviewed >= 10
    ).count { it }

    HtmlList(padding) {
        item { SectionHeading("👤 My Profile", "") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Accent.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            ) {
                Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👤", fontSize = 48.sp)
                    Text(profile.name.ifBlank { "New Student" }, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(profile.grade, color = Accent, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(listOf(Gold, Orange))).padding(horizontal = 10.dp, vertical = 3.dp)) {
                            Text("LEVEL ${progress.level}", color = Bg, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Text("${progress.xpIntoLevel} / 1000 XP", color = Muted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                        color = Gold,
                        trackColor = Color.White.copy(alpha = 0.05f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfileMini("🔥${progress.streak}", "Streak")
                        ProfileMini(progress.xp.toString(), "Points")
                        ProfileMini("🎖️$earnedBadges", "Badges")
                        ProfileMini("L${progress.level}", "Level")
                    }
                }
            }
        }
        item { FeatureActionCard("🤖", "AI Study Coach", "Get personalized study tips and motivation from your AI coach.", "Open Study Coach", openAi) }
        item {
            HtmlCard {
                Text("⚙️ App Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Your Grade", color = Muted, fontSize = 11.sp)
                    Text(profile.grade, fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        repository.sendPasswordReset(profile.email) { result ->
                            resetMessage = if (result.isSuccess) "Password reset email sent." else result.exceptionOrNull()?.localizedMessage
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Send Password Reset") }
                Spacer(Modifier.height(7.dp))
                Button(onClick = repository::signOut, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))) { Text("Sign Out") }
                resetMessage?.let { Text(it, color = Good, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        item { SmallNotice("Firebase Auth + Cloud Firestore + Firebase AI Logic are active. Release App Check uses Play Integrity after the signing fingerprint is registered.", Accent) }
    }
}

@Composable
private fun AlertsTab(progress: Progress, syncError: String?, padding: PaddingValues) {
    HtmlList(padding) {
        item { SectionHeading("🔔 Alerts", "Notifications and sync status") }
        if (syncError != null) item { SmallNotice("Sync issue: $syncError", Orange) }
        else item { SmallNotice("Firebase sync is connected.", Good) }
        item {
            HtmlCard {
                Text("Daily Check-in", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(if (progress.lastCheckInDate == LocalDate.now().toString()) "Today's reward has been claimed." else "Today's reward is still available on Home.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item {
            HtmlCard {
                Text("No fake alerts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("The HTML sample notification count is not carried over. This tab shows only real state.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun FlashcardCard(repository: FirebaseRepository) {
    val cards = remember {
        listOf(
            "What is 3x - 9 = 0?" to "x = 3",
            "What is the formula for force?" to "F = ma",
            "What is 12 × 8?" to "96"
        )
    }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var counted by remember { mutableStateOf(setOf<Int>()) }

    HtmlCard {
        Text("⚡ Smart Flashcards", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(14.dp)).background(Accent.copy(alpha = 0.07f)).border(1.dp, Accent.copy(alpha = 0.16f), RoundedCornerShape(14.dp)).clickable {
                revealed = !revealed
                if (revealed && index !in counted) {
                    counted = counted + index
                    repository.recordFlashcardReviewed()
                }
            }.padding(vertical = 24.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (revealed) "Answer" else "Tap to reveal", color = Muted, fontSize = 9.sp)
                Text(if (revealed) cards[index].second else cards[index].first, textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Text("Box ${index + 1}/${cards.size}", color = Color.White.copy(alpha = 0.22f), fontSize = 9.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryAction("Previous", Modifier.weight(1f)) { index = (index - 1 + cards.size) % cards.size; revealed = false }
            SecondaryAction("Next", Modifier.weight(1f)) { index = (index + 1) % cards.size; revealed = false }
        }
    }
}

@Composable
private fun AiCatalogCard(icon: String, title: String, badge: String, body: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Accent.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha = 0.12f)).border(1.dp, Accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 18.sp) }
                    Spacer(Modifier.width(9.dp))
                    Text(title, color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Accent.copy(alpha = 0.13f)).border(1.dp, Accent.copy(alpha = 0.26f), RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(badge, color = Accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(body, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 9.dp))
            PrimaryAction("$icon Open →", onClick = onClick)
        }
    }
}

@Composable
private fun FeatureActionCard(icon: String, title: String, body: String, action: String, onClick: () -> Unit) {
    HtmlCard {
        Text("$icon $title", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = Faint, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
        PrimaryAction(action, onClick = onClick)
    }
}

@Composable
private fun HtmlList(padding: PaddingValues, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(title, color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, color = Color.White.copy(alpha = 0.38f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun HtmlCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun SmallFeature(icon: String, title: String, body: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.border(1.dp, accent.copy(alpha = 0.16f), RoundedCornerShape(14.dp)), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.06f)), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(11.dp)) {
            Text(icon, fontSize = 18.sp)
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(body, color = Color.White.copy(alpha = 0.68f), fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun MoodButton(emoji: String, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) Accent.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.03f)).border(if (selected) 1.5.dp else 1.dp, if (selected) Accent else Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 7.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RedBadge(text: String) {
    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(Red.copy(alpha = 0.11f)).border(1.dp, Red.copy(alpha = 0.18f), RoundedCornerShape(7.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
        Text(text, color = Red, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TinyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(if (selected) Accent else Accent.copy(alpha = 0.07f)).border(1.dp, Accent.copy(alpha = 0.21f), RoundedCornerShape(9.dp)).clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 7.dp)) {
        Text(label, color = if (selected) Bg else Accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrimaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 9.dp)
    ) { Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SecondaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(vertical = 9.dp)) {
        Text(text, color = Accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, done: Boolean, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.04f)).padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(if (done) Good else Orange))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 8.sp, color = if (done) Good else Muted)
        }
        if (!done) TextButton(onClick = onDone, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) { Text("Done", fontSize = 9.sp) }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp)).background(if (selected) Accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.035f)).border(1.dp, if (selected) Accent else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(10.dp)) {
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun SmallNotice(text: String, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.06f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp))) {
        Text(text, color = Color.White.copy(alpha = 0.68f), fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(11.dp))
    }
}

@Composable
private fun StatTile(icon: String, value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.border(1.dp, Accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp)), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 17.sp)
            Text(value, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun AnalyticsRow(label: String, value: String, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, fontSize = 10.sp)
        Text(value, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileMini(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, fontSize = 8.sp)
    }
}

@Composable
private fun HtmlField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
}

private fun friendlyAiError(error: Throwable): String {
    val message = error.localizedMessage.orEmpty()
    return when {
        message.contains("app check", ignoreCase = true) || message.contains("403") -> "AI is connected, but Firebase App Check is blocking this debug build until its debug token is registered."
        message.contains("network", ignoreCase = true) || message.contains("unavailable", ignoreCase = true) -> "The AI service could not be reached. Check the internet connection and try again."
        else -> message.ifBlank { "The AI request failed. Please try again." }
    }
}

private fun formatClock(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

private fun formatStudyTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
