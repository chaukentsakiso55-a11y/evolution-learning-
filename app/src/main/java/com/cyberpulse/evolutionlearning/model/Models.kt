package com.cyberpulse.evolutionlearning.model

data class UserProfile(
    val uid: String = "",
    val name: String = "Student",
    val email: String = "",
    val grade: String = "Grade 10"
)

data class Progress(
    val studySeconds: Long = 0,
    val quizzesCompleted: Long = 0,
    val questionsAnswered: Long = 0,
    val correctAnswers: Long = 0,
    val flashcardsReviewed: Long = 0,
    val focusSessionsCompleted: Long = 0,
    val goalsCompleted: Long = 0,
    val homeworkCompleted: Long = 0,
    val xp: Long = 0,
    val streak: Long = 0,
    val lastCheckInDate: String = ""
) {
    val accuracyPercent: Int?
        get() = if (questionsAnswered <= 0L) null
        else ((correctAnswers.toDouble() / questionsAnswered.toDouble()) * 100).toInt()

    val level: Long
        get() = (xp / 1000L) + 1L

    val xpIntoLevel: Long
        get() = xp % 1000L
}

data class StudyGoal(
    val id: String = "",
    val title: String = "",
    val completed: Boolean = false
)

data class HomeworkItem(
    val id: String = "",
    val subject: String = "",
    val title: String = "",
    val completed: Boolean = false
)
