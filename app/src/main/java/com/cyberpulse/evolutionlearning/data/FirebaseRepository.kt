package com.cyberpulse.evolutionlearning.data

import com.cyberpulse.evolutionlearning.model.HomeworkItem
import com.cyberpulse.evolutionlearning.model.Progress
import com.cyberpulse.evolutionlearning.model.StudyGoal
import com.cyberpulse.evolutionlearning.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun currentUser(): FirebaseUser? = auth.currentUser

    fun observeAuthState(onChange: (FirebaseUser?) -> Unit): FirebaseAuth.AuthStateListener {
        val listener = FirebaseAuth.AuthStateListener { onChange(it.currentUser) }
        auth.addAuthStateListener(listener)
        return listener
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }

    fun signIn(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun signUp(name: String, email: String, password: String, grade: String, onResult: (Result<Unit>) -> Unit) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user ?: run {
                    onResult(Result.failure(IllegalStateException("Firebase did not return a user.")))
                    return@addOnSuccessListener
                }
                val userRef = db.collection("users").document(user.uid)
                val profile = mapOf(
                    "uid" to user.uid,
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "grade" to grade,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val zeroProgress = mapOf(
                    "studySeconds" to 0L,
                    "quizzesCompleted" to 0L,
                    "questionsAnswered" to 0L,
                    "correctAnswers" to 0L,
                    "flashcardsReviewed" to 0L,
                    "focusSessionsCompleted" to 0L,
                    "goalsCompleted" to 0L,
                    "homeworkCompleted" to 0L,
                    "xp" to 0L,
                    "streak" to 0L,
                    "lastCheckInDate" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.batch().apply {
                    set(userRef, profile)
                    set(userRef.collection("progress").document("current"), zeroProgress)
                }.commit()
                    .addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { error ->
                        user.delete()
                        onResult(Result.failure(error))
                    }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun sendPasswordReset(email: String, onResult: (Result<Unit>) -> Unit) {
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun signOut() = auth.signOut()

    fun listenToProfile(uid: String, onChange: (UserProfile) -> Unit, onError: (Throwable) -> Unit = {}): ListenerRegistration =
        db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) onChange(snapshot.toProfile(uid))
        }

    fun listenToProgress(uid: String, onChange: (Progress) -> Unit, onError: (Throwable) -> Unit = {}): ListenerRegistration =
        db.collection("users").document(uid).collection("progress").document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) onChange(snapshot.toProgress()) else onChange(Progress())
            }

    fun listenToGoals(uid: String, onChange: (List<StudyGoal>) -> Unit, onError: (Throwable) -> Unit = {}): ListenerRegistration =
        db.collection("users").document(uid).collection("goals").orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents?.map { doc ->
                    StudyGoal(doc.id, doc.getString("title").orEmpty(), doc.getBoolean("completed") ?: false)
                }.orEmpty())
            }

    fun listenToHomework(uid: String, onChange: (List<HomeworkItem>) -> Unit, onError: (Throwable) -> Unit = {}): ListenerRegistration =
        db.collection("users").document(uid).collection("homework").orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents?.map { doc ->
                    HomeworkItem(doc.id, doc.getString("subject").orEmpty(), doc.getString("title").orEmpty(), doc.getBoolean("completed") ?: false)
                }.orEmpty())
            }

    fun addGoal(title: String, onResult: (Result<Unit>) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onResult(Result.failure(IllegalStateException("Not signed in")))
        if (title.isBlank()) return onResult(Result.failure(IllegalArgumentException("Goal cannot be empty")))
        db.collection("users").document(uid).collection("goals").add(
            mapOf("title" to title.trim(), "completed" to false, "createdAt" to FieldValue.serverTimestamp())
        ).addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun completeGoal(goalId: String, onResult: (Result<Unit>) -> Unit = {}) = completeItem("goals", goalId, "goalsCompleted", 50, onResult)

    fun addHomework(subject: String, title: String, onResult: (Result<Unit>) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onResult(Result.failure(IllegalStateException("Not signed in")))
        if (subject.isBlank() || title.isBlank()) return onResult(Result.failure(IllegalArgumentException("Subject and task are required")))
        db.collection("users").document(uid).collection("homework").add(
            mapOf("subject" to subject.trim(), "title" to title.trim(), "completed" to false, "createdAt" to FieldValue.serverTimestamp())
        ).addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun completeHomework(itemId: String, onResult: (Result<Unit>) -> Unit = {}) = completeItem("homework", itemId, "homeworkCompleted", 50, onResult)

    private fun completeItem(collection: String, itemId: String, progressField: String, xpReward: Long, onResult: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(Result.failure(IllegalStateException("Not signed in")))
        val userRef = db.collection("users").document(uid)
        val itemRef = userRef.collection(collection).document(itemId)
        val progressRef = userRef.collection("progress").document("current")
        db.runTransaction { transaction ->
            val item = transaction.get(itemRef)
            if (item.exists() && item.getBoolean("completed") != true) {
                transaction.update(itemRef, mapOf("completed" to true, "completedAt" to FieldValue.serverTimestamp()))
                transaction.set(
                    progressRef,
                    mapOf(
                        progressField to FieldValue.increment(1),
                        "xp" to FieldValue.increment(xpReward),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
        }.addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun recordCompletedFocusSession(durationSeconds: Long, onResult: (Result<Unit>) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onResult(Result.failure(IllegalStateException("Not signed in")))
        if (durationSeconds <= 0L) return onResult(Result.failure(IllegalArgumentException("Duration must be positive")))
        val xpReward = maxOf(25L, durationSeconds / 60L)
        val ref = db.collection("users").document(uid).collection("progress").document("current")
        ref.set(
            mapOf(
                "studySeconds" to FieldValue.increment(durationSeconds),
                "focusSessionsCompleted" to FieldValue.increment(1),
                "xp" to FieldValue.increment(xpReward),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun recordQuizResult(correct: Int, total: Int, onResult: (Result<Unit>) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onResult(Result.failure(IllegalStateException("Not signed in")))
        if (total <= 0 || correct !in 0..total) return onResult(Result.failure(IllegalArgumentException("Invalid quiz result")))
        val ref = db.collection("users").document(uid).collection("progress").document("current")
        ref.set(
            mapOf(
                "quizzesCompleted" to FieldValue.increment(1),
                "questionsAnswered" to FieldValue.increment(total.toLong()),
                "correctAnswers" to FieldValue.increment(correct.toLong()),
                "xp" to FieldValue.increment((correct * 20L) + 20L),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun recordFlashcardReviewed(onResult: (Result<Unit>) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onResult(Result.failure(IllegalStateException("Not signed in")))
        val ref = db.collection("users").document(uid).collection("progress").document("current")
        ref.set(
            mapOf(
                "flashcardsReviewed" to FieldValue.increment(1),
                "xp" to FieldValue.increment(5),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun recordDailyCheckIn(onResult: (Result<Boolean>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(Result.failure(IllegalStateException("Not signed in")))
        val today = LocalDate.now()
        val todayText = today.toString()
        val userRef = db.collection("users").document(uid)
        val progressRef = userRef.collection("progress").document("current")
        val checkInRef = userRef.collection("checkins").document(todayText)
        db.runTransaction { transaction ->
            if (transaction.get(checkInRef).exists()) return@runTransaction false
            val progress = transaction.get(progressRef)
            val last = progress.getString("lastCheckInDate").orEmpty()
            val oldStreak = progress.getLong("streak") ?: 0L
            val newStreak = runCatching {
                when {
                    last.isBlank() -> 1L
                    LocalDate.parse(last) == today.minusDays(1) -> oldStreak + 1L
                    else -> 1L
                }
            }.getOrDefault(1L)
            transaction.set(checkInRef, mapOf("date" to todayText, "createdAt" to FieldValue.serverTimestamp()))
            transaction.set(
                progressRef,
                mapOf(
                    "xp" to FieldValue.increment(100),
                    "streak" to newStreak,
                    "lastCheckInDate" to todayText,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            true
        }.addOnSuccessListener { onResult(Result.success(it)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun setMood(mood: String, onResult: (Result<Unit>) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onResult(Result.failure(IllegalStateException("Not signed in")))
        val date = LocalDate.now().toString()
        db.collection("users").document(uid).collection("moods").document(date)
            .set(mapOf("mood" to mood, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}

private fun DocumentSnapshot.toProfile(uid: String) = UserProfile(
    uid = uid,
    name = getString("name") ?: "Student",
    email = getString("email") ?: "",
    grade = getString("grade") ?: "Grade 10"
)

private fun DocumentSnapshot.toProgress() = Progress(
    studySeconds = getLong("studySeconds") ?: 0,
    quizzesCompleted = getLong("quizzesCompleted") ?: 0,
    questionsAnswered = getLong("questionsAnswered") ?: 0,
    correctAnswers = getLong("correctAnswers") ?: 0,
    flashcardsReviewed = getLong("flashcardsReviewed") ?: 0,
    focusSessionsCompleted = getLong("focusSessionsCompleted") ?: 0,
    goalsCompleted = getLong("goalsCompleted") ?: 0,
    homeworkCompleted = getLong("homeworkCompleted") ?: 0,
    xp = getLong("xp") ?: 0,
    streak = getLong("streak") ?: 0,
    lastCheckInDate = getString("lastCheckInDate") ?: ""
)
