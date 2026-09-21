package com.cyberpulse.evolutionlearning.ai

import com.google.firebase.FirebaseApp
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Small, fully on-device fallback model for Evolution-learning.
 * The GGUF file is downloaded by CI and packaged under assets/models.
 */
object OfflineLlmEngine {
    private const val ASSET_PATH = "models/SmolLM2-135M-Instruct-Q4_K_M.gguf"
    private const val MODEL_FILE = "SmolLM2-135M-Instruct-Q4_K_M.gguf"
    private val gate = Mutex()

    suspend fun complete(prompt: String): String = gate.withLock {
        val context = FirebaseApp.getInstance().applicationContext
        val modelDir = File(context.filesDir, "offline_models").apply { mkdirs() }
        val modelFile = File(modelDir, MODEL_FILE)

        if (!modelFile.exists() || modelFile.length() < 50_000_000L) {
            context.assets.open(ASSET_PATH).use { input ->
                modelFile.outputStream().use { output -> input.copyTo(output, 1024 * 1024) }
            }
        }

        val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
        val model = Llama.loadModel(
            modelPath = modelFile.absolutePath,
            config = LlamaConfig(contextSize = 2048, threads = threads),
        )
        try {
            val result = Llama.complete(
                model,
                prompt = prompt,
                systemPrompt = "You are Evolution-learning, a concise school learning assistant. Explain clearly, teach step by step, and say when you are uncertain. Keep responses suitable for the learner's grade.",
                maxTokens = 384,
            )
            result.text.trim().ifBlank { error("The offline model returned no text.") }
        } finally {
            Llama.releaseModel(model)
        }
    }
}
