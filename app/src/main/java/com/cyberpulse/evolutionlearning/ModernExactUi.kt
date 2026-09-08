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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import java.util.Locale

private val Mbg = Color(0xFF040914)
private val Mcard = Color(0xD90C1727)
private val Maccent = Color(0xFF67E8F9)
private val Mblue = Color(0xFF38BDF8)
private val Mpurple = Color(0xFF8B5CF6)
private val Mgreen = Color(0xFF34D399)
private val Mgood = Color(0xFF4ADE80)
private val Mgold = Color(0xFFFBBF24)
private val Morange = Color(0xFFFB923C)
private val Mred = Color(0xFFF87171)
private val Mtext = Color(0xFFF8FBFF)
private val Msub = Color(0x8FE2E8F0)
private val Mborder = Color(0x1C94A3B8)

private enum class MTab(val label: String, val icon: String) {
    HOME("Home", "🏠"), STUDY("Study", "📖"), AI("AI", "🤖"), QUIZ("Quiz", "✏️"), STATS("Stats", "📊"), ME("Me", "👤")
}

private enum class MStage { SPLASH, AUTH, APP }

@Composable
fun EvolutionLearningModernRoot() {
    val repository = remember { FirebaseRepository() }
    var user by remember { mutableStateOf<FirebaseUser?>(repository.currentUser()) }
    var ready by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf(MStage.SPLASH) }

    DisposableEffect(repository) {
        val listener = repository.observeAuthState {
            user = it
            ready = true
            if (stage == MStage.APP && it == null) stage = MStage.AUTH
        }
        onDispose { repository.removeAuthStateListener(listener) }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Maccent,
            secondary = Mpurple,
            background = Mbg,
            surface = Mcard,
            onPrimary = Color(0xFF05111D),
            onBackground = Mtext,
            onSurface = Mtext
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF07101D), Mbg, Color(0xFF07111C)))
            )
        ) {
            when {
                !ready -> MLoading()
                stage == MStage.SPLASH -> MSplash { stage = if (user == null) MStage.AUTH else MStage.APP }
                stage == MStage.AUTH || user == null -> MAuth(repository) { stage = MStage.APP }
                else -> MSignedIn(repository, user!!)
            }
        }
    }
}

@Composable
private fun MLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📚", fontSize = 42.sp)
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(color = Maccent)
        }
    }
}

@Composable
private fun MSplash(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Mborder, RoundedCornerShape(30.dp)),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xC20F1C2F))
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(82.dp).clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Maccent.copy(alpha = .12f), Mpurple.copy(alpha = .14f))))
                        .border(1.dp, Maccent.copy(alpha = .18f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("📚", fontSize = 40.sp) }
                Spacer(Modifier.height(18.dp))
                Text("EVOLUTION LEARNING", color = Maccent, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("YOUR FUTURE, BUILT ONE SESSION AT A TIME", color = Msub, fontSize = 11.sp, letterSpacing = 3.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                Text("v5.0 · Modern · Focused · Built for real learning", color = Msub, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp, bottom = 22.dp))
                MPrimary("Begin Your Journey →", onClick = onStart)
            }
        }
    }
}

@Composable
private fun MAuth(repository: FirebaseRepository, onDone: () -> Unit) {
    var signUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("Grade 10") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    val grades = remember { (1..12).map { "Grade $it" } + listOf("University", "College") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, Mborder, RoundedCornerShape(30.dp)),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xC20F1C2F))
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(82.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Maccent.copy(alpha = .12f), Mpurple.copy(alpha = .14f)))), contentAlignment = Alignment.Center) { Text("👩‍🎓", fontSize = 38.sp) }
                    Spacer(Modifier.height(16.dp))
                    Text("Sign in to Evolution", fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("Protect your progress, focus sessions, and AI study history.", color = Msub, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MPrimary("Sign In", Modifier.weight(1f)) { signUp = false; message = null }
                        Box(
                            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(15.dp))
                                .background(if (signUp) Morange else Color.White.copy(alpha = .07f))
                                .clickable { signUp = true; message = null },
                            contentAlignment = Alignment.Center
                        ) { Text("Sign Up", color = if (signUp) Mbg else Mtext, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(if (signUp) "Create Account" else "Welcome Back", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(10.dp))
                    if (signUp) {
                        MField(name, { name = it }, "Full name")
                        Spacer(Modifier.height(8.dp))
                    }
                    MField(email, { email = it }, "Email address")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    if (signUp) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label = { Text("Confirm password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                        Text("Select grade", color = Msub, fontSize = 10.sp, modifier = Modifier.align(Alignment.Start).padding(top = 12.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            grades.forEach { option -> MChip(option, grade == option) { grade = option } }
                        }
                    }
                    message?.let { Text(it, color = if (error) Mred else Mgood, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp)) }
                    Spacer(Modifier.height(14.dp))
                    MPrimary(if (busy) "Please wait…" else if (signUp) "Create Account" else "Sign In") {
                        if (busy) return@MPrimary
                        val clean = email.trim()
                        message = null
                        error = false
                        when {
                            !Patterns.EMAIL_ADDRESS.matcher(clean).matches() -> { message = "Enter a valid email address."; error = true }
                            password.length < 6 -> { message = "Password must be at least 6 characters."; error = true }
                            signUp && name.trim().length < 2 -> { message = "Enter your full name."; error = true }
                            signUp && password != confirm -> { message = "Passwords do not match."; error = true }
                            else -> {
                                busy = true
                                if (signUp) repository.signUp(name, clean, password, grade) { result ->
                                    busy = false
                                    if (result.isSuccess) onDone() else { message = result.exceptionOrNull()?.localizedMessage ?: "Account creation failed."; error = true }
                                } else repository.signIn(clean, password) { result ->
                                    busy = false
                                    if (result.isSuccess) onDone() else { message = result.exceptionOrNull()?.localizedMessage ?: "Sign in failed."; error = true }
                                }
                            }
                        }
                    }
                    if (!signUp) TextButton(onClick = {
                        if (Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) repository.sendPasswordReset(email) { r ->
                            message = if (r.isSuccess) "Password reset email sent." else r.exceptionOrNull()?.localizedMessage
                            error = r.isFailure
                        } else { message = "Enter your email first."; error = true }
                    }) { Text("Forgot password?", color = Maccent) }
                }
            }
        }
    }
}

@Composable
private fun MSignedIn(repository: FirebaseRepository, user: FirebaseUser) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(UserProfile(uid = user.uid, email = user.email.orEmpty())) }
    var progress by remember { mutableStateOf(Progress()) }
    var goals by remember { mutableStateOf<List<StudyGoal>>(emptyList()) }
    var homework by remember { mutableStateOf<List<HomeworkItem>>(emptyList()) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(MTab.HOME) }
    var alerts by remember { mutableStateOf(false) }
    var aiTool by remember { mutableStateOf<AiFeature?>(null) }
    var aiTitle by remember { mutableStateOf("") }
    var aiPrefill by remember { mutableStateOf("") }
    var voice by remember { mutableStateOf(false) }
    var scanner by remember { mutableStateOf(false) }

    DisposableEffect(user.uid) {
        val p = repository.listenToProfile(user.uid, { profile = it }, { syncError = it.localizedMessage })
        val r = repository.listenToProgress(user.uid, { progress = it }, { syncError = it.localizedMessage })
        val g = repository.listenToGoals(user.uid, { goals = it }, { syncError = it.localizedMessage })
        val h = repository.listenToHomework(user.uid, { homework = it }, { syncError = it.localizedMessage })
        onDispose { p.remove(); r.remove(); g.remove(); h.remove() }
    }

    fun openAi(feature: AiFeature, title: String = feature.label, prefill: String = "") {
        aiTool = feature
        aiTitle = title
        aiPrefill = prefill
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { MHeader(profile, progress) },
        bottomBar = { MBottom(tab, { tab = it }, { alerts = true }) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                MTab.HOME -> MHome(repository, progress, goals, homework, padding) { context.startActivity(Intent(context, FocusLockActivity::class.java)) }
                MTab.STUDY -> MStudy(repository, progress, padding, ::openAi) { context.startActivity(Intent(context, FocusLockActivity::class.java)) }
                MTab.AI -> MAiHub(progress, padding, ::openAi, { voice = true }, { scanner = true })
                MTab.QUIZ -> MQuiz(repository, padding, ::openAi)
                MTab.STATS -> MStats(progress, goals, padding)
                MTab.ME -> MProfile(repository, profile, progress, padding, ::openAi)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp).size(56.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Maccent, Mblue, Mpurple)))
                    .clickable { openAi(AiFeature.TUTOR, "AI Tutor") },
                contentAlignment = Alignment.Center
            ) { Text("🤖", fontSize = 24.sp) }
        }
    }

    if (alerts) MAlerts(progress, syncError) { alerts = false }
    aiTool?.let { MAiDialog(it, aiTitle.ifBlank { it.label }, profile.grade, aiPrefill) { aiTool = null } }
    if (voice) MVoiceReader { voice = false }
    if (scanner) MScanner(profile.grade) { scanner = false }
}

@Composable
private fun MHeader(profile: UserProfile, progress: Progress) {
    Card(
        modifier = Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp).fillMaxWidth().border(1.dp, Mborder, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xE0050D19)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📚", fontSize = 22.sp)
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("EVOLUTION", color = Maccent, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
                    Text("LEARNING v5.0", color = Msub, fontSize = 9.sp, letterSpacing = 1.4.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = .05f)).border(1.dp, Maccent.copy(alpha = .2f), CircleShape), contentAlignment = Alignment.Center) { Text("👩🏾‍🎓", fontSize = 20.sp) }
                Column(horizontalAlignment = Alignment.End) {
                    Text(profile.name.ifBlank { "Student" }, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Text(profile.grade, color = Msub, fontSize = 8.sp)
                    Text("Online", color = Mgood, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    MHeaderBadge("⏱ ${mTime(progress.studySeconds)}", Maccent)
                    MHeaderBadge("✏️ ${progress.quizzesCompleted}", Mgold)
                }
            }
        }
    }
}

@Composable
private fun MHeaderBadge(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = .09f)).padding(horizontal = 7.dp, vertical = 3.dp)) { Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MBottom(selected: MTab, onSelect: (MTab) -> Unit, onAlerts: () -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xF0050D19)).border(1.dp, Mborder, RoundedCornerShape(24.dp)).padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MTab.entries.forEach { item -> MNav(item.icon, item.label, selected == item, Modifier.weight(1f)) { onSelect(item) } }
            MNav("🔔", "Alerts", false, Modifier.weight(.8f), onAlerts)
        }
    }
}

@Composable
private fun MNav(icon: String, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (active) Brush.verticalGradient(listOf(Maccent.copy(alpha = .11f), Mpurple.copy(alpha = .07f))) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 16.sp)
        Text(label, fontSize = 8.sp, color = if (active) Color(0xFFBDF5FF) else Color.White.copy(alpha = .28f), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(2.dp))
        Box(Modifier.width(13.dp).height(2.dp).clip(RoundedCornerShape(9.dp)).background(if (active) Brush.horizontalGradient(listOf(Maccent, Mpurple)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))))
    }
}

@Composable
private fun MHome(repository: FirebaseRepository, progress: Progress, goals: List<StudyGoal>, homework: List<HomeworkItem>, padding: PaddingValues, openFocus: () -> Unit) {
    var mood by remember { mutableStateOf<String?>(null) }
    var goalText by remember { mutableStateOf("") }
    var hwSubject by remember { mutableStateOf("") }
    var hwTask by remember { mutableStateOf("") }
    val goalProgress = goals.count { it.completed }.toFloat() / goals.size.coerceAtLeast(1).toFloat()

    MList(padding) {
        item { MSection("🏠 Home", "Learn, Track, Grow") }
        item {
            Card(Modifier.fillMaxWidth().border(1.dp, Maccent.copy(alpha = .16f), RoundedCornerShape(26.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xE60E2136)), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp)) {
                    MPill("●  LIVE ACTIVITY", Mgreen)
                    Text("Build momentum from what you actually do.", fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp))
                    Text("No pre-filled streaks, scores, or predictions. Evolution reports only activity recorded in this app.", color = Msub, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MRealMetric(mTime(progress.studySeconds), "Learning time", Modifier.weight(1f))
                        MRealMetric(progress.quizzesCompleted.toString(), "Quizzes done", Modifier.weight(1f))
                        MRealMetric(progress.focusSessionsCompleted.toString(), "Focus sessions", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            MCard {
                Text("💭 How are you feeling today?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("😴" to "Tired", "😟" to "Stressed", "😐" to "Okay", "😊" to "Good", "🚀" to "Focused").forEach { (emoji, label) ->
                        MMood(emoji, label, mood == label, Modifier.weight(1f)) { mood = label; repository.setMood(label) }
                    }
                }
            }
        }
        item {
            MCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("📈 Daily Progress", fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text("${(goalProgress * 100).toInt()}%", color = Maccent, fontSize = 11.sp) }
                LinearProgressIndicator(progress = { goalProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).padding(top = 0.dp), color = Maccent, trackColor = Color.White.copy(alpha = .07f))
                Text("Complete goals to fill the bar", color = Msub, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item {
            MCard {
                Text("🔒 Focus Lock Session", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Block distraction apps and stay on task. Set your mission PIN before the study session starts.", color = Msub, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 9.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("TikTok", "Instagram", "YouTube", "Games", "WhatsApp").forEach { MRedPill(it) } }
                Spacer(Modifier.height(10.dp))
                MPrimary("Start Focus Lock", onClick = openFocus)
            }
        }
        item {
            MCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("🎯 Study Goals", fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text("${goals.count { it.completed }}/${goals.size} done", color = Maccent, fontSize = 11.sp) }
                Spacer(Modifier.height(8.dp))
                if (goals.isEmpty()) MEmpty("No goals yet. Add one below and your completion percentage will start from real activity.")
                goals.take(5).forEach { goal -> MActivity(goal.title, if (goal.completed) "Completed" else "Active", if (goal.completed) "✓" else "Done") { if (!goal.completed) repository.completeGoal(goal.id) } }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(goalText, { goalText = it }, Modifier.weight(1f), label = { Text("Add a new goal…") }, singleLine = true)
                    Spacer(Modifier.width(7.dp))
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Maccent).clickable { repository.addGoal(goalText) { if (it.isSuccess) goalText = "" } }.padding(horizontal = 14.dp, vertical = 14.dp)) { Text("Add", color = Mbg, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                }
            }
        }
        item {
            MCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("📝 Homework Tracker", fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text("${homework.count { !it.completed }} pending", color = Morange, fontSize = 11.sp) }
                Spacer(Modifier.height(8.dp))
                if (homework.isEmpty()) MEmpty("No homework added yet.")
                homework.take(5).forEach { item -> MActivity("${item.subject}: ${item.title}", if (item.completed) "Completed" else "Pending", if (item.completed) "✓" else "Done") { if (!item.completed) repository.completeHomework(item.id) } }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(hwSubject, { hwSubject = it }, Modifier.weight(.45f), label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(hwTask, { hwTask = it }, Modifier.weight(1f), label = { Text("Assignment task") }, singleLine = true)
                }
                Spacer(Modifier.height(8.dp))
                MPrimary("Add Homework") { repository.addHomework(hwSubject, hwTask) { if (it.isSuccess) { hwSubject = ""; hwTask = "" } } }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Mgold.copy(alpha = .05f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Mgold.copy(alpha = .13f), RoundedCornerShape(14.dp))) {
                Column(Modifier.padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💡", fontSize = 18.sp)
                    Text("“Education is the most powerful weapon which you can use to change the world.”", color = Color.White.copy(alpha = .72f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    Text("— Nelson Mandela", color = Msub, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun MStudy(repository: FirebaseRepository, progress: Progress, padding: PaddingValues, openAi: (AiFeature, String, String) -> Unit, openFocus: () -> Unit) {
    var timer by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var mindTopic by remember { mutableStateOf("") }
    var cardIndex by remember { mutableIntStateOf(0) }
    var reveal by remember { mutableStateOf(false) }
    val flashcards = remember { listOf("What is 3x - 9 = 0?" to "x = 3", "What is the formula for force?" to "F = ma", "What is 12 × 8?" to "96") }

    LaunchedEffect(running, timer) {
        if (running && timer > 0) { delay(1000); timer-- } else if (timer == 0) running = false
    }

    MList(padding) {
        item { MSection("📖 Study Hub", "AI-enhanced learning tools") }
        item { MFeature("🎓", "Exam Prep Centre", "Get ready for finals with resources and strategy tools.", "Past Papers & Strategy") { openAi(AiFeature.COACH, "Exam Strategy", "Help me prepare for my next exam with a practical revision strategy.") } }
        item {
            MCard {
                Text("⚡ Smart Flashcards", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(14.dp)).background(Maccent.copy(alpha = .07f)).border(1.dp, Maccent.copy(alpha = .16f), RoundedCornerShape(14.dp)).clickable {
                    reveal = !reveal
                    if (reveal) repository.recordFlashcardReviewed()
                }.padding(vertical = 24.dp, horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (reveal) "ANSWER" else "TAP TO REVEAL", color = Msub, fontSize = 9.sp)
                        Text(if (reveal) flashcards[cardIndex].second else flashcards[cardIndex].first, textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                        Text("Box ${cardIndex + 1}/${flashcards.size}", color = Color.White.copy(alpha = .22f), fontSize = 9.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MSecondary("← Prev", Modifier.weight(1f)) { cardIndex = (cardIndex - 1 + flashcards.size) % flashcards.size; reveal = false }
                    MPrimary("Next →", Modifier.weight(1f)) { cardIndex = (cardIndex + 1) % flashcards.size; reveal = false }
                }
            }
        }
        item {
            MCard {
                Text("🧠 AI Mind Map Generator", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Type any topic — AI builds a structured concept map.", color = Msub, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(mindTopic, { mindTopic = it }, Modifier.weight(1f), label = { Text("e.g. Photosynthesis, Algebra, WW2…") }, singleLine = true)
                    Spacer(Modifier.width(7.dp))
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Maccent, Mblue, Mpurple))).clickable { openAi(AiFeature.MIND_MAP, "AI Mind Map Generator", mindTopic) }, contentAlignment = Alignment.Center) { Text("🗺️", fontSize = 18.sp) }
                }
            }
        }
        item {
            MCard {
                Text("⏱️ Pomodoro Timer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(mClock(timer), color = Maccent, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text("FOCUS TIME", color = Msub, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MPrimary(if (running) "Pause" else "Start", Modifier.weight(1f)) { running = !running }
                        MSecondary("Reset", Modifier.weight(1f)) { running = false; timer = 25 * 60 }
                    }
                }
            }
        }
        item {
            MCard {
                Text("🔎 Subject Explorer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Choose a subject for summaries, key facts, and guided help.", color = Msub, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Mathematics", "Science").forEach { s -> MSecondary(s, Modifier.weight(1f)) { openAi(AiFeature.TUTOR, "$s Explorer", "Give me a grade-appropriate overview and key facts for $s.") } }
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("History", "English").forEach { s -> MSecondary(s, Modifier.weight(1f)) { openAi(AiFeature.TUTOR, "$s Explorer", "Give me a grade-appropriate overview and key facts for $s.") } }
                }
            }
        }
        item { MFeature("🔒", "Focus Lock Mode", "Block distraction apps and focus on studying for a set time.", "Enter Focus Mode", openFocus) }
        item { MFeature("📅", "Personalised Study Planner", "AI builds a custom schedule from subjects, dates, weak areas, and available time.", "Create My Plan") { openAi(AiFeature.STUDY_PLAN, "Personalised Study Planner", "") } }
        item { MFeature("📝", "AI Quiz Generator from Notes", "Paste notes and generate a custom practice quiz.", "Generate Quiz from Notes") { openAi(AiFeature.QUIZ, "Quiz Generator from Notes", "") } }
        item { MFeature("👩🏽‍🏫", "Live AI Teacher", "Ask for step-by-step explanations, examples, and study help.", "Open Live AI Tutor") { openAi(AiFeature.TUTOR, "Live AI Teacher", "") } }
        item { MFeature("📷", "Camera Homework Solver", "Use the Smart Scanner in AI Power Centre to extract a homework question and ask AI.", "Open Homework Helper") { openAi(AiFeature.TUTOR, "Homework Helper", "Paste or type your homework question here.") } }
        item { MFeature("🧠", "AI Weakness Detector", "Analyze real learning totals and identify where more practice is needed.", "Analyze Weaknesses") {
            openAi(AiFeature.WEAKNESS, "AI Weakness Detector", "Study time ${mTime(progress.studySeconds)}, quizzes ${progress.quizzesCompleted}, accuracy ${progress.accuracyPercent ?: 0}%, flashcards ${progress.flashcardsReviewed}, focus sessions ${progress.focusSessionsCompleted}.")
        } }
        item { MFeature("🧩", "Memory Training Game", "Smart Flashcards are the active memory-training tool in this build.", "Review Flashcards") { } }
        item { MFeature("🏆", "Class Leaderboard", "No fake classmates are shown. A real class leaderboard needs a shared backend and privacy rules.", "No live leaderboard") { } }
        item { MCard { Text("🧠 AI Mistake Bank", color = Mred, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text("${(progress.questionsAnswered - progress.correctAnswers).coerceAtLeast(0)} wrong answers recorded in aggregate. Detailed history is never fabricated.", color = Msub, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) } }
        item { MCard { Text("📝 My Notes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); MEmpty("No notes yet. Only real saved notes will appear here.") } }
    }
}

private data class MTool(val icon: String, val title: String, val badge: String, val body: String, val feature: AiFeature, val prefill: String = "", val accent: Color = Maccent)

@Composable
private fun MAiHub(progress: Progress, padding: PaddingValues, openAi: (AiFeature, String, String) -> Unit, openVoice: () -> Unit, openScanner: () -> Unit) {
    val tools = remember(progress) {
        listOf(
            MTool("📖", "Smart Dictionary AI", "Reference", "Search any English word for a clear definition, synonyms, and examples.", AiFeature.DICTIONARY),
            MTool("🤖", "AI Tutor", "Most Used", "Ask any school question and get a grade-appropriate explanation.", AiFeature.TUTOR),
            MTool("✍️", "Essay Grader", "Powerful", "Paste an essay for structured feedback and improvements.", AiFeature.TUTOR, "Review this essay as a teacher. Give a score out of 100, strengths, weaknesses, and concrete improvements:\n", Morange),
            MTool("📚", "AI Homework Helper", "New", "Get step-by-step learning help for homework problems.", AiFeature.TUTOR, "Help me understand and solve this homework problem step by step:\n"),
            MTool("📝", "AI Notes Summarizer", "Essential", "Turn long notes into a concise, high-impact study summary.", AiFeature.SUMMARIZE),
            MTool("🚀", "AI Career Path Finder", "Career", "Explore careers based on your subjects, interests, and goals.", AiFeature.COACH, "Based on my subjects, interests and goals, suggest realistic career paths and what I should study next:\n"),
            MTool("✨", "EDITH", "Analytical", "Challenge your reasoning and refine your logic.", AiFeature.TUTOR, "Act as an analytical study partner. Challenge my reasoning constructively and help me improve this idea:\n", Color(0xFFC084FC))
        )
    }
    MList(padding) {
        item { MSection("🤖 AI Power Centre", "AI study tools connected securely through Firebase") }
        item { MAiCard("🔊", "AI Voice Reader", "Accessibility", "Listen to notes, summaries, or any text using Android text-to-speech.", Maccent, openVoice) }
        item { MAiCard("📸", "AI Smart Scanner", "PRO", "Use the camera and real OCR to extract homework or notes, then ask AI.", Maccent, openScanner) }
        tools.forEach { tool -> item { MAiCard(tool.icon, tool.title, tool.badge, tool.body, tool.accent) { openAi(tool.feature, tool.title, tool.prefill) } } }
        item { MNotice("AI requests use Firebase AI Logic. The provider secret is not displayed in the app. Live responses still depend on your Firebase AI/App Check setup.", Mgood) }
    }
}

@Composable
private fun MAiCard(icon: String, title: String, badge: String, body: String, accent: Color, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = .15f), RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .05f)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = .12f)).border(1.dp, accent.copy(alpha = .22f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 19.sp) }
                    Spacer(Modifier.width(10.dp))
                    Text(title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                MPill(badge, accent)
            }
            Text(body, color = Color.White.copy(alpha = .55f), fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 11.dp))
            MPrimary("$icon Open →", onClick = onClick)
        }
    }
}

private data class MQuizQuestion(val question: String, val options: List<String>, val correct: Int)

@Composable
private fun MQuiz(repository: FirebaseRepository, padding: PaddingValues, openAi: (AiFeature, String, String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var subject by remember { mutableStateOf<String?>(null) }
    var difficulty by remember { mutableStateOf("Medium") }
    var mode by remember { mutableStateOf("Practice") }
    var active by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var choice by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var saved by remember { mutableStateOf(false) }
    var battleNote by remember { mutableStateOf(false) }

    val subjects = remember {
        listOf(
            Triple("➗", "Mathematics", "STEM"), Triple("⚛️", "Physical Sciences", "STEM"), Triple("🧬", "Life Sciences", "STEM"), Triple("💻", "Computer Studies", "STEM"),
            Triple("📖", "English", "Languages"), Triple("🗣️", "Home Language", "Languages"), Triple("💼", "Accounting", "Commerce"), Triple("📈", "Economics", "Commerce"),
            Triple("🌍", "Geography", "Humanities"), Triple("🏛️", "History", "Humanities"), Triple("🎨", "Visual Arts", "Arts"), Triple("🎭", "Drama", "Arts")
        )
    }
    val questions = remember {
        listOf(
            MQuizQuestion("Solve: 3x - 9 = 0", listOf("x = 1", "x = 2", "x = 3", "x = 6"), 2),
            MQuizQuestion("What is 25% of 80?", listOf("10", "20", "25", "40"), 1),
            MQuizQuestion("Which process lets green plants make food?", listOf("Respiration", "Photosynthesis", "Diffusion", "Transpiration"), 1)
        )
    }

    MList(padding) {
        item { MSection("✏️ Adaptive Quiz Engine", "Practice, measure, improve") }
        if (battleNote) item { MNotice("Study Battles need a real second learner/backend connection. No simulated opponent is used.", Morange) }
        when {
            !active && subject == null && !finished -> {
                item {
                    MCard {
                        Text("🔍 Find Your Subject", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        MField(search, { search = it }, "Search 12+ subjects…")
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("All", "STEM", "Languages", "Commerce", "Humanities", "Arts").forEach { c -> MChip(c, category == c) { category = c } }
                        }
                        Spacer(Modifier.height(12.dp))
                        val filtered = subjects.filter { (category == "All" || it.third == category) && it.second.contains(search, true) }
                        filtered.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { item -> MSecondary("${item.first} ${item.second}", Modifier.weight(1f)) { subject = item.second } }
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
                                Text("Challenge a real classmate to a live quiz duel when multiplayer is connected.", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                                TextButton(onClick = { battleNote = true }) { Text("Join Battle", color = Color.White, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
            !active && subject != null && !finished -> {
                item {
                    MCard {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚", fontSize = 40.sp)
                            Text(subject!!, color = Maccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Curriculum-aligned practice setup", color = Msub, fontSize = 12.sp, modifier = Modifier.padding(vertical = 14.dp))
                            Text("CHOOSE DIFFICULTY", color = Msub, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Easy", "Medium", "Hard").forEach { d -> MSecondary(if (d == "Medium") "Med" else d, Modifier.weight(1f), difficulty == d) { difficulty = d } }
                            }
                            Text("CHOOSE MODE", color = Msub, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(top = 18.dp))
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Practice", "Mock Exam").forEach { m -> MSecondary(m, Modifier.weight(1f), mode == m) { mode = m } }
                            }
                            Spacer(Modifier.height(8.dp))
                            MSecondary("Topic-by-Topic Test", Modifier.fillMaxWidth(), mode == "Topic Test") { mode = "Topic Test" }
                            Spacer(Modifier.height(18.dp))
                            MPrimary("🚀 Start Session") { active = true; index = 0; score = 0; choice = null }
                            TextButton(onClick = { subject = null }) { Text("← Back to Selection", color = Msub) }
                        }
                    }
                }
            }
            active -> {
                item {
                    val q = questions[index]
                    MCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(subject ?: "Practice Quiz", color = Maccent, fontWeight = FontWeight.Bold); Text("Q ${index + 1}/${questions.size}", color = Morange, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { index.toFloat() / questions.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Maccent, trackColor = Color.White.copy(alpha = .07f))
                        Text(q.question, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 15.dp))
                        q.options.forEachIndexed { i, option -> MQuizOption(option, choice == i) { choice = i } }
                        Spacer(Modifier.height(10.dp))
                        MPrimary(if (index == questions.lastIndex) "Finish Session" else "Next Question →") {
                            val selected = choice ?: return@MPrimary
                            val nextScore = score + if (selected == q.correct) 1 else 0
                            score = nextScore
                            if (index == questions.lastIndex) {
                                active = false
                                finished = true
                                repository.recordQuizResult(nextScore, questions.size) { saved = it.isSuccess }
                            } else { index++; choice = null }
                        }
                    }
                }
            }
            finished -> {
                item {
                    MCard {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆", fontSize = 50.sp)
                            Text("Session Complete!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("${score * 100 / questions.size}%", color = Mgood, fontSize = 36.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 10.dp))
                            Text(if (saved) "Saved to your real progress." else "Saving result…", color = if (saved) Mgood else Msub, fontSize = 11.sp)
                            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MPrimary("Try Again", Modifier.weight(1f)) { finished = false; subject = null }
                                MSecondary("AI Practice", Modifier.weight(1f)) { openAi(AiFeature.QUIZ, "AI Quiz Generator", subject.orEmpty()) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MStats(progress: Progress, goals: List<StudyGoal>, padding: PaddingValues) {
    MList(padding) {
        item { MSection("📊 Real Progress", "Only activity recorded by Evolution Learning") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { MMetric("⏱️", mTime(progress.studySeconds), "Learning time", Modifier.weight(1f)); MMetric("✏️", progress.quizzesCompleted.toString(), "Completed quizzes", Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { MMetric("🎯", progress.accuracyPercent?.let { "$it%" } ?: "—", "Average quiz accuracy", Modifier.weight(1f)); MMetric("🧠", progress.flashcardsReviewed.toString(), "Flashcards reviewed", Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { MMetric("🔒", progress.focusSessionsCompleted.toString(), "Completed focus sessions", Modifier.weight(1f)); MMetric("✅", progress.homeworkCompleted.toString(), "Homework completed", Modifier.weight(1f)) } }
        item { MCard { Text("🧾 Quiz history", fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); if (progress.quizzesCompleted == 0L) MEmpty("No completed quizzes yet. Your real results will appear here.") else MActivity("Completed quizzes", "Questions answered: ${progress.questionsAnswered}", progress.quizzesCompleted.toString()) { } } }
        item { MCard { Text("🎯 Goals", fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); if (goals.isEmpty()) MEmpty("No goals tracked yet.") else goals.forEach { goal -> MActivity(goal.title, if (goal.completed) "Completed" else "Active", if (goal.completed) "✓" else "•") { } } } }
        item { MCard { Text("⏱️ App activity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text("Study time, quizzes, flashcards, focus sessions and homework are based only on actions recorded by Evolution Learning.", color = Msub, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp)) } }
    }
}

@Composable
private fun MProfile(repository: FirebaseRepository, profile: UserProfile, progress: Progress, padding: PaddingValues, openAi: (AiFeature, String, String) -> Unit) {
    var info by remember { mutableStateOf<String?>(null) }
    val earned = listOf(
        "🌱 Study Starter" to (progress.studySeconds >= 25 * 60),
        "⚡ Flash Master" to (progress.flashcardsReviewed >= 50),
        "🏆 Quiz Champion" to (progress.accuracyPercent == 100),
        "🧘 Focus Builder" to (progress.focusSessionsCompleted >= 10),
        "✅ Homework Hero" to (progress.homeworkCompleted >= 10),
        "🎯 Goal Keeper" to (progress.goalsCompleted >= 5)
    )
    MList(padding) {
        item { MSection("👤 My Profile", "") }
        item {
            Card(Modifier.fillMaxWidth().border(1.dp, Maccent.copy(alpha = .16f), RoundedCornerShape(26.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xE60E2136)), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👩🏾‍🎓", fontSize = 54.sp)
                    Text(profile.name.ifBlank { "New Student" }, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("${profile.grade} learning profile", color = Msub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MRealMetric(mTime(progress.studySeconds), "Learning time", Modifier.weight(1f))
                        MRealMetric(progress.quizzesCompleted.toString(), "Quizzes", Modifier.weight(1f))
                        MRealMetric(progress.goalsCompleted.toString(), "Goals done", Modifier.weight(1f))
                    }
                }
            }
        }
        item { MFeature("🤖", "AI Study Coach", "Get personalized study tips and motivation from your AI coach.", "Open Study Coach") { openAi(AiFeature.COACH, "AI Study Coach", "") } }
        item {
            MCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("🛡️ System Security", color = Mgood, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); MPill("LOCAL CHECKS", Mgood) }
                Text("Review real protection state. Evolution Learning does not pretend to run an antivirus scan or invent a threat score.", color = Msub, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 10.dp))
                MSecondary("Review Study Lock Permission", Modifier.fillMaxWidth()) { info = "Study Lock blocking works only after you enable Evolution Learning Study Lock in Android Accessibility settings." }
            }
        }
        item {
            MCard {
                Text("⚙️ App Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Your Grade", color = Msub, fontSize = 12.sp); Text(profile.grade, fontSize = 12.sp) }
                MSecondary("Send Password Reset", Modifier.fillMaxWidth()) { repository.sendPasswordReset(profile.email) { info = if (it.isSuccess) "Password reset email sent." else it.exceptionOrNull()?.localizedMessage } }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MSecondary("🏆 Achievements", Modifier.weight(1f)) { info = earned.joinToString("\n") { (name, ok) -> "${if (ok) "✓" else "○"} $name" } }
                    MSecondary("📚 Resources", Modifier.weight(1f)) { info = "Resources will show only real connected learning packs and links." }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MSecondary("📝 Notes", Modifier.weight(1f)) { info = "No saved notes yet." }
                    MSecondary("🎥 Classes", Modifier.weight(1f)) { info = "No live classes are connected yet." }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = repository::signOut, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))) { Text("Sign Out") }
                info?.let { Text(it, color = Msub, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 10.dp)) }
                Text("Evolution Learning v5.0 · Your learning data stays yours", color = Color.White.copy(alpha = .2f), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
            }
        }
    }
}

@Composable
private fun MAlerts(progress: Progress, error: String?, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).border(1.dp, Mborder, RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF081320)), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("🔔 Alerts", color = Maccent, fontSize = 17.sp, fontWeight = FontWeight.Bold); TextButton(onClick = onClose) { Text("✕") } }
                if (error != null) MNotice("Firebase sync issue: $error", Morange) else MNotice("Firebase account sync is connected.", Mgood)
                Spacer(Modifier.height(9.dp))
                MEmpty(if (progress.quizzesCompleted + progress.focusSessionsCompleted == 0L) "No alerts yet. Evolution only shows events that actually happen in your account." else "Your recorded study activity is up to date. No new alerts.")
            }
        }
    }
}

@Composable
private fun MAiDialog(feature: AiFeature, title: String, grade: String, prefill: String, onClose: () -> Unit) {
    val ai = remember { AiRepository() }
    val scope = rememberCoroutineScope()
    var input by remember(title) { mutableStateOf(prefill) }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(650.dp).border(1.dp, Maccent.copy(alpha = .22f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF081320)), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(title, color = Maccent, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(feature.hint, color = Msub, fontSize = 10.sp) }
                    TextButton(onClick = onClose) { Text("✕") }
                }
                OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth().height(170.dp), label = { Text(feature.hint) })
                Spacer(Modifier.height(9.dp))
                MPrimary(if (busy) "Thinking…" else "Ask Evolution AI") {
                    if (busy || input.isBlank()) return@MPrimary
                    busy = true; error = null; output = null
                    scope.launch {
                        val result = ai.runFeature(feature, grade, input)
                        busy = false
                        result.onSuccess { output = it }.onFailure { error = mAiError(it) }
                    }
                }
                if (busy) CircularProgressIndicator(color = Maccent, modifier = Modifier.padding(top = 10.dp).size(20.dp))
                error?.let { Text(it, color = Morange, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp)) }
                output?.let { response ->
                    Text("AI RESPONSE", color = Mpurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 7.dp)) { item { Text(response, color = Color.White.copy(alpha = .88f), fontSize = 12.sp, lineHeight = 18.sp) } }
                }
            }
        }
    }
}

@Composable
private fun MVoiceReader(onClose: () -> Unit) {
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
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).border(1.dp, Mborder, RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF081320)), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("AI Voice Reader", color = Maccent, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("Convert text to natural device speech.", color = Msub, fontSize = 10.sp) }; TextButton(onClick = onClose) { Text("✕") } }
                OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth().height(220.dp), label = { Text("Paste text here to read aloud…") })
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MPrimary("🔊 Read Aloud", Modifier.weight(2f)) { tts?.setSpeechRate(speed); tts?.speak(input, TextToSpeech.QUEUE_FLUSH, null, "evolution-reader") }
                    MSecondary("⏹ Stop", Modifier.weight(1f)) { tts?.stop() }
                }
                Text("VOICE SPEED", color = Msub, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf(.7f, 1f, 1.3f, 1.6f).forEach { s -> MChip("${s}x", speed == s) { speed = s } } }
            }
        }
    }
}

@Composable
private fun MScanner(grade: String, onClose: () -> Unit) {
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
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener { result -> extracted = result.text; busy = false }.addOnFailureListener { throwable -> error = throwable.localizedMessage ?: "OCR failed"; busy = false }
        }
    }
    DisposableEffect(Unit) { onDispose { recognizer.close() } }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(680.dp).border(1.dp, Maccent.copy(alpha = .22f), RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF081320)), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Smart Scanner", color = Maccent, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("OCR-VISION · Extract text from a camera photo", color = Msub, fontSize = 10.sp) }; TextButton(onClick = onClose) { Text("✕") } }
                Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black).border(1.dp, Maccent.copy(alpha = .3f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Text(if (extracted.isBlank()) "📸\nCamera OCR" else "TEXT EXTRACTED ✓", color = if (extracted.isBlank()) Msub else Mgood, textAlign = TextAlign.Center) }
                Spacer(Modifier.height(9.dp))
                MPrimary("📸 Open Camera") { launcher.launch(null) }
                if (busy) CircularProgressIndicator(color = Maccent, modifier = Modifier.padding(top = 8.dp).size(20.dp))
                error?.let { Text(it, color = Morange, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
                if (extracted.isNotBlank()) {
                    Text("EXTRACTED TEXT", color = Maccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    LazyColumn(Modifier.fillMaxWidth().height(120.dp).padding(top = 5.dp)) { item { Text(extracted, fontSize = 11.sp) } }
                    Spacer(Modifier.height(7.dp))
                    MPrimary("Ask AI") {
                        if (busy) return@MPrimary
                        busy = true
                        scope.launch {
                            val result = ai.runFeature(AiFeature.TUTOR, grade, extracted)
                            busy = false
                            result.onSuccess { answer = it }.onFailure { error = mAiError(it) }
                        }
                    }
                }
                answer?.let { response -> Text("AI RESPONSE", color = Mpurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)); LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 5.dp)) { item { Text(response, fontSize = 11.sp, lineHeight = 17.sp) } } }
            }
        }
    }
}

@Composable
private fun MList(padding: PaddingValues, content: LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun MSection(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 3.dp)) { Text(title, color = Mtext, fontSize = 22.sp, fontWeight = FontWeight.Black); if (subtitle.isNotBlank()) Text(subtitle, color = Msub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)) }
}

@Composable
private fun MCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().border(1.dp, Mborder, RoundedCornerShape(22.dp)), colors = CardDefaults.cardColors(containerColor = Mcard), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun MPrimary(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(44.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Maccent, Mblue, Mpurple))).clickable(onClick = onClick).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(text, color = Color(0xFF05111D), fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center) }
}

@Composable
private fun MSecondary(text: String, modifier: Modifier = Modifier, active: Boolean = false, onClick: () -> Unit) {
    Box(modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(if (active) Maccent else Color(0xA60F1C2F)).border(1.dp, Maccent.copy(alpha = .16f), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text(text, color = if (active) Mbg else Color(0xFF9EEEFA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center) }
}

@Composable
private fun MFeature(icon: String, title: String, body: String, action: String, onClick: () -> Unit) {
    MCard { Text("$icon $title", fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(body, color = Msub, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 9.dp)); MPrimary(action, onClick = onClick) }
}

@Composable
private fun MRealMetric(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .035f)).border(1.dp, Color.White.copy(alpha = .07f), RoundedCornerShape(16.dp)).padding(horizontal = 9.dp, vertical = 11.dp)) { Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black); Text(label.uppercase(), color = Color.White.copy(alpha = .4f), fontSize = 8.sp, letterSpacing = .4.sp, modifier = Modifier.padding(top = 3.dp)) }
}

@Composable
private fun MMetric(icon: String, value: String, label: String, modifier: Modifier) {
    Card(modifier.height(112.dp).border(1.dp, Mborder, RoundedCornerShape(20.dp)), colors = CardDefaults.cardColors(containerColor = Mcard), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(15.dp)) { Text(icon, fontSize = 18.sp); Spacer(Modifier.height(10.dp)); Text(value, fontSize = 23.sp, fontWeight = FontWeight.Black); Text(label, color = Msub, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) } }
}

@Composable
private fun MMood(emoji: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) Maccent.copy(alpha = .13f) else Color.White.copy(alpha = .03f)).border(if (selected) 1.5.dp else 1.dp, if (selected) Maccent else Color.White.copy(alpha = .07f), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(emoji, fontSize = 17.sp); Text(label, fontSize = 8.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun MChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(if (active) Maccent else Color(0xA60F1C2F)).border(1.dp, Maccent.copy(alpha = .16f), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp)) { Text(label, color = if (active) Mbg else Color(0xFF9EEEFA), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MPill(text: String, accent: Color) {
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(accent.copy(alpha = .07f)).border(1.dp, accent.copy(alpha = .2f), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(text, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp) }
}

@Composable
private fun MRedPill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Mred.copy(alpha = .11f)).border(1.dp, Mred.copy(alpha = .18f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(text, color = Mred, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MEmpty(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color.White.copy(alpha = .02f)).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(17.dp)).padding(18.dp), contentAlignment = Alignment.Center) { Text(text, color = Msub, fontSize = 11.sp, lineHeight = 17.sp, textAlign = TextAlign.Center) }
}

@Composable
private fun MActivity(title: String, subtitle: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Msub, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp)) }; Text(value, color = Maccent, fontSize = 12.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun MQuizOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) Maccent.copy(alpha = .14f) else Color.White.copy(alpha = .035f)).border(1.dp, if (selected) Maccent else Color.White.copy(alpha = .06f), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp)) { Text(label, fontSize = 12.sp) }
}

@Composable
private fun MNotice(text: String, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .06f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = .14f), RoundedCornerShape(14.dp))) { Text(text, color = Color.White.copy(alpha = .7f), fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(12.dp)) }
}

@Composable
private fun MField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
}

private fun mAiError(error: Throwable): String {
    val msg = error.localizedMessage.orEmpty()
    return when {
        msg.contains("app check", true) || msg.contains("403") -> "AI is connected, but Firebase App Check or AI Logic configuration is blocking this build."
        msg.contains("network", true) || msg.contains("unavailable", true) -> "The AI service could not be reached. Check your internet connection and try again."
        else -> msg.ifBlank { "The AI request failed. Please try again." }
    }
}

private fun mTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun mClock(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
