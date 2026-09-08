package com.cyberpulse.evolutionlearning

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
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
import java.util.Locale

private val ModernBg = Color(0xFF040914)
private val ModernBg2 = Color(0xFF07101D)
private val ModernCard = Color(0xD90C1727)
private val ModernCard2 = Color(0xD9081220)
private val ModernAccent = Color(0xFF67E8F9)
private val ModernBlue = Color(0xFF38BDF8)
private val ModernPurple = Color(0xFF8B5CF6)
private val ModernGreen = Color(0xFF34D399)
private val ModernGood = Color(0xFF4ADE80)
private val ModernGold = Color(0xFFFBBF24)
private val ModernOrange = Color(0xFFFB923C)
private val ModernRed = Color(0xFFF87171)
private val ModernText = Color(0xFFF8FBFF)
private val ModernSub = Color(0x8FE2E8F0)
private val ModernFaint = Color(0x70E2E8F0)
private val ModernBorder = Color(0x1C94A3B8)

private enum class ModernTab(val label: String, val icon: String) {
    HOME("Home", "🏠"), STUDY("Study", "📖"), AI("AI", "🤖"), QUIZ("Quiz", "✏️"), STATS("Stats", "📊"), ME("Me", "👤")
}

private enum class EntryStage { SPLASH, AUTH, APP }

@Composable
fun EvolutionLearningModernRoot() {
    val repository = remember { FirebaseRepository() }
    var user by remember { mutableStateOf<FirebaseUser?>(repository.currentUser()) }
    var authReady by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf(EntryStage.SPLASH) }

    DisposableEffect(repository) {
        val listener = repository.observeAuthState {
            user = it
            authReady = true
            if (stage == EntryStage.APP && it == null) stage = EntryStage.AUTH
        }
        onDispose { repository.removeAuthStateListener(listener) }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ModernAccent,
            secondary = ModernPurple,
            background = ModernBg,
            surface = ModernCard,
            onPrimary = Color(0xFF05111D),
            onBackground = ModernText,
            onSurface = ModernText
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF07101D), ModernBg, Color(0xFF07111C)))
            )
        ) {
            when {
                !authReady -> ModernLoading()
                stage == EntryStage.SPLASH -> ModernSplash {
                    stage = if (user == null) EntryStage.AUTH else EntryStage.APP
                }
                stage == EntryStage.AUTH || user == null -> ModernAuth(repository) {
                    stage = EntryStage.APP
                }
                else -> ModernSignedIn(repository, user!!)
            }
        }
    }
}

@Composable
private fun ModernLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📚", fontSize = 42.sp)
            Spacer(Modifier.height(10.dp))
            CircularProgressIndicator(color = ModernAccent)
        }
    }
}

@Composable
private fun ModernSplash(onStart: () -> Unit) {
    Box(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, ModernBorder, RoundedCornerShape(30.dp)),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xC20F1C2F))
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(82.dp).clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(ModernAccent.copy(alpha = .12f), ModernPurple.copy(alpha = .14f))))
                        .border(1.dp, ModernAccent.copy(alpha = .18f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("📚", fontSize = 40.sp) }
                Spacer(Modifier.height(18.dp))
                Text("EVOLUTION LEARNING", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ModernAccent, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("YOUR FUTURE, BUILT ONE SESSION AT A TIME", fontSize = 11.sp, letterSpacing = 3.sp, color = ModernSub, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("v5.0 · Modern · Focused · Built for real learning", color = ModernSub, fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                GradientButton("Begin Your Journey →", onStart)
            }
        }
    }
}

@Composable
private fun ModernAuth(repository: FirebaseRepository, onAuthenticated: () -> Unit) {
    var signUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("Grade 10") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val grades = remember { (1..12).map { "Grade $it" } + listOf("University", "College") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, ModernBorder, RoundedCornerShape(30.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xC20F1C2F)),
                shape = RoundedCornerShape(30.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(82.dp).clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(ModernAccent.copy(alpha = .12f), ModernPurple.copy(alpha = .14f)))),
                        contentAlignment = Alignment.Center
                    ) { Text("👩‍🎓", fontSize = 38.sp) }
                    Spacer(Modifier.height(16.dp))
                    Text("Sign in to Evolution", fontSize = 25.sp, fontWeight = FontWeight.Black, color = ModernText)
                    Text("Protect your progress, focus sessions, and AI study history.", color = ModernSub, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton("Sign In", { signUp = false; message = null }, Modifier.weight(1f))
                        Box(
                            Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(15.dp))
                                .background(if (signUp) ModernOrange else Color.White.copy(alpha = .07f))
                                .clickable { signUp = true; message = null },
                            contentAlignment = Alignment.Center
                        ) { Text("Sign Up", fontWeight = FontWeight.Bold, color = if (signUp) ModernBg else ModernText) }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(if (signUp) "Create Account" else "Welcome Back", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(10.dp))
                    if (signUp) {
                        ModernField(name, { name = it }, "Full name")
                        Spacer(Modifier.height(8.dp))
                    }
                    ModernField(email, { email = it }, "Email address")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (signUp) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Select grade", color = ModernSub, fontSize = 10.sp, modifier = Modifier.align(Alignment.Start))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            grades.forEach { option -> ChoiceChip(option, grade == option) { grade = option } }
                        }
                    }
                    message?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = if (isError) ModernRed else ModernGood, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    GradientButton(if (busy) "Please wait…" else if (signUp) "Create Account" else "Sign In", {
                        if (busy) return@GradientButton
                        val clean = email.trim()
                        isError = false
                        message = null
                        when {
                            !Patterns.EMAIL_ADDRESS.matcher(clean).matches() -> { message = "Enter a valid email address."; isError = true }
                            password.length < 6 -> { message = "Password must be at least 6 characters."; isError = true }
                            signUp && name.trim().length < 2 -> { message = "Enter your full name."; isError = true }
                            signUp && password != confirm -> { message = "Passwords do not match."; isError = true }
                            else -> {
                                busy = true
                                if (signUp) repository.signUp(name, clean, password, grade) { result ->
                                    busy = false
                                    if (result.isSuccess) onAuthenticated() else { message = result.exceptionOrNull()?.localizedMessage ?: "Account creation failed."; isError = true }
                                } else repository.signIn(clean, password) { result ->
                                    busy = false
                                    if (result.isSuccess) onAuthenticated() else { message = result.exceptionOrNull()?.localizedMessage ?: "Sign in failed."; isError = true }
                                }
                            }
                        }
                    })
                    if (!signUp) {
                        TextButton(onClick = {
                            if (Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                repository.sendPasswordReset(email) { r ->
                                    message = if (r.isSuccess) "Password reset email sent." else r.exceptionOrNull()?.localizedMessage
                                    isError = r.isFailure
                                }
                            } else { message = "Enter your email first."; isError = true }
                        }) { Text("Forgot password?", color = ModernAccent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSignedIn(repository: FirebaseRepository, user: FirebaseUser) {
    var profile by remember { mutableStateOf(UserProfile(uid = user.uid, email = user.email.orEmpty())) }
    var progress by remember { mutableStateOf(Progress()) }
    var goals by remember { mutableStateOf<List<StudyGoal>>(emptyList()) }
    var homework by remember { mutableStateOf<List<HomeworkItem>>(emptyList()) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(ModernTab.HOME) }
    var alerts by remember { mutableStateOf(false) }
    var aiTool by remember { mutableStateOf<AiFeature?>(null) }
    var aiTitle by remember { mutableStateOf<String?>(null) }
    var aiPrefill by remember { mutableStateOf("") }
    var showVoice by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    DisposableEffect(user.uid) {
        val p = repository.listenToProfile(user.uid, { profile = it }, { syncError = it.localizedMessage })
        val r = repository.listenToProgress(user.uid, { progress = it }, { syncError = it.localizedMessage })
        val g = repository.listenToGoals(user.uid, { goals = it }, { syncError = it.localizedMessage })
        val h = repository.listenToHomework(user.uid, { homework = it }, { syncError = it.localizedMessage })
        onDispose { p.remove(); r.remove(); g.remove(); h.remove() }
    }

    val openAi: (AiFeature, String?, String) -> Unit = { feature, title, prefill ->
        aiTool = feature; aiTitle = title; aiPrefill = prefill
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { ModernHeader(profile, progress) },
        bottomBar = { ModernBottomBar(tab, { tab = it }, { alerts = true }) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                ModernTab.HOME -> ModernHome(repository, progress, goals, homework, padding) {
                    LocalContext.current.startActivity(Intent(LocalContext.current, FocusLockActivity::class.java))
                }
                ModernTab.STUDY -> ModernStudy(repository, progress, padding, openAi) {
                    LocalContext.current.startActivity(Intent(LocalContext.current, FocusLockActivity::class.java))
                }
                ModernTab.AI -> ModernAiHub(profile, progress, padding, openAi, { showVoice = true }, { showScanner = true })
                ModernTab.QUIZ -> ModernQuiz(repository, padding, openAi)
                ModernTab.STATS -> ModernStats(progress, goals, padding)
                ModernTab.ME -> ModernProfile(repository, profile, progress, padding, openAi)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp).size(56.dp)
                    .clip(CircleShape).background(Brush.linearGradient(listOf(ModernAccent, ModernBlue, ModernPurple)))
                    .clickable { openAi(AiFeature.TUTOR, "AI Tutor", "") },
                contentAlignment = Alignment.Center
            ) { Text("🤖", fontSize = 24.sp) }
        }
    }

    if (alerts) ModernAlertsDialog(progress, syncError) { alerts = false }
    aiTool?.let { feature ->
        ModernAiDialog(feature, aiTitle ?: feature.label, profile.grade, aiPrefill) { aiTool = null }
    }
    if (showVoice) ModernVoiceReader { showVoice = false }
    if (showScanner) ModernScanner(profile.grade) { showScanner = false }
}

@Composable
private fun ModernHeader(profile: UserProfile, progress: Progress) {
    Card(
        modifier = Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp).fillMaxWidth()
            .border(1.dp, ModernBorder, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xE0050D19)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📚", fontSize = 22.sp)
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("EVOLUTION", color = ModernAccent, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
                    Text("LEARNING v5.0", color = ModernSub, fontSize = 9.sp, letterSpacing = 1.4.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = .05f)).border(1.dp, ModernAccent.copy(alpha = .2f), CircleShape), contentAlignment = Alignment.Center) {
                    Text("👩🏾‍🎓", fontSize = 20.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(profile.name.ifBlank { "Student" }, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text(profile.grade, fontSize = 8.sp, color = ModernSub)
                    Text("Online", fontSize = 8.sp, color = ModernGood, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    HeaderBadge("⏱ ${formatModernTime(progress.studySeconds)}", ModernAccent)
                    HeaderBadge("✏️ ${progress.quizzesCompleted}", ModernGold)
                }
            }
        }
    }
}

@Composable
private fun HeaderBadge(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = .09f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModernBottomBar(selected: ModernTab, onSelect: (ModernTab) -> Unit, onAlerts: () -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xF0050D19))
                .border(1.dp, ModernBorder, RoundedCornerShape(24.dp)).padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModernTab.entries.forEach { item -> NavItem(item.icon, item.label, selected == item, Modifier.weight(1f)) { onSelect(item) } }
            NavItem("🔔", "Alerts", false, Modifier.weight(.8f), onAlerts)
        }
    }
}

@Composable
private fun NavItem(icon: String, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (active) Brush.verticalGradient(listOf(ModernAccent.copy(alpha = .11f), ModernPurple.copy(alpha = .07f))) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
            .clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 16.sp)
        Text(label, fontSize = 8.sp, color = if (active) Color(0xFFBDF5FF) else Color.White.copy(alpha = .28f), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(2.dp))
        Box(Modifier.width(13.dp).height(2.dp).clip(RoundedCornerShape(9.dp)).background(if (active) Brush.horizontalGradient(listOf(ModernAccent, ModernPurple)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))))
    }
}

@Composable
private fun ModernHome(repository: FirebaseRepository, progress: Progress, goals: List<StudyGoal>, homework: List<HomeworkItem>, padding: PaddingValues, openFocus: () -> Unit) {
    var mood by remember { mutableStateOf<String?>(null) }
    var newGoal by remember { mutableStateOf("") }
    var hwSubject by remember { mutableStateOf("") }
    var hwTask by remember { mutableStateOf("") }
    val denominator = goals.size.coerceAtLeast(1).toFloat()
    val goalProgress = goals.count { it.completed }.toFloat() / denominator

    ModernList(padding) {
        item { ModernSection("🏠 Home", "Learn, Track, Grow") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, ModernAccent.copy(alpha = .16f), RoundedCornerShape(26.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xE60E2136)),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Pill("●  LIVE ACTIVITY", ModernGreen)
                    Spacer(Modifier.height(12.dp))
                    Text("Build momentum from what you actually do.", fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, color = ModernText)
                    Text("No pre-filled streaks, scores, or predictions. Evolution reports only activity recorded in this app.", color = ModernSub, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RealMetric(formatModernTime(progress.studySeconds), "Learning time", Modifier.weight(1f))
                        RealMetric(progress.quizzesCompleted.toString(), "Quizzes done", Modifier.weight(1f))
                        RealMetric(progress.focusSessionsCompleted.toString(), "Focus sessions", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            ModernCard {
                Text("💭 How are you feeling today?", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("😴" to "Tired", "😟" to "Stressed", "😐" to "Okay", "😊" to "Good", "🚀" to "Focused").forEach { (emoji, label) ->
                        MoodChoice(emoji, label, mood == label, Modifier.weight(1f)) { mood = label; repository.setMood(label) }
                    }
                }
            }
        }
        item {
            ModernCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📈 Daily Progress", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("${(goalProgress * 100).toInt()}%", color = ModernAccent, fontSize = 11.sp)
                }
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(progress = { goalProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = ModernAccent, trackColor = Color.White.copy(alpha = .07f))
                Text("Complete goals to fill the bar", color = ModernSub, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item {
            ModernCard {
                Text("🔒 Focus Lock Session", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Block distraction apps and stay on task. Your mission PIN is set before the session starts.", color = ModernSub, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 9.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("TikTok", "Instagram", "YouTube", "Games", "WhatsApp").forEach { RedPill(it) }
                }
                Spacer(Modifier.height(10.dp))
                GradientButton("Start Focus Lock", openFocus)
            }
        }
        item {
            ModernCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎯 Study Goals", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("${goals.count { it.completed }}/${goals.size} done", color = ModernAccent, fontSize = 11.sp)
                }
                Spacer(Modifier.height(9.dp))
                if (goals.isEmpty()) EmptyState("No goals yet. Add one below and your completion percentage will start from real activity.")
                goals.take(5).forEach { goal -> ActivityRow(goal.title, if (goal.completed) "Completed" else "Active", if (goal.completed) "✓" else "Done") { if (!goal.completed) repository.completeGoal(goal.id) } }
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(newGoal, { newGoal = it }, Modifier.weight(1f), label = { Text("Add a new goal…") }, singleLine = true)
                    Spacer(Modifier.width(7.dp))
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(ModernAccent).clickable { repository.addGoal(newGoal) { if (it.isSuccess) newGoal = "" } }.padding(horizontal = 14.dp, vertical = 14.dp)) { Text("Add", color = ModernBg, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                }
            }
        }
        item {
            ModernCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📝 Homework Tracker", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("${homework.count { !it.completed }} pending", color = ModernOrange, fontSize = 11.sp)
                }
                Spacer(Modifier.height(9.dp))
                if (homework.isEmpty()) EmptyState("No homework added yet.")
                homework.take(5).forEach { item -> ActivityRow("${item.subject}: ${item.title}", if (item.completed) "Completed" else "Pending", if (item.completed) "✓" else "Done") { if (!item.completed) repository.completeHomework(item.id) } }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(hwSubject, { hwSubject = it }, Modifier.weight(.45f), label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(hwTask, { hwTask = it }, Modifier.weight(1f), label = { Text("Assignment task") }, singleLine = true)
                }
                Spacer(Modifier.height(8.dp))
                GradientButton("Add Homework") { repository.addHomework(hwSubject, hwTask) { if (it.isSuccess) { hwSubject = ""; hwTask = "" } } }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ModernGold.copy(alpha = .05f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().border(1.dp, ModernGold.copy(alpha = .13f), RoundedCornerShape(14.dp))) {
                Column(Modifier.padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💡", fontSize = 18.sp)
                    Text("“Education is the most powerful weapon which you can use to change the world.”", color = Color.White.copy(alpha = .72f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    Text("— Nelson Mandela", color = ModernSub, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ModernStudy(repository: FirebaseRepository, progress: Progress, padding: PaddingValues, openAi: (AiFeature, String?, String) -> Unit, openFocus: () -> Unit) {
    var pomodoroSeconds by remember { mutableIntStateOf(25 * 60) }
    var pomodoroRunning by remember { mutableStateOf(false) }
    var mindTopic by remember { mutableStateOf("") }
    var cardIndex by remember { mutableIntStateOf(0) }
    var reveal by remember { mutableStateOf(false) }
    val cards = remember { listOf("What is 3x - 9 = 0?" to "x = 3", "What is the formula for force?" to "F = ma", "What is 12 × 8?" to "96") }

    LaunchedEffect(pomodoroRunning, pomodoroSeconds) {
        if (pomodoroRunning && pomodoroSeconds > 0) { delay(1000); pomodoroSeconds-- }
        else if (pomodoroSeconds == 0) pomodoroRunning = false
    }

    ModernList(padding) {
        item { ModernSection("📖 Study Hub", "AI-enhanced learning tools") }
        item { FeatureCard("🎓", "Exam Prep Centre", "Get ready for finals with official resources and proven strategies.", "Past Papers & Strategy") { openAi(AiFeature.COACH, "Exam Strategy", "Help me prepare for my next exam. Give a practical revision strategy.") } }
        item {
            ModernCard {
                Text("⚡ Smart Flashcards", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Box(Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(14.dp)).background(ModernAccent.copy(alpha = .07f)).border(1.dp, ModernAccent.copy(alpha = .16f), RoundedCornerShape(14.dp)).clickable {
                    reveal = !reveal
                    if (reveal) repository.recordFlashcardReviewed()
                }.padding(vertical = 22.dp, horizontal = 15.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (reveal) "ANSWER" else "TAP TO REVEAL", color = ModernSub, fontSize = 9.sp, letterSpacing = 1.sp)
                        Text(if (reveal) cards[cardIndex].second else cards[cardIndex].first, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 7.dp))
                        Text("Box ${cardIndex + 1}/${cards.size}", color = Color.White.copy(alpha = .22f), fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("← Prev", Modifier.weight(1f)) { cardIndex = (cardIndex - 1 + cards.size) % cards.size; reveal = false }
                    GradientButton("Next →", { cardIndex = (cardIndex + 1) % cards.size; reveal = false }, Modifier.weight(1f))
                }
            }
        }
        item {
            ModernCard {
                Text("🧠 AI Mind Map Generator", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Type any topic — the AI builds a structured concept map.", color = ModernSub, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(mindTopic, { mindTopic = it }, Modifier.weight(1f), label = { Text("e.g. Photosynthesis, Algebra, WW2…") }, singleLine = true)
                    Spacer(Modifier.width(7.dp))
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(ModernAccent, ModernBlue, ModernPurple))).clickable { openAi(AiFeature.MIND_MAP, "AI Mind Map Generator", mindTopic) }, contentAlignment = Alignment.Center) { Text("🗺️", fontSize = 18.sp) }
                }
            }
        }
        item {
            ModernCard {
                Text("⏱️ Pomodoro Timer", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatClockModern(pomodoroSeconds), color = ModernAccent, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text("FOCUS TIME", color = ModernSub, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientButton(if (pomodoroRunning) "Pause" else "Start", { pomodoroRunning = !pomodoroRunning }, Modifier.weight(1f))
                        SecondaryButton("Reset", Modifier.weight(1f)) { pomodoroRunning = false; pomodoroSeconds = 25 * 60 }
                    }
                }
            }
        }
        item {
            ModernCard {
                Text("🔎 Subject Explorer", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Choose a subject for a summary, key facts, and guided recommendations.", color = ModernSub, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Mathematics", "Science").forEach { subject -> SecondaryButton(subject, Modifier.weight(1f)) { openAi(AiFeature.TUTOR, "$subject Explorer", "Give me a Grade-level overview and key facts for $subject.") } }
                }
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("History", "English").forEach { subject -> SecondaryButton(subject, Modifier.weight(1f)) { openAi(AiFeature.TUTOR, "$subject Explorer", "Give me a Grade-level overview and key facts for $subject.") } }
                }
            }
        }
        item { FeatureCard("🔒", "Focus Lock Mode", "Block distractions and focus on studying for a set time.", "Enter Focus Mode", openFocus) }
        item { FeatureCard("📅", "Personalised Study Planner", "AI generates a custom schedule based on subjects, exam dates, and available time.", "Create My Plan") { openAi(AiFeature.STUDY_PLAN, "Personalised Study Planner", "") } }
        item { FeatureCard("📝", "AI Quiz Generator from Notes", "Paste your notes and generate a custom practice quiz.", "Generate Quiz from Notes") { openAi(AiFeature.QUIZ, "Note Quiz Generator", "") } }
        item { FeatureCard("👩🏽‍🏫", "Live AI Teacher", "Ask for step-by-step explanations, examples, and study help.", "Open Live AI Tutor") { openAi(AiFeature.TUTOR, "Live AI Teacher", "") } }
        item { FeatureCard("📷", "Camera Homework Solver", "Use the AI Smart Scanner in the AI Power Centre to extract a question and ask AI.", "Open AI Power Centre") { openAi(AiFeature.TUTOR, "Homework Solver", "Paste or type your homework question here.") } }
        item { FeatureCard("🧠", "AI Weakness Detector", "Analyze real learning totals and identify where more practice is needed.", "Analyze Weaknesses") {
            val summary = "Study time ${formatModernTime(progress.studySeconds)}, quizzes ${progress.quizzesCompleted}, accuracy ${progress.accuracyPercent ?: 0}%, flashcards ${progress.flashcardsReviewed}, focus sessions ${progress.focusSessionsCompleted}."
            openAi(AiFeature.WEAKNESS, "AI Weakness Detector", summary)
        } }
        item { FeatureCard("🧩", "Memory Training Game", "Use real flashcard review to strengthen recall.", "Review Flashcards") { } }
        item { FeatureCard("🏆", "Class Leaderboard", "No fake classmates are shown. A real leaderboard will appear only when a shared class system is connected.", "Leaderboard unavailable") { } }
        item {
            ModernCard {
                Text("🧠 AI Mistake Bank", color = ModernRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("${(progress.questionsAnswered - progress.correctAnswers).coerceAtLeast(0)} wrong answers recorded in aggregate. Detailed question history is never fabricated.", color = ModernSub, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item { ModernCard { Text("📝 My Notes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); EmptyState("No notes yet. Add real notes in a future notes storage update.") } }
    }
}

private data class AiCardSpec(val icon: String, val title: String, val badge: String, val body: String, val feature: AiFeature, val prefill: String = "", val accent: Color = ModernAccent)

@Composable
private fun ModernAiHub(profile: UserProfile, progress: Progress, padding: PaddingValues, openAi: (AiFeature, String?, String) -> Unit, openVoice: () -> Unit, openScanner: () -> Unit) {
    val specs = remember(progress) {
        listOf(
            AiCardSpec("📖", "Smart Dictionary AI", "Reference", "Search any English word for a clear definition, synonyms, and examples.", AiFeature.DICTIONARY),
            AiCardSpec("🤖", "AI Tutor", "Most Used", "Ask any question on any subject and get a grade-appropriate explanation.", AiFeature.TUTOR),
            AiCardSpec("✍️", "Essay Grader", "Powerful", "Paste an essay for structured feedback and an improvement checklist.", AiFeature.TUTOR, "Review this essay as a teacher. Give a score out of 100, strengths, weaknesses, and concrete improvements:\n", ModernOrange),
            AiCardSpec("📚", "AI Homework Helper", "New", "Get step-by-step help for homework problems.", AiFeature.TUTOR, "Help me solve this homework problem step by step without skipping the learning process:\n"),
            AiCardSpec("📝", "AI Notes Summarizer", "Essential", "Turn long study notes into a concise, high-impact summary.", AiFeature.SUMMARIZE),
            AiCardSpec("🚀", "AI Career Path Finder", "Career", "Explore careers based on subjects, interests, and goals.", AiFeature.COACH, "Act as a career exploration coach. Based on my interests, subjects and goals, suggest suitable paths and what to study next:\n"),
            AiCardSpec("✨", "EDITH", "Analytical", "Challenge your reasoning, debate complex topics, and refine your logic.", AiFeature.TUTOR, "Act as EDITH, an analytical study partner. Challenge my reasoning constructively and help me improve this idea:\n", Color(0xFFC084FC))
        )
    }

    ModernList(padding) {
        item { ModernSection("🤖 AI Power Centre", "AI study tools connected securely through Firebase") }
        item {
            AiCard("🔊", "AI Voice Reader", "Accessibility", "Listen to notes, summaries, or any text using Android text-to-speech.", ModernAccent, openVoice)
        }
        item {
            AiCard("📸", "AI Smart Scanner", "PRO", "Use the camera and real OCR to extract homework or notes, then ask AI.", ModernAccent, openScanner)
        }
        specs.forEach { spec ->
            item { AiCard(spec.icon, spec.title, spec.badge, spec.body, spec.accent) { openAi(spec.feature, spec.title, spec.prefill) } }
        }
        item {
            SmallNotice("AI answers use Firebase AI Logic. No provider secret is displayed in the app. Availability still depends on the Firebase project and App Check configuration.", ModernGood)
        }
    }
}

@Composable
private fun AiCard(icon: String, title: String, badge: String, body: String, accent: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = .15f), RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .05f)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = .12f)).border(1.dp, accent.copy(alpha = .22f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 19.sp) }
                    Spacer(Modifier.width(10.dp))
                    Text(title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Pill(badge, accent)
            }
            Text(body, color = Color.White.copy(alpha = .55f), fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 11.dp))
            GradientButton("$icon Open →", onClick)
        }
    }
}

private data class QuizQuestion(val q: String, val options: List<String>, val correct: Int)

@Composable
private fun ModernQuiz(repository: FirebaseRepository, padding: PaddingValues, openAi: (AiFeature, String?, String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var difficulty by remember { mutableStateOf("Medium") }
    var mode by remember { mutableStateOf("Practice") }
    var active by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var choice by remember { mutableStateOf<Int?>(null) }
    var finished by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var battleNotice by remember { mutableStateOf(false) }

    val subjects = remember {
        listOf(
            Triple("➗", "Mathematics", "STEM"), Triple("⚛️", "Physical Sciences", "STEM"), Triple("🧬", "Life Sciences", "STEM"), Triple("💻", "Computer Studies", "STEM"),
            Triple("📖", "English", "Languages"), Triple("🗣️", "Home Language", "Languages"), Triple("💼", "Accounting", "Commerce"), Triple("📈", "Economics", "Commerce"),
            Triple("🌍", "Geography", "Humanities"), Triple("🏛️", "History", "Humanities"), Triple("🎨", "Visual Arts", "Arts"), Triple("🎭", "Drama", "Arts")
        )
    }
    val questions = remember {
        listOf(
            QuizQuestion("Solve: 3x - 9 = 0", listOf("x = 1", "x = 2", "x = 3", "x = 6"), 2),
            QuizQuestion("What is 25% of 80?", listOf("10", "20", "25", "40"), 1),
            QuizQuestion("Which process lets green plants make food?", listOf("Respiration", "Photosynthesis", "Diffusion", "Transpiration"), 1)
        )
    }

    ModernList(padding) {
        item { ModernSection("✏️ Adaptive Quiz Engine", "Practice, measure, improve") }
        if (battleNotice) item { SmallNotice("Study Battles need a real second learner/backend connection. No simulated opponent is used.", ModernOrange) }
        if (!active && selectedSubject == null && !finished) {
            item {
                ModernCard {
                    Text("🔍 Find Your Subject", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    ModernField(search, { search = it }, "Search 12+ subjects…")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("All", "STEM", "Languages", "Commerce", "Humanities", "Arts").forEach { c -> ChoiceChip(c, category == c) { category = c } }
                    }
                    Spacer(Modifier.height(12.dp))
                    val filtered = subjects.filter { (category == "All" || it.third == category) && it.second.contains(search, true) }
                    filtered.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { item ->
                                SecondaryButton("${item.first} ${item.second}", Modifier.weight(1f)) { selectedSubject = item.second }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(7.dp))
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF43F5E)), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔️", fontSize = 32.sp)
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f)) {
                            Text("STUDY BATTLES", fontWeight = FontWeight.Black)
                            Text("Challenge a real classmate to a live quiz duel when multiplayer is connected.", fontSize = 11.sp, color = Color.White.copy(alpha = .8f))
                            TextButton(onClick = { battleNotice = true }) { Text("Join Battle", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        } else if (!active && selectedSubject != null && !finished) {
            item {
                ModernCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 40.sp)
                        Text(selectedSubject!!, color = ModernAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Curriculum-aligned practice setup", color = ModernSub, fontSize = 12.sp, modifier = Modifier.padding(vertical = 14.dp))
                        Text("CHOOSE DIFFICULTY", color = ModernSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Easy", "Medium", "Hard").forEach { d -> SecondaryButton(if (d == "Medium") "Med" else d, Modifier.weight(1f), difficulty == d) { difficulty = d } }
                        }
                        Text("CHOOSE MODE", color = ModernSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(top = 18.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Practice", "Mock Exam").forEach { m -> SecondaryButton(m, Modifier.weight(1f), mode == m) { mode = m } }
                        }
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton("Topic-by-Topic Test", Modifier.fillMaxWidth(), mode == "Topic Test") { mode = "Topic Test" }
                        Spacer(Modifier.height(18.dp))
                        GradientButton("🚀 Start Session") { active = true; index = 0; score = 0; choice = null }
                        TextButton(onClick = { selectedSubject = null }) { Text("← Back to Selection", color = ModernSub) }
                    }
                }
            }
        } else if (active) {
            item {
                ModernCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(selectedSubject ?: "Practice Quiz", color = ModernAccent, fontWeight = FontWeight.Bold)
                        Text("Q ${index + 1}/${questions.size}", color = ModernOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { index.toFloat() / questions.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = ModernAccent, trackColor = Color.White.copy(alpha = .07f))
                    val q = questions[index]
                    Text(q.q, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 15.dp))
                    q.options.forEachIndexed { i, option -> QuizOption(option, choice == i) { choice = i } }
                    Spacer(Modifier.height(10.dp))
                    GradientButton(if (index == questions.lastIndex) "Finish Session" else "Next Question →") {
                        val c = choice ?: return@GradientButton
                        val nextScore = score + if (c == q.correct) 1 else 0
                        score = nextScore
                        if (index == questions.lastIndex) {
                            active = false; finished = true
                            repository.recordQuizResult(nextScore, questions.size) { saved = it.isSuccess }
                        } else { index++; choice = null }
                    }
                }
            }
        } else if (finished) {
            item {
                ModernCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 50.sp)
                        Text("Session Complete!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        val percent = score * 100 / questions.size
                        Text("$percent%", color = ModernGood, fontSize = 36.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 10.dp))
                        Text(if (saved) "Saved to your real progress." else "Saving result…", color = if (saved) ModernGood else ModernSub, fontSize = 11.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientButton("Try Again", { finished = false; selectedSubject = null }, Modifier.weight(1f))
                            SecondaryButton("AI Practice", Modifier.weight(1f)) { openAi(AiFeature.QUIZ, "AI Quiz Generator", selectedSubject.orEmpty()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernStats(progress: Progress, goals: List<StudyGoal>, padding: PaddingValues) {
    ModernList(padding) {
        item { ModernSection("📊 Real Progress", "Only activity recorded by Evolution Learning") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard("⏱️", formatModernTime(progress.studySeconds), "Learning time", Modifier.weight(1f))
                MetricCard("✏️", progress.quizzesCompleted.toString(), "Completed quizzes", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard("🎯", progress.accuracyPercent?.let { "$it%" } ?: "—", "Average quiz accuracy", Modifier.weight(1f))
                MetricCard("🧠", progress.flashcardsReviewed.toString(), "Flashcards reviewed", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard("🔒", progress.focusSessionsCompleted.toString(), "Completed focus sessions", Modifier.weight(1f))
                MetricCard("✅", progress.homeworkCompleted.toString(), "Homework completed", Modifier.weight(1f))
            }
        }
        item { ModernCard { Text("🧾 Quiz history", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); if (progress.quizzesCompleted == 0L) EmptyState("No completed quizzes yet. Your real results will appear here.") else ActivityRow("Completed quizzes", "Questions answered: ${progress.questionsAnswered}", progress.quizzesCompleted.toString()) { } } }
        item { ModernCard { Text("🎯 Goals", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); if (goals.isEmpty()) EmptyState("No goals tracked yet.") else goals.forEach { ActivityRow(it.title, if (it.completed) "Completed" else "Active", if (it.completed) "✓" else "•") { } } } }
        item { ModernCard { Text("⏱️ App activity", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("Study time, quiz activity, flashcards, focus sessions and homework above are based only on actions recorded by Evolution Learning.", color = ModernSub, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp)) } }
    }
}

@Composable
private fun ModernProfile(repository: FirebaseRepository, profile: UserProfile, progress: Progress, padding: PaddingValues, openAi: (AiFeature, String?, String) -> Unit) {
    var info by remember { mutableStateOf<String?>(null) }
    val achievements = remember(progress) {
        listOf(
            "🌱 Study Starter" to (progress.studySeconds >= 25 * 60),
            "⚡ Flash Master" to (progress.flashcardsReviewed >= 50),
            "🏆 Quiz Champion" to (progress.accuracyPercent == 100),
            "🧘 Focus Builder" to (progress.focusSessionsCompleted >= 10),
            "✅ Homework Hero" to (progress.homeworkCompleted >= 10),
            "🎯 Goal Keeper" to (progress.goalsCompleted >= 5)
        )
    }
    ModernList(padding) {
        item { ModernSection("👤 My Profile", "") }
        item {
            Card(modifier = Modifier.fillMaxWidth().border(1.dp, ModernAccent.copy(alpha = .16f), RoundedCornerShape(26.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xE60E2136)), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👩🏾‍🎓", fontSize = 54.sp)
                    Text(profile.name.ifBlank { "New Student" }, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("${profile.grade} learning profile", color = ModernSub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RealMetric(formatModernTime(progress.studySeconds), "Learning time", Modifier.weight(1f))
                        RealMetric(progress.quizzesCompleted.toString(), "Quizzes", Modifier.weight(1f))
                        RealMetric(progress.goalsCompleted.toString(), "Goals done", Modifier.weight(1f))
                    }
                }
            }
        }
        item { FeatureCard("🤖", "AI Study Coach", "Get personalized study tips and motivation from your AI coach.", "Open Study Coach") { openAi(AiFeature.COACH, "AI Study Coach", "") } }
        item {
            ModernCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🛡️ System Security", color = ModernGood, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Pill("LOCAL CHECKS", ModernGood)
                }
                Text("Review real app protection state. Evolution Learning does not claim to be an antivirus or fabricate a threat scan.", color = ModernSub, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 10.dp))
                SecondaryButton("Review Study Lock Permission", Modifier.fillMaxWidth()) { info = "Study Lock app blocking requires Evolution Learning Study Lock to be enabled in Android Accessibility settings." }
            }
        }
        item {
            ModernCard {
                Text("⚙️ App Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Your Grade", color = ModernSub, fontSize = 12.sp); Text(profile.grade, fontSize = 12.sp) }
                SecondaryButton("Send Password Reset", Modifier.fillMaxWidth()) { repository.sendPasswordReset(profile.email) { info = if (it.isSuccess) "Password reset email sent." else it.exceptionOrNull()?.localizedMessage } }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("🏆 Achievements", Modifier.weight(1f)) { info = achievements.joinToString("\n") { (name, earned) -> "${if (earned) "✓" else "○"} $name" } }
                    SecondaryButton("📚 Resources", Modifier.weight(1f)) { info = "Resources will show only real connected learning packs and links." }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("📝 Notes", Modifier.weight(1f)) { info = "No saved notes yet." }
                    SecondaryButton("🎥 Classes", Modifier.weight(1f)) { info = "No live classes are connected yet." }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = repository::signOut, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))) { Text("Sign Out") }
                info?.let { Text(it, color = ModernSub, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 10.dp)) }
                Spacer(Modifier.height(10.dp))
                Text("Evolution Learning v5.0 · Your learning data stays yours", color = Color.White.copy(alpha = .2f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ModernAlertsDialog(progress: Progress, syncError: String?, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).border(1.dp, ModernBorder, RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF081320)), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("🔔 Alerts", color = ModernAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold); TextButton(onClick = onClose) { Text("✕") } }
                if (syncError != null) SmallNotice("Firebase sync issue: $syncError", ModernOrange) else SmallNotice("Firebase account sync is connected.", ModernGood)
                Spacer(Modifier.height(9.dp))
                EmptyState(if (progress.quizzesCompleted + progress.focusSessionsCompleted == 0L) "No alerts yet. Evolution only shows events that actually happen in your account." else "Your recorded study activity is up to date. No new alerts.")
            }
        }
    }
}

@Composable
private fun ModernAiDialog(feature: AiFeature, title: String, grade: String, prefill: String, onClose: () -> Unit) {
    val ai = remember { AiRepository() }
    val scope = rememberCoroutineScope()
    var input by remember(title) { mutableStateOf(prefill) }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(650.dp).border(1.dp, ModernAccent.copy(alpha = .22f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF081320)), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(title, color = ModernAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(feature.hint, color = ModernSub, fontSize = 10.sp) }
                    TextButton(onClick = onClose) { Text("✕") }
                }
                OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth().height(170.dp), label = { Text(feature.hint) })
                Spacer(Modifier.height(9.dp))
                GradientButton(if (busy) "Thinking…" else "Ask Evolution AI") {
                    if (busy || input.isBlank()) return@GradientButton
                    busy = true; error = null; output = null
                    scope.launch {
                        val result = ai.runFeature(feature, grade, input)
                        busy = false
                        result.onSuccess { output = it }.onFailure { error = friendlyModernAiError(it) }
                    }
                }
                if (busy) CircularProgressIndicator(color = ModernAccent, modifier = Modifier.padding(top = 10.dp).size(20.dp))
                error?.let { Text(it, color = ModernOrange, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp)) }
                output?.let {
                    Text("AI RESPONSE", color = ModernPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 7.dp)) { item { Text(it, color = Color.White.copy(alpha = .88f), fontSize = 12.sp, lineHeight = 18.sp) } }
                }
            }
        }
    }
}

@Composable
private fun ModernVoiceReader(onClose: () -> Unit) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf(1f) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) runCatching { tts?.language = Locale.getDefault() } }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).border(1.dp, ModernBorder, RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF081320)), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("AI Voice Reader", color = ModernAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("Convert text to natural device speech.", color = ModernSub, fontSize = 10.sp) }; TextButton(onClick = onClose) { Text("✕") } }
                OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth().height(220.dp), label = { Text("Paste text here to read aloud…") })
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton("🔊 Read Aloud", { tts?.setSpeechRate(speed); tts?.speak(input, TextToSpeech.QUEUE_FLUSH, null, "evolution-reader") }, Modifier.weight(2f))
                    SecondaryButton("⏹ Stop", Modifier.weight(1f)) { tts?.stop() }
                }
                Text("VOICE SPEED", color = ModernSub, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf(.7f, 1f, 1.3f, 1.6f).forEach { s -> ChoiceChip("${s}x", speed == s) { speed = s } } }
            }
        }
    }
}

@Composable
private fun ModernScanner(grade: String, onClose: () -> Unit) {
    val ai = remember { AiRepository() }
    val scope = rememberCoroutineScope()
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var extracted by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            busy = true; error = null
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result -> extracted = result.text; busy = false }
                .addOnFailureListener { throwable -> error = throwable.localizedMessage ?: "OCR failed"; busy = false }
        }
    }
    DisposableEffect(Unit) { onDispose { recognizer.close() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(680.dp).border(1.dp, ModernAccent.copy(alpha = .22f), RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF081320)), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Smart Scanner", color = ModernAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("OCR-VISION · Extract text from a camera photo", color = ModernSub, fontSize = 10.sp) }; TextButton(onClick = onClose) { Text("✕") } }
                Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black).border(1.dp, ModernAccent.copy(alpha = .3f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Text(if (extracted.isBlank()) "📸\nCamera OCR" else "TEXT EXTRACTED ✓", color = if (extracted.isBlank()) ModernSub else ModernGood, textAlign = TextAlign.Center) }
                Spacer(Modifier.height(9.dp))
                GradientButton("📸 Open Camera") { launcher.launch(null) }
                if (busy) CircularProgressIndicator(color = ModernAccent, modifier = Modifier.padding(top = 8.dp).size(20.dp))
                error?.let { Text(it, color = ModernOrange, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
                if (extracted.isNotBlank()) {
                    Text("EXTRACTED TEXT", color = ModernAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    LazyColumn(Modifier.fillMaxWidth().height(120.dp).padding(top = 5.dp)) { item { Text(extracted, fontSize = 11.sp) } }
                    Spacer(Modifier.height(7.dp))
                    GradientButton("Ask AI") {
                        if (busy) return@GradientButton
                        busy = true
                        scope.launch {
                            val result = ai.runFeature(AiFeature.TUTOR, grade, extracted)
                            busy = false
                            result.onSuccess { answer = it }.onFailure { error = friendlyModernAiError(it) }
                        }
                    }
                }
                answer?.let { Text("AI RESPONSE", color = ModernPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)); LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 5.dp)) { item { Text(it, fontSize = 11.sp, lineHeight = 17.sp) } } }
            }
        }
    }
}

@Composable
private fun ModernList(padding: PaddingValues, content: LazyListScope.() -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun ModernSection(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 3.dp)) {
        Text(title, color = ModernText, fontSize = 22.sp, fontWeight = FontWeight.Black)
        if (subtitle.isNotBlank()) Text(subtitle, color = ModernSub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ModernCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().border(1.dp, ModernBorder, RoundedCornerShape(22.dp)), colors = CardDefaults.cardColors(containerColor = ModernCard), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.height(44.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(ModernAccent, ModernBlue, ModernPurple))).clickable(onClick = onClick).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color(0xFF05111D), fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SecondaryButton(text: String, modifier: Modifier = Modifier, active: Boolean = false, onClick: () -> Unit) {
    Box(modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(if (active) ModernAccent else Color(0xA60F1C2F)).border(1.dp, ModernAccent.copy(alpha = .16f), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (active) ModernBg else Color(0xFF9EEEFA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FeatureCard(icon: String, title: String, body: String, action: String, onClick: () -> Unit) {
    ModernCard { Text("$icon $title", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text(body, color = ModernSub, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 9.dp)); GradientButton(action, onClick) }
}

@Composable
private fun RealMetric(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .035f)).border(1.dp, Color.White.copy(alpha = .07f), RoundedCornerShape(16.dp)).padding(horizontal = 9.dp, vertical = 11.dp)) {
        Text(value, color = ModernText, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Text(label.uppercase(), color = Color.White.copy(alpha = .4f), fontSize = 8.sp, letterSpacing = .4.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun MetricCard(icon: String, value: String, label: String, modifier: Modifier) {
    Card(modifier = modifier.height(112.dp).border(1.dp, ModernBorder, RoundedCornerShape(20.dp)), colors = CardDefaults.cardColors(containerColor = ModernCard), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp)) { Text(icon, fontSize = 18.sp); Spacer(Modifier.height(10.dp)); Text(value, fontSize = 23.sp, fontWeight = FontWeight.Black); Text(label, color = ModernSub, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) }
    }
}

@Composable
private fun MoodChoice(emoji: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) ModernAccent.copy(alpha = .13f) else Color.White.copy(alpha = .03f)).border(if (selected) 1.5.dp else 1.dp, if (selected) ModernAccent else Color.White.copy(alpha = .07f), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 17.sp); Text(label, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChoiceChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(if (active) ModernAccent else Color(0xA60F1C2F)).border(1.dp, ModernAccent.copy(alpha = .16f), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp)) {
        Text(label, color = if (active) ModernBg else Color(0xFF9EEEFA), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Pill(text: String, accent: Color) {
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(accent.copy(alpha = .07f)).border(1.dp, accent.copy(alpha = .2f), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(text, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp) }
}

@Composable
private fun RedPill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ModernRed.copy(alpha = .11f)).border(1.dp, ModernRed.copy(alpha = .18f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(text, color = ModernRed, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color.White.copy(alpha = .02f)).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(17.dp)).padding(18.dp), contentAlignment = Alignment.Center) { Text(text, color = ModernSub, fontSize = 11.sp, lineHeight = 17.sp, textAlign = TextAlign.Center) }
}

@Composable
private fun ActivityRow(title: String, subtitle: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 9.sp, color = ModernSub, modifier = Modifier.padding(top = 2.dp)) }
        Text(value, color = ModernAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun QuizOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) ModernAccent.copy(alpha = .14f) else Color.White.copy(alpha = .035f)).border(1.dp, if (selected) ModernAccent else Color.White.copy(alpha = .06f), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp)) { Text(label, fontSize = 12.sp) }
}

@Composable
private fun SmallNotice(text: String, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .06f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = .14f), RoundedCornerShape(14.dp))) { Text(text, color = Color.White.copy(alpha = .7f), fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(12.dp)) }
}

@Composable
private fun ModernField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
}

private fun friendlyModernAiError(error: Throwable): String {
    val msg = error.localizedMessage.orEmpty()
    return when {
        msg.contains("app check", true) || msg.contains("403") -> "AI is connected, but Firebase App Check or AI Logic configuration is blocking this build."
        msg.contains("network", true) || msg.contains("unavailable", true) -> "The AI service could not be reached. Check your internet connection and try again."
        else -> msg.ifBlank { "The AI request failed. Please try again." }
    }
}

private fun formatModernTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatClockModern(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
