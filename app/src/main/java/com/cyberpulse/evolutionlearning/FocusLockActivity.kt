package com.cyberpulse.evolutionlearning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpulse.evolutionlearning.data.FirebaseRepository
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.security.SecureRandom

private val FocusAccent = Color(0xFF38BDF8)
private val FocusBg = Color(0xFF050F1A)
private val FocusCard = Color(0xCC0A1828)
private val FocusMuted = Color(0xFF93A4BC)
private val FocusDanger = Color(0xFFF87171)
private val FocusGood = Color(0xFF4ADE80)

class FocusLockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = FocusAccent,
                    background = FocusBg,
                    surface = FocusCard,
                    onPrimary = FocusBg,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                FocusLockScreen(onFinish = { finish() })
            }
        }
    }
}

@Composable
private fun FocusLockScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    var active by remember { mutableStateOf(FocusSessionStore.isActive(context)) }
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showAbort by remember { mutableStateOf(false) }
    var showAccessibilityHelp by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(FocusSessionStore.secondsRemaining(context)) }
    var savingCompletion by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (active) showAbort = true else onFinish()
    }

    LaunchedEffect(active) {
        while (active) {
            val remaining = FocusSessionStore.secondsRemaining(context)
            secondsLeft = remaining
            if (remaining <= 0) {
                val durationSeconds = FocusSessionStore.durationSeconds(context)
                FocusSessionStore.finish(context)
                active = false
                savingCompletion = true
                repository.recordCompletedFocusSession(durationSeconds) {
                    savingCompletion = false
                }
                break
            }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0C1B33), FocusBg, Color(0xFF130820)),
                    radius = 1500f
                )
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FocusAccent.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = FocusCard),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎯", fontSize = 44.sp)
                Text(
                    if (active) "STUDY LOCK · MISSION ACTIVE" else "STUDY LOCK MISSION",
                    color = FocusAccent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    if (active) "Stay inside Evolution Learning until the mission ends or you choose to abort."
                    else "Set a mission PIN and duration, then activate all-app focus blocking.",
                    color = FocusMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                )

                if (active) {
                    Text(formatFocusClock(secondsLeft), color = FocusAccent, fontSize = 52.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "All ordinary apps are blocked while Study Lock is active. Android system and emergency screens stay available for safety.",
                        color = FocusMuted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .background(FocusGood.copy(alpha = 0.09f), RoundedCornerShape(12.dp))
                            .border(1.dp, FocusGood.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text("🔒 Mission PIN configured · Abort only needs confirmation", color = FocusGood, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { showAbort = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Abort Mission", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("MISSION DURATION", color = FocusMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(25, 45, 60).forEach { minutes ->
                            val selected = selectedMinutes == minutes
                            OutlinedButton(
                                onClick = { selectedMinutes = minutes },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) FocusAccent.copy(alpha = 0.14f) else Color.Transparent,
                                    contentColor = FocusAccent
                                ),
                                shape = RoundedCornerShape(11.dp),
                                contentPadding = PaddingValues(vertical = 9.dp)
                            ) {
                                Text("$minutes min", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { value -> pin = value.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mission PIN (4–6 digits)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { value -> confirmPin = value.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm mission PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (error != null) {
                        Text(error!!, color = FocusDanger, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            error = null
                            when {
                                pin.length !in 4..6 -> error = "Choose a 4–6 digit mission PIN."
                                pin != confirmPin -> error = "The PINs do not match."
                                !FocusSessionStore.isAccessibilityEnabled(context) -> showAccessibilityHelp = true
                                else -> {
                                    FocusSessionStore.savePin(context, pin)
                                    FocusSessionStore.start(context, selectedMinutes)
                                    secondsLeft = selectedMinutes * 60
                                    pin = ""
                                    confirmPin = ""
                                    active = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = FocusAccent, contentColor = FocusBg),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("🔒 Start Mission", fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Android requires Accessibility permission for Evolution Learning to return you to Study Lock when another app opens. You grant that permission yourself in system Settings.",
                        color = FocusMuted,
                        fontSize = 9.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (savingCompletion) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = FocusAccent, strokeWidth = 2.dp)
                }
            }
        }
    }

    if (showAbort) {
        AlertDialog(
            onDismissRequest = { showAbort = false },
            title = { Text("Abort mission?") },
            text = { Text("Do you want to abort mission? Your Study Lock session will end immediately. No PIN is required.") },
            confirmButton = {
                Button(
                    onClick = {
                        FocusSessionStore.abort(context)
                        active = false
                        showAbort = false
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
                ) { Text("Yes, abort") }
            },
            dismissButton = {
                TextButton(onClick = { showAbort = false }) { Text("Keep studying") }
            }
        )
    }

    if (showAccessibilityHelp) {
        AlertDialog(
            onDismissRequest = { showAccessibilityHelp = false },
            title = { Text("Enable Study Lock blocking") },
            text = {
                Text("To block other apps during a mission, enable Evolution Learning Study Lock in Android Accessibility settings. Then return here and tap Start Mission again.")
            },
            confirmButton = {
                Button(onClick = {
                    showAccessibilityHelp = false
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("Open Accessibility Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityHelp = false }) { Text("Not now") }
            }
        )
    }
}

internal object FocusSessionStore {
    private const val PREFS = "evolution_focus_lock"
    private const val KEY_ACTIVE = "active"
    private const val KEY_END_AT = "end_at"
    private const val KEY_DURATION_SECONDS = "duration_seconds"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_PIN_HASH = "pin_hash"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun start(context: Context, minutes: Int) {
        val seconds = minutes.coerceAtLeast(1) * 60L
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_END_AT, System.currentTimeMillis() + seconds * 1000L)
            .putLong(KEY_DURATION_SECONDS, seconds)
            .apply()
    }

    fun isActive(context: Context): Boolean {
        val p = prefs(context)
        if (!p.getBoolean(KEY_ACTIVE, false)) return false
        if (System.currentTimeMillis() >= p.getLong(KEY_END_AT, 0L)) {
            finish(context)
            return false
        }
        return true
    }

    fun secondsRemaining(context: Context): Int {
        val endAt = prefs(context).getLong(KEY_END_AT, 0L)
        if (endAt <= 0L) return 0
        val remainingMs = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
        return ((remainingMs + 999L) / 1000L).toInt()
    }

    fun durationSeconds(context: Context): Long = prefs(context).getLong(KEY_DURATION_SECONDS, 0L)

    fun abort(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, false)
            .remove(KEY_END_AT)
            .remove(KEY_DURATION_SECONDS)
            .apply()
    }

    fun finish(context: Context) = abort(context)

    fun savePin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        prefs(context).edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val expected = "${context.packageName}/${FocusAccessibilityService::class.java.name}"
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}

private fun formatFocusClock(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
