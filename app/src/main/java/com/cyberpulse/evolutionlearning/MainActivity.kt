package com.cyberpulse.evolutionlearning

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpulse.evolutionlearning.data.FirebaseRepository
import com.cyberpulse.evolutionlearning.model.HomeworkItem
import com.cyberpulse.evolutionlearning.model.Progress
import com.cyberpulse.evolutionlearning.model.StudyGoal
import com.cyberpulse.evolutionlearning.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay

private val Cyan = Color(0xFF38BDF8)
private val Blue = Color(0xFF2563EB)
private val Purple = Color(0xFFC084FC)
private val Bg = Color(0xFF050B14)
private val CardBg = Color(0xCC0C1726)
private val Muted = Color(0xFF93A4BC)
private val Good = Color(0xFF4ADE80)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EvolutionLearningApp() }
    }
}

private enum class RootTab(val label: String, val glyph: String) {
    HOME("Home", "⌂"),
    STUDY("Study", "▣"),
    PROGRESS("Progress", "↗"),
    PROFILE("Me", "◉")
}

@Composable
private fun EvolutionLearningApp() {
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
            primary = Cyan,
            secondary = Purple,
            background = Bg,
            surface = CardBg,
            onPrimary = Color(0xFF001018),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color(0xFF102A42), Bg, Color(0xFF130A22)),
                    radius = 1400f
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
            Text("EVOLUTION", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Cyan)
            Text("LEARNING", fontSize = 12.sp, letterSpacing = 5.sp, color = Muted)
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(color = Cyan)
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
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("📚", fontSize = 48.sp)
            Text("EVOLUTION", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Cyan)
            Text("LEARNING v5", fontSize = 11.sp, letterSpacing = 4.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            Text("Learn → Practice → Improve → Evolve", color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
        }
        item {
            GlassCard {
                Text(if (createMode) "Create your account" else "Welcome back", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (createMode) "Your real progress will sync securely with Firebase." else "Sign in with your Evolution Learning account.",
                    color = Muted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                if (createMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full name") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Grade", fontSize = 12.sp, color = Muted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Grade 9", "Grade 10", "Grade 11", "Grade 12").forEach { option ->
                            SmallChip(option.removePrefix("Grade "), grade == option) { grade = option }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true
                )
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
                if (message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(message!!, color = if (error) Color(0xFFFCA5A5) else Good, fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        message = null
                        error = false
                        val cleanEmail = email.trim()
                        when {
                            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                                message = "Enter a valid email address."
                                error = true
                            }
                            password.length < 6 -> {
                                message = "Password must be at least 6 characters."
                                error = true
                            }
                            createMode && name.trim().length < 2 -> {
                                message = "Enter your name."
                                error = true
                            }
                            createMode && password != confirm -> {
                                message = "Passwords do not match."
                                error = true
                            }
                            else -> {
                                busy = true
                                if (createMode) {
                                    repository.signUp(name, cleanEmail, password, grade) { result ->
                                        busy = false
                                        result.exceptionOrNull()?.let {
                                            message = it.localizedMessage ?: "Account creation failed."
                                            error = true
                                        }
                                    }
                                } else {
                                    repository.signIn(cleanEmail, password) { result ->
                                        busy = false
                                        result.exceptionOrNull()?.let {
                                            message = it.localizedMessage ?: "Sign in failed."
                                            error = true
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF001018))
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (createMode) "Create account" else "Sign in", fontWeight = FontWeight.Bold)
                }
                if (!createMode) {
                    TextButton(onClick = {
                        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            message = "Enter your email first, then tap Forgot password."
                            error = true
                        } else {
                            repository.sendPasswordReset(email) { result ->
                                message = if (result.isSuccess) "Password reset email sent." else result.exceptionOrNull()?.localizedMessage
                                error = result.isFailure
                            }
                        }
                    }) { Text("Forgot password?") }
                }
                TextButton(onClick = {
                    createMode = !createMode
                    message = null
                    error = false
                }) {
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
        val p = repository.listenToProfile(user.uid, { profile = it }, { syncError = it.localizedMessage })
        val r = repository.listenToProgress(user.uid, { progress = it }, { syncError = it.localizedMessage })
        val g = repository.listenToGoals(user.uid, { goals = it }, { syncError = it.localizedMessage })
        val h = repository.listenToHomework(user.uid, { homework = it }, { syncError = it.localizedMessage })
        onDispose {
            p.remove(); r.remove(); g.remove(); h.remove()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = Color(0xF20A111C), modifier = Modifier.navigationBarsPadding()) {
                RootTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        icon = { Text(tab.glyph, fontSize = 19.sp) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Cyan,
                            selectedTextColor = Cyan,
                            indicatorColor = Cyan.copy(alpha = 0.12f),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        when (selected) {
            RootTab.HOME -> HomeScreen(profile, progress, goals, homework, syncError, padding) { selected = RootTab.STUDY }
            RootTab.STUDY -> StudyScreen(repository, goals, homework, padding)
            RootTab.PROGRESS -> ProgressScreen(progress, padding)
            RootTab.PROFILE -> ProfileScreen(repository, profile, padding)
        }
    }
}

@Composable
private fun HomeScreen(
    profile: UserProfile,
    progress: Progress,
    goals: List<StudyGoal>,
    homework: List<HomeworkItem>,
    syncError: String?,
    padding: PaddingValues,
    openStudy: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header(profile) }
        item {
            GlassCard {
                Text("YOUR LEARNING COMMAND CENTER", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("Learn smarter. Track what you actually do.", fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("No fake progress — every number below comes from completed activity saved to your account.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Button(onClick = openStudy, colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF001018))) {
                    Text("Start studying", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (syncError != null) {
            item { NoticeCard("Sync issue", syncError, Color(0xFFF59E0B)) }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Study time", formatStudyTime(progress.studySeconds), "completed focus", Cyan, Modifier.weight(1f))
                MetricCard("Quiz accuracy", progress.accuracyPercent?.let { "$it%" } ?: "—", "real attempts", Purple, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Focus", progress.focusSessionsCompleted.toString(), "sessions", Good, Modifier.weight(1f))
                MetricCard("Flashcards", progress.flashcardsReviewed.toString(), "reviewed", Color(0xFFFBBF24), Modifier.weight(1f))
            }
        }
        item { SectionTitle("Today", "Your active work") }
        item {
            GlassCard {
                Text("🎯 Study goals", fontWeight = FontWeight.Bold)
                Text("${goals.count { it.completed }} completed · ${goals.count { !it.completed }} active", color = Muted)
                Spacer(Modifier.height(10.dp))
                goals.filter { !it.completed }.take(3).forEach { Text("• ${it.title}", modifier = Modifier.padding(vertical = 3.dp)) }
                if (goals.none { !it.completed }) Text("No active goals yet. Add one in Study.", color = Muted)
            }
        }
        item {
            GlassCard {
                Text("📝 Homework", fontWeight = FontWeight.Bold)
                Text("${homework.count { it.completed }} completed · ${homework.count { !it.completed }} pending", color = Muted)
                Spacer(Modifier.height(10.dp))
                homework.filter { !it.completed }.take(3).forEach { Text("• ${it.subject}: ${it.title}", modifier = Modifier.padding(vertical = 3.dp)) }
                if (homework.none { !it.completed }) Text("No pending homework yet.", color = Muted)
            }
        }
    }
}

@Composable
private fun StudyScreen(repository: FirebaseRepository, goals: List<StudyGoal>, homework: List<HomeworkItem>, padding: PaddingValues) {
    var focusMinutes by remember { mutableIntStateOf(25) }
    var secondsLeft by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var focusMessage by remember { mutableStateOf<String?>(null) }
    var goalText by remember { mutableStateOf("") }
    var hwSubject by remember { mutableStateOf("") }
    var hwTitle by remember { mutableStateOf("") }

    LaunchedEffect(running, secondsLeft) {
        if (running && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        } else if (running && secondsLeft == 0) {
            running = false
            repository.recordCompletedFocusSession(focusMinutes * 60L) { result ->
                focusMessage = if (result.isSuccess) "Focus session saved to your real progress." else result.exceptionOrNull()?.localizedMessage
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SimpleHeader("📖", "Study Hub", "Practice tools that update real progress") }
        item {
            GlassCard {
                Text("🔒 Focus session", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Only a fully completed timer is added to your study time.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(25, 45, 60).forEach { value ->
                        SmallChip("$value min", focusMinutes == value) {
                            if (!running) {
                                focusMinutes = value
                                secondsLeft = value * 60
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(formatClock(secondsLeft), fontSize = 44.sp, fontWeight = FontWeight.Black, color = Cyan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { running = !running }, colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF001018))) {
                        Text(if (running) "Pause" else "Start")
                    }
                    OutlinedButton(onClick = { running = false; secondsLeft = focusMinutes * 60 }) { Text("Reset") }
                }
                if (focusMessage != null) Text(focusMessage!!, color = Good, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item { QuizCard(repository) }
        item { FlashcardCard(repository) }
        item {
            GlassCard {
                Text("🎯 Goals", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = goalText, onValueChange = { goalText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("New study goal") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    repository.addGoal(goalText) { if (it.isSuccess) goalText = "" }
                }, enabled = goalText.isNotBlank()) { Text("Add goal") }
                Spacer(Modifier.height(8.dp))
                goals.take(6).forEach { goal ->
                    ActionRow(
                        title = goal.title,
                        subtitle = if (goal.completed) "Completed" else "Active",
                        done = goal.completed,
                        action = if (goal.completed) null else { { repository.completeGoal(goal.id) } }
                    )
                }
            }
        }
        item {
            GlassCard {
                Text("📝 Homework tracker", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = hwSubject, onValueChange = { hwSubject = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Subject") }, singleLine = true)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(value = hwTitle, onValueChange = { hwTitle = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Task") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    repository.addHomework(hwSubject, hwTitle) {
                        if (it.isSuccess) { hwSubject = ""; hwTitle = "" }
                    }
                }, enabled = hwSubject.isNotBlank() && hwTitle.isNotBlank()) { Text("Add homework") }
                Spacer(Modifier.height(8.dp))
                homework.take(6).forEach { item ->
                    ActionRow(
                        title = "${item.subject}: ${item.title}",
                        subtitle = if (item.completed) "Completed" else "Pending",
                        done = item.completed,
                        action = if (item.completed) null else { { repository.completeHomework(item.id) } }
                    )
                }
            }
        }
        item {
            NoticeCard(
                "🤖 AI Tutor",
                "The interface is reserved, but AI is not faked. A secure backend/model connection still needs to be configured before AI answers are enabled.",
                Purple
            )
        }
    }
}

private data class QuizQuestion(val q: String, val options: List<String>, val correct: Int)

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
    var selected by remember { mutableStateOf<Int?>(null) }
    var finished by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    GlassCard {
        Text("✏️ Quick quiz", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Your score is recorded only when the quiz is completed.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        if (!finished) {
            val question = questions[index]
            Text("Question ${index + 1}/${questions.size}", color = Cyan, fontSize = 11.sp)
            Text(question.q, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            question.options.forEachIndexed { optionIndex, label ->
                ChoiceRow(label, selected == optionIndex) { selected = optionIndex }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val chosen = selected ?: return@Button
                    val wasCorrect = chosen == question.correct
                    val finalScore = score + if (wasCorrect) 1 else 0
                    score = finalScore
                    if (index == questions.lastIndex) {
                        finished = true
                        repository.recordQuizResult(finalScore, questions.size) { saved = it.isSuccess }
                    } else {
                        index += 1
                        selected = null
                    }
                },
                enabled = selected != null
            ) { Text(if (index == questions.lastIndex) "Finish quiz" else "Next") }
        } else {
            Text("Score: $score / ${questions.size}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Cyan)
            Text(if (saved) "Saved to Firebase progress." else "Saving result…", color = if (saved) Good else Muted)
            TextButton(onClick = { index = 0; score = 0; selected = null; finished = false; saved = false }) { Text("Try again") }
        }
    }
}

@Composable
private fun FlashcardCard(repository: FirebaseRepository) {
    val cards = remember {
        listOf(
            "What is the formula for force?" to "F = ma",
            "What is the capital of South Africa's Limpopo province?" to "Polokwane",
            "What is 12 × 8?" to "96"
        )
    }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var counted by remember { mutableStateOf(setOf<Int>()) }

    GlassCard {
        Text("⚡ Smart flashcards", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Card ${index + 1}/${cards.size}", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cyan.copy(alpha = 0.07f)).clickable {
                revealed = !revealed
                if (revealed && index !in counted) {
                    counted = counted + index
                    repository.recordFlashcardReviewed()
                }
            }.padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(if (revealed) cards[index].second else cards[index].first, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { index = (index - 1 + cards.size) % cards.size; revealed = false }) { Text("Prev") }
            Button(onClick = { index = (index + 1) % cards.size; revealed = false }) { Text("Next") }
        }
    }
}

@Composable
private fun ProgressScreen(progress: Progress, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SimpleHeader("📊", "Real Progress", "Nothing is pre-filled or simulated") }
        item {
            GlassCard {
                Text("Learning record", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(formatStudyTime(progress.studySeconds), fontSize = 38.sp, fontWeight = FontWeight.Black)
                Text("Total completed focus time", color = Muted)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Quizzes", progress.quizzesCompleted.toString(), "completed", Cyan, Modifier.weight(1f))
                MetricCard("Accuracy", progress.accuracyPercent?.let { "$it%" } ?: "—", "${progress.questionsAnswered} answers", Purple, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Focus", progress.focusSessionsCompleted.toString(), "sessions", Good, Modifier.weight(1f))
                MetricCard("Flashcards", progress.flashcardsReviewed.toString(), "reviewed", Color(0xFFFBBF24), Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Goals", progress.goalsCompleted.toString(), "completed", Blue, Modifier.weight(1f))
                MetricCard("Homework", progress.homeworkCompleted.toString(), "completed", Purple, Modifier.weight(1f))
            }
        }
        item { NoticeCard("How progress works", "The app starts from zero. Firebase updates these totals only after a real quiz, completed focus timer, reviewed flashcard, completed goal, or completed homework item.", Cyan) }
    }
}

@Composable
private fun ProfileScreen(repository: FirebaseRepository, profile: UserProfile, padding: PaddingValues) {
    var resetMessage by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SimpleHeader("👤", "Profile", "Firebase account") }
        item {
            GlassCard {
                Box(Modifier.size(62.dp).clip(CircleShape).background(Cyan.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text(profile.name.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Cyan)
                }
                Spacer(Modifier.height(12.dp))
                Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(profile.grade, color = Cyan)
                Text(profile.email, color = Muted)
            }
        }
        item {
            GlassCard {
                Text("Account security", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Passwords are handled by Firebase Authentication, not stored inside the app.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = {
                    repository.sendPasswordReset(profile.email) {
                        resetMessage = if (it.isSuccess) "Password reset email sent." else it.exceptionOrNull()?.localizedMessage
                    }
                }) { Text("Send password reset") }
                if (resetMessage != null) Text(resetMessage!!, color = Good, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = repository::signOut, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))) { Text("Sign out") }
            }
        }
        item { NoticeCard("Firebase", "Authentication: Email/Password · Database: Cloud Firestore · App Check: Play Integrity will be added after the signing SHA-256 is registered.", Cyan) }
    }
}

@Composable
private fun Header(profile: UserProfile) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("EVOLUTION", color = Cyan, fontWeight = FontWeight.Black, fontSize = 19.sp)
            Text("LEARNING v5", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(profile.name.ifBlank { "Student" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(profile.grade, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SimpleHeader(icon: String, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 26.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Black, color = Cyan)
            Text(subtitle, fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = 0.10f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun MetricCard(label: String, value: String, sub: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.border(1.dp, accent.copy(alpha = 0.14f), RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = Muted, fontSize = 11.sp)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = accent)
            Text(sub, color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun NoticeCard(title: String, body: String, accent: Color) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.07f)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = accent)
            Text(body, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun SmallChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) Cyan else Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) Color(0xFF001018) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) Cyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.035f))
            .border(1.dp, if (selected) Cyan else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(11.dp)
    ) { Text(label) }
}

@Composable
private fun ActionRow(title: String, subtitle: String, done: Boolean, action: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (done) Good else Cyan))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 10.sp, color = if (done) Good else Muted)
        }
        if (action != null) TextButton(onClick = action) { Text("Done") }
    }
}

private fun formatClock(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

private fun formatStudyTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
