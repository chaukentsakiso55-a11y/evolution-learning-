package com.cyberpulse.evolutionlearning

val appSpec = AppSpec(
    name = "Evolution Learning",
    shortName = "EL",
    tagline = "Learn → practise → improve → evolve.",
    hero = "Build a learning loop that adapts as your knowledge, habits and confidence grow.",
    primary = 0xFF2CE6FF,
    secondary = 0xFFA961FF,
    focusLabel = "Evolution cycle",
    logHint = "Add a goal, homework task or reflection",
    features = listOf(
        AppFeature("Daily Check-in", "Set the mood, priority and target for today.", "TODAY"),
        AppFeature("Study Hub", "Connect flashcards, notes and subject plans.", "LEARN"),
        AppFeature("AI Tools", "Prepare tutor, quiz and mind-map workflows.", "SMART"),
        AppFeature("Quiz", "Turn weak areas into deliberate practice.", "TEST"),
        AppFeature("Progress", "Make consistency and improvement visible.", "EVOLVE"),
        AppFeature("Profile", "Keep goals and learning preferences in one place.", "YOU")
    ),
    metrics = listOf(
        AppMetric("Loop", "Active"),
        AppMetric("Check-in", "Local"),
        AppMetric("Subjects", "Flexible"),
        AppMetric("AI tools", "Phase 2")
    ),
    about = "Evolution Learning is a Cyber Pulse education product built around a simple loop: learn, practise, improve and evolve."
)
