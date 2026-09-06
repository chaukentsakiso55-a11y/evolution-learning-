package com.cyberpulse.evolutionlearning.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

class AiRepository {
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-3.7-flash")

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

        val response = model.generateContent(prompt)
        response.text?.trim().takeUnless { it.isNullOrBlank() }
            ?: error("The AI returned no text. Please try again.")
    }
}

enum class AiFeature(val label: String, val hint: String) {
    TUTOR("Tutor", "Ask a school question or paste a problem"),
    QUIZ("Quiz Maker", "Enter a topic or paste notes"),
    SUMMARIZE("Summarize", "Paste the notes you want shortened"),
    MIND_MAP("Mind Map", "Enter a topic or paste notes")
}
