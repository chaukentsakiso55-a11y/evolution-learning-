package com.cyberpulse.evolutionlearning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        AppCheckConfig.install()
        setContent { EvolutionLearningShell() }

        if (FocusSessionStore.isActive(this)) {
            startActivity(Intent(this, FocusLockActivity::class.java))
        }
    }
}

@Composable
private fun EvolutionLearningShell() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    Box(Modifier.fillMaxSize()) {
        EvolutionLearningRoot()

        if (user != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 110.dp)
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
                        ),
                        CircleShape
                    )
                    .clickable {
                        context.startActivity(Intent(context, FocusLockActivity::class.java))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("🔒", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
