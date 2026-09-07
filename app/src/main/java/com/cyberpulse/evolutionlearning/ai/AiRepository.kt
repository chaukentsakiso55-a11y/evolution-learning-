package com.cyberpulse.evolutionlearning.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

class AiRepository {
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-3.7-flash")

    suspend fun runFeature(feature: AiFeature, grade: String, input: String): Result<String> = runCatching {
        require(input.isNotBlank()) { "Enter something for the AI first." }

        val prompt = when (feature) {
            AiFeature.TUTOR -> """
                You are the Evolution Learning AI Tutor for a $grade learner.
                Explain the student's question clearly, step by step, at the learner's level.
                Teach rather than only giving the final answer. Use short examples when useful.

                Student question:
                $input
            """.trimIndent()

            AiFeature.QUIZ -> """
                You are the Evolution Learning Quiz Generator for a $grade learner.
                Create a useful 5-question practice quiz from the topic or notes below.
                Put the answer key after the questions with one short explanation per answer.
                Do not invent unsupported facts when notes are supplied.

                Topic or notes:
                $input
            """.trimIndent()

            AiFeature.SUMMARIZE -> """
                You are the Evolution Learning Notes Summarizer for a $grade learner.
                Summarize only what is supported by the notes below.
                Keep key definitions, dates, formulas, causes, effects, and examples that matter for studying.
                Use clear headings and concise bullet points.

                Notes:
                $input
            """.trimIndent()

            AiFeature.MIND_MAP -> """
                You are the Evolution Learning Mind Map Generator for a $grade learner.
                Turn the topic or notes below into a text mind map.
                Use one central topic, then major branches, then short sub-branches.

                Topic or notes:
                $input
            """.trimIndent()

            AiFeature.DICTIONARY -> """
                You are the Evolution Learning Smart Dictionary for a $grade learner.
                For the word or phrase below, give a clear definition, part of speech when relevant,
                two useful synonyms, and two short example sentences. Keep it school-friendly and accurate.

                Word or phrase:
                $input
            """.trimIndent()

            AiFeature.STUDY_PLAN -> """
                You are the Evolution Learning Study Planner for a $grade learner.
                Build a realistic study plan from the learner's subjects, exam dates, weak areas, and available time below.
                Organize it into a simple schedule with priorities, breaks, and revision checkpoints.

                Learner information:
                $input
            """.trimIndent()

            AiFeature.WEAKNESS -> """
                You are the Evolution Learning Weakness Detector for a $grade learner.
                Analyze the real progress summary below. Identify likely weak areas only when supported by the data.
                If the data is insufficient, say so. Give 3 practical next study actions.

                Progress summary:
                $input
            """.trimIndent()

            AiFeature.COACH -> """
                You are the Evolution Learning AI Study Coach for a $grade learner.
                Give concise, encouraging, practical study guidance based on the learner's situation below.
                Avoid fake praise and focus on the next useful action.

                Learner situation:
                $input
            """.trimIndent()
        }

        val response = model.generateContent(prompt)
        response.text?.trim().takeUnless { it.isNullOrBlank() }
            ?: error("The AI returned no text. Please try again.")
    }
}

enum class AiFeature(val label: String, val hint: String) {
    TUTOR("AI Tutor", "Ask a school question or paste a problem"),
    QUIZ("Quiz Generator", "Enter a topic or paste notes"),
    SUMMARIZE("Note Summarizer", "Paste notes to summarize"),
    MIND_MAP("Mind Map", "Enter a topic or paste notes"),
    DICTIONARY("Smart Dictionary", "Enter a word or phrase"),
    STUDY_PLAN("Study Planner", "Enter subjects, exam dates and available time"),
    WEAKNESS("Weakness Detector", "Analyze your real learning progress"),
    COACH("Study Coach", "Describe what you are working on")
}
