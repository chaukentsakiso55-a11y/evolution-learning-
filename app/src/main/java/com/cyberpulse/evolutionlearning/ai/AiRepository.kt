package com.cyberpulse.evolutionlearning.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AiRepository {
    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent"

    suspend fun runFeature(
        feature: AiFeature,
        grade: String,
        input: String
    ): Result<String> = runCatching {
        require(input.isNotBlank()) { "Enter something for the AI first." }

        val prompt = when (feature) {
            AiFeature.TUTOR -> """
                You are the Evolution Learning AI Tutor for a $grade learner.
                Explain the student's question clearly, step by step, at the learner's level.
                Teach rather than merely giving an answer. Use short examples when useful.
                If the question is unclear, state what is unclear and ask one concise follow-up.

                Student question:
                $input
            """.trimIndent()

            AiFeature.QUIZ -> """
                You are the Evolution Learning Quiz Generator for a $grade learner.
                Create a useful 5-question practice quiz from the topic or notes below.
                Mix question styles where appropriate. Do not invent facts that are not supported by the supplied notes when notes are provided.
                Put the answer key after the questions, with one short explanation per answer.

                Topic or notes:
                $input
            """.trimIndent()

            AiFeature.SUMMARIZE -> """
                You are the Evolution Learning Notes Summarizer for a $grade learner.
                Summarize only what is supported by the notes below.
                Keep key definitions, dates, formulas, causes, effects, and examples that matter for studying.
                Use clear headings and concise bullet points. If the notes are incomplete, say what information is missing rather than guessing.

                Notes:
                $input
            """.trimIndent()

            AiFeature.MIND_MAP -> """
                You are the Evolution Learning Mind Map Generator for a $grade learner.
                Turn the topic or notes below into a text mind map.
                Use one central topic, then major branches, then short sub-branches.
                Keep it easy to copy into a visual mind-map tool. Do not add unsupported claims when source notes are supplied.

                Topic or notes:
                $input
            """.trimIndent()
        }

        requestGemini(prompt)
    }

    private suspend fun requestGemini(prompt: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("x-goog-api-key", EmbeddedGeminiKey.value())
        }

        try {
            val requestJson = JSONObject()
                .put(
                    "contents",
                    org.json.JSONArray().put(
                        JSONObject().put(
                            "parts",
                            org.json.JSONArray().put(JSONObject().put("text", prompt))
                        )
                    )
                )

            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(requestJson.toString())
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (status !in 200..299) {
                val apiMessage = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull()
                error(apiMessage?.takeIf { it.isNotBlank() } ?: "Gemini request failed with HTTP $status")
            }

            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates")
                ?: error("The AI returned no candidates. Please try again.")
            val content = candidates.optJSONObject(0)?.optJSONObject("content")
                ?: error("The AI returned no content. Please try again.")
            val parts = content.optJSONArray("parts")
                ?: error("The AI returned no text. Please try again.")

            buildString {
                for (i in 0 until parts.length()) {
                    val text = parts.optJSONObject(i)?.optString("text").orEmpty()
                    if (text.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(text)
                    }
                }
            }.trim().takeIf { it.isNotBlank() }
                ?: error("The AI returned no text. Please try again.")
        } finally {
            connection.disconnect()
        }
    }
}

enum class AiFeature(val label: String, val hint: String) {
    TUTOR("Tutor", "Ask a school question or paste a problem"),
    QUIZ("Quiz Maker", "Enter a topic or paste notes"),
    SUMMARIZE("Summarize", "Paste the notes you want shortened"),
    MIND_MAP("Mind Map", "Enter a topic or paste notes")
}
