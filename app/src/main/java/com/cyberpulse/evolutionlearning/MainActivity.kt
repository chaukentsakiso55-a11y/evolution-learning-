package com.cyberpulse.evolutionlearning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        AppCheckConfig.install()
        setContent { EvolutionLearningModernRoot() }

        if (FocusSessionStore.isActive(this)) {
            startActivity(Intent(this, FocusLockActivity::class.java))
        }
    }
}
