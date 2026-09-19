package com.cyberpulse.evolutionlearning.ai

import com.google.firebase.FirebaseApp

object EmbeddedApiKey {
    private const val ASSET_PATH = "config/evolution-api.key"

    fun readOrNull(): String? {
        val context = FirebaseApp.getInstance().applicationContext
        return runCatching {
            context.assets.open(ASSET_PATH).bufferedReader().use { it.readText().trim() }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    fun isConfigured(): Boolean = readOrNull() != null
}
